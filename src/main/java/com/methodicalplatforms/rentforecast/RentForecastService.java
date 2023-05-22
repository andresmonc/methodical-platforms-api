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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RentForecastService {

    private static final String ALL_UNITS = "ALL UNITS";
    private final ActualRentForecastService actualRentForecastService;
    private final MarketRentForecastService marketRentForecastService;

    private static final int DECIMAL_PLACES = 15;

    @Autowired
    public RentForecastService(ActualRentForecastService actualRentForecastService, MarketRentForecastService marketRentForecastService) {
        this.actualRentForecastService = actualRentForecastService;
        this.marketRentForecastService = marketRentForecastService;
    }

    public RentResponse forecastRents(RentForecastRequest rentForecastRequest) {
        Map<String, List<RentForecastMonth>> rentByMonths = forecastRentsForAllUnitTypes(rentForecastRequest.getUnitTypeForecastList());

        RentResponse.RentResponseBuilder rentResponseBuilder = RentResponse.builder();
        RentForecastOptions options = rentForecastRequest.getOptions();
        if (options != null && rentForecastRequest.getOptions().getSummarizeByYear()) {
            // Summarize by Year
            Map<String, List<RentForecastYear>> rentByYears = summarizeYearsForAllUnitTypes(rentByMonths);
            if (options.getSummarizeByUnitType()) {
                // Summarize by unit type
                rentByYears = yearlySummaryByUnitType(rentByYears);
            }
            rentResponseBuilder.unitTypeMarketRentYears(rentByYears);
        } else {
            // Monthly summary
            if (options != null && options.getSummarizeByUnitType()) {
                // Summarize by unit type
                rentByMonths = monthlySummaryByUnitType(rentByMonths);
            }
            rentResponseBuilder.unitTypeMarketRentMonths(rentByMonths);
        }

        return rentResponseBuilder.build();
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


    private Map<String, List<RentForecastMonth>> forecastRentsForAllUnitTypes(List<UnitTypeForecast> unitTypeForecastList) {
        // Use a stream to iterate through the forecast data for each unit type and create a map with unit type as key and monthly market rents as value
        return unitTypeForecastList.stream()
                // The first argument to Collectors.toMap specifies the key to be used in the map
                .collect(Collectors.toMap(
                        UnitTypeForecast::getUnitType, // key is unit type
                        this::forecastMonthlyRentsByIndividualUnitType, // value is monthly market rents
                        // This merge function resolves any key collisions by keeping the last value encountered (i.e. using the new value instead of the old value)
                        (a, b) -> b, HashMap::new));
    }

    private List<RentForecastMonth> forecastMonthlyRentsByIndividualUnitType(UnitTypeForecast unitTypeForecast) {
        List<RentForecastMonth> forecastedRentsByMonth = new ArrayList<>();

        // Track the rent values for the unit type
        BigDecimal marketRent = Objects.requireNonNullElse(unitTypeForecast.getStartingMarketRent(), BigDecimal.ZERO);
        BigDecimal actualRent = Objects.requireNonNullElse(unitTypeForecast.getStartingActualRent(), BigDecimal.ZERO);
        BigDecimal excessRentAdjustmentRate = Objects.requireNonNullElse(unitTypeForecast.getExcessRentAdjustmentRate(), BigDecimal.ZERO);
        BigDecimal compoundedActualEscalationRate = BigDecimal.ONE;
        BigDecimal marketEscalationRate = BigDecimal.ONE;

        // Sort the escalation months
        List<ForecastMonth> sortedForecastMonths = unitTypeForecast.getForecastMonthData().stream()
                .sorted(Comparator.comparingInt(ForecastMonth::getYear)
                        .thenComparingInt(ForecastMonth::getMonth))
                .toList();

        for (int i = 0; i < sortedForecastMonths.size(); i++) {
            // Get current month
            ForecastMonth forecastMonth = sortedForecastMonths.get(i);

            BigDecimal currentMonthActualEscalationRate = BigDecimal.ONE;
            // Only escalate actual if it's the month after contract end
            if (isEscalationMonthForActual(unitTypeForecast, i)) {
                currentMonthActualEscalationRate = compoundedActualEscalationRate;
            }

            // Forecast rents for month in question
            BigDecimal forecastedActualRent = actualRentForecastService.calculateActualRentForMonth(unitTypeForecast.getStartingActualRent(), actualRent, currentMonthActualEscalationRate);
            BigDecimal forecastedMarketRent = marketRentForecastService.calculateMarketRentForMonth(marketEscalationRate, marketRent, forecastedActualRent, excessRentAdjustmentRate);


            // Compound actual escalation and determine actual escalation rate for next month
            compoundedActualEscalationRate = compoundedActualEscalationRate
                    .multiply(BigDecimal.ONE.add(
                                    Objects.requireNonNullElse(forecastMonth.getActualEscalationRate(), BigDecimal.ZERO)
                            )
                    );

            // We want to escalate the market rents the following month, last month/last year discarded if it exists
            marketEscalationRate = BigDecimal.ONE.add(forecastMonth.getMarketEscalationRate());

            RentForecastMonth rentMonth = RentForecastMonth.builder()
                    .month(forecastMonth.getMonth())
                    .year(forecastMonth.getYear())
                    .marketRent(forecastedMarketRent.setScale(DECIMAL_PLACES, RoundingMode.HALF_EVEN))
                    .actualRent(forecastedActualRent.setScale(DECIMAL_PLACES, RoundingMode.HALF_EVEN))
                    .build();

            // Update the tracker for current market rents for the unit type
            forecastedRentsByMonth.add(rentMonth);
            marketRent = forecastedMarketRent;
            actualRent = forecastedActualRent;
        }

        return forecastedRentsByMonth;
    }


    private Map<String, List<RentForecastYear>> summarizeYearsForAllUnitTypes(Map<String, List<RentForecastMonth>> forecastedRentMonthsByUnitType) {
        Map<String, List<RentForecastYear>> yearSummaryByUnitType = new HashMap<>();
        // Summarize the year for each unit type
        forecastedRentMonthsByUnitType.forEach((unitType, forecastedRentMonths) -> {
            List<RentForecastYear> rentYearSummaryForUnitType = summarizeYearsForAUnitType(forecastedRentMonths);
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

    private boolean isEscalationMonthForActual(UnitTypeForecast unitTypeForecast, int currentMonth) {
        // this works because our array is 0 indexed, so if we renew every 6 months, index 6 would actually be the 7th month
        return unitTypeForecast.getContractTerm() != null && (currentMonth % unitTypeForecast.getContractTerm()) == 0;
    }

}
