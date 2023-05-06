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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        Map<String, RentForecastMonth> totalRentMonths = new LinkedHashMap<>();
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
        // Track the rent values for the unit type
        BigDecimal marketRent = Optional.ofNullable(unitTypeForecast.getStartingMarketRent()).orElse(BigDecimal.ZERO);
        BigDecimal actualRent = Optional.ofNullable(unitTypeForecast.getStartingActualRent()).orElse(BigDecimal.ZERO);
        BigDecimal excessRentAdjustmentRate = Optional.ofNullable(unitTypeForecast.getExcessRentAdjustmentRate()).orElse(BigDecimal.ZERO);
        BigDecimal compoundedActualEscalationRate = BigDecimal.ONE;
        // Sort the escalation months
        List<ForecastMonth> sortedForecastMonths = unitTypeForecast.getForecastMonthData().stream()
                .sorted(Comparator.comparingInt(ForecastMonth::getYear)
                        .thenComparingInt(ForecastMonth::getMonth))
                .toList();

        for (int i = 0; i < sortedForecastMonths.size(); i++) {
            // Get current month
            ForecastMonth forecastMonth = sortedForecastMonths.get(i);

            // Compound actual escalation and determine actual escalation rate for this month
            compoundedActualEscalationRate = compoundedActualEscalationRate.multiply(BigDecimal.ONE.add(forecastMonth.getActualEscalationRate()));
            BigDecimal currentMonthActualEscalationRate = BigDecimal.ONE;
            // Only escalate actual if we're in a renewal month, check remainder of i/term
            if (unitTypeForecast.getContractTerm() != null && (i % unitTypeForecast.getContractTerm()) == 0) {
                currentMonthActualEscalationRate = compoundedActualEscalationRate;
            }

            // Forecast rents for month in question
            BigDecimal forecastedActualRent = actualRentForecastService.calculateActualRentForMonth(unitTypeForecast.getStartingActualRent(), actualRent, currentMonthActualEscalationRate);
            BigDecimal forecastedMarketRent = marketRentForecastService.calculateMarketRentForMonth(forecastMonth, marketRent, forecastedActualRent, excessRentAdjustmentRate);

            RentForecastMonth rentMonth = RentForecastMonth.builder()
                    .month(forecastMonth.getMonth())
                    .year(forecastMonth.getYear())
                    .marketRent(forecastedMarketRent)
                    .actualRent(forecastedActualRent)
                    .build();

            // Update the tracker for current market rents for the unit type
            marketRentsByMonth.add(rentMonth);
            marketRent = forecastedMarketRent;
            actualRent = forecastedActualRent;
        }

        return marketRentsByMonth;
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
        Map<Integer, RentForecastYear> yearSummaries = new LinkedHashMap<>();
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
