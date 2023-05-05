package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.market.MarketRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import com.methodicalplatforms.rentforecast.request.RentForecastOptions;
import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import com.methodicalplatforms.rentforecast.response.RentForecastMonth;
import com.methodicalplatforms.rentforecast.response.RentForecastYear;
import com.methodicalplatforms.rentforecast.response.RentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RentForecastService {

    private static final String ALL_UNITS = "ALL UNITS";
    private final ActualRentForecastService actualRentForecastService;
    private final MarketRentForecastService marketRentForecastService;

    @Autowired
    public RentForecastService(ActualRentForecastService actualRentForecastService, MarketRentForecastService marketRentForecastService) {
        this.actualRentForecastService = actualRentForecastService;
        this.marketRentForecastService = marketRentForecastService;
    }

    public RentResponse calculateMarketRent(RentForecastRequest rentForecastRequest) {
        Map<String, List<RentForecastMonth>> rentByMonths = getMonthlyMarketRentsForAllUnitTypes(rentForecastRequest.getUnitTypeForecastList());

        RentResponse.RentResponseBuilder marketResponseBuilder = RentResponse.builder();
        RentForecastOptions options = rentForecastRequest.getOptions();
        if (options != null && rentForecastRequest.getOptions().getSummarizeByYear()) {
            // Summarize by Year
            Map<String, List<RentForecastYear>> rentByYears = summarizeYearsForAllUnitTypes(rentByMonths);
            if (options.getSummarizeByUnitType()) {
                // Summarize by unit type
                rentByYears = yearlySummaryByUnitType(rentByYears);
            }
            marketResponseBuilder.unitTypeMarketRentYears(rentByYears);
        } else {
            // Monthly summary
            if (options != null && options.getSummarizeByUnitType()) {
                // Summarize by unit type
                rentByMonths = monthlySummaryByUnitType(rentByMonths);
            }
            marketResponseBuilder.unitTypeMarketRentMonths(rentByMonths);
        }

        return marketResponseBuilder.build();
    }

    private Map<String, List<RentForecastMonth>> monthlySummaryByUnitType(Map<String, List<RentForecastMonth>> rentMonthsByUnitType) {
        Map<String, RentForecastMonth> totalRentMonths = new HashMap<>();
        rentMonthsByUnitType.values().stream()
                .flatMap(List::stream)
                .forEach(unitRentMonth -> totalRentMonths.merge(
                        unitRentMonth.getMonth() + "-" + unitRentMonth.getYear(),
                        unitRentMonth,
                        (oldRentMonth, newRentMonth) -> {
                            oldRentMonth.setMarketRent(oldRentMonth.getMarketRent().add(newRentMonth.getMarketRent()));
                            return oldRentMonth;
                        }));

        return Map.of(ALL_UNITS, new ArrayList<>(totalRentMonths.values()));
    }

    private Map<String, List<RentForecastYear>> yearlySummaryByUnitType(Map<String, List<RentForecastYear>> rentYearsByUnitType) {
        Map<Integer, RentForecastYear> totalsForRentYears = new HashMap<>();
        rentYearsByUnitType.values().stream()
                .flatMap(List::stream)
                .forEach(unitRentYear -> totalsForRentYears.merge(
                        unitRentYear.getYear(),
                        unitRentYear,
                        (oldRentYear, newRentYear) -> {
                            oldRentYear.setMarketRent(oldRentYear.getMarketRent().add(newRentYear.getMarketRent()));
                            return oldRentYear;
                        }));

        return Map.of(ALL_UNITS, new ArrayList<>(totalsForRentYears.values()));
    }


    private Map<String, List<RentForecastMonth>> getMonthlyMarketRentsForAllUnitTypes(List<UnitTypeForecast> unitTypeForecastList) {
        // Use a stream to iterate through the forecast data for each unit type and create a map with unit type as key and monthly market rents as value
        return unitTypeForecastList.stream()
                // The first argument to Collectors.toMap specifies the key to be used in the map
                .collect(Collectors.toMap(
                        UnitTypeForecast::getUnitType, // key is unit type
                        this::calculateMonthlyMarketRentsByIndividualUnitType, // value is monthly market rents
                        // This merge function resolves any key collisions by keeping the last value encountered (i.e. using the new value instead of the old value)
                        (a, b) -> b, HashMap::new));
    }


    private List<RentForecastMonth> calculateMonthlyMarketRentsByIndividualUnitType(UnitTypeForecast unitTypeForecast) {
        List<RentForecastMonth> marketRentsByMonth = new ArrayList<>();
        // Track the market rent for the unit type
        BigDecimal marketRent = unitTypeForecast.getStartingMarketRent();
        BigDecimal actualRent = unitTypeForecast.getStartingActualRent();

        // Sort the escalation months
        List<ForecastMonth> sortedForecastMonths = unitTypeForecast.getForecastMonthData().stream()
                .sorted(Comparator.comparingInt(ForecastMonth::getYear)
                        .thenComparingInt(ForecastMonth::getMonth))
                .toList();

        for (ForecastMonth forecastMonth : sortedForecastMonths) {
            // Calculate the market rent for the month in question
            RentForecastMonth rentMonth = forecastRentsForMonth(forecastMonth, marketRent, actualRent);

            // Update the tracker for current market rents for the unit type
            marketRentsByMonth.add(rentMonth);
            marketRent = rentMonth.getMarketRent();
            actualRent = rentMonth.getActualRent();
        }

        return marketRentsByMonth;
    }

    private RentForecastMonth forecastRentsForMonth(ForecastMonth forecastMonth, BigDecimal currentMarketRent, BigDecimal currentActualRent) {
        BigDecimal forecastedMarketRent = marketRentForecastService.calculateMarketRentForMonth(forecastMonth, currentMarketRent);
        BigDecimal forecastedActualRent = actualRentForecastService.calculateActualRentForMonth(forecastMonth, currentActualRent);

        return RentForecastMonth.builder()
                .month(forecastMonth.getMonth())
                .year(forecastMonth.getYear())
                .marketRent(forecastedMarketRent)
                .actualRent(forecastedActualRent)
                .build();
    }

    private Map<String, List<RentForecastYear>> summarizeYearsForAllUnitTypes(Map<String, List<RentForecastMonth>> marketRentMonthsByUnitType) {
        Map<String, List<RentForecastYear>> yearSummaryByUnitType = new HashMap<>();

        // Summarize the year for each unit type
        marketRentMonthsByUnitType.forEach((unitType, marketRentMonths) -> {
            List<RentForecastYear> rentYearSummaryForUnitType = summarizeYearsForAUnitType(marketRentMonths);
            yearSummaryByUnitType.put(unitType, rentYearSummaryForUnitType);
        });

        return yearSummaryByUnitType;
    }

    private List<RentForecastYear> summarizeYearsForAUnitType(List<RentForecastMonth> rentMonths) {
        Map<Integer, RentForecastYear> yearSummaries = new HashMap<>();
        rentMonths.forEach(rentForecastMonth -> {
            int year = rentForecastMonth.getYear();
            if (!yearSummaries.containsKey(year)) {
                yearSummaries.put(year, RentForecastYear.builder()
                        .year(year)
                        .actualRent(BigDecimal.ZERO)
                        .marketRent(BigDecimal.ZERO)
                        .build());
            }
            var yearSummary = yearSummaries.get(year);
            yearSummary.setMarketRent(yearSummary.getMarketRent().add(rentForecastMonth.getMarketRent()));
            yearSummary.setActualRent(yearSummary.getActualRent().add(rentForecastMonth.getActualRent()));
        });

        // Convert the map to a list of RentYear objects
        return yearSummaries.values().stream().toList();
    }

}
