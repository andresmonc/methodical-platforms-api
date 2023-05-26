package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.market.MarketRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import com.methodicalplatforms.rentforecast.request.RentForecastOptions;
import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.request.UnitDetails;
import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import com.methodicalplatforms.rentforecast.response.RentForecastMonth;
import com.methodicalplatforms.rentforecast.response.RentForecastYear;
import com.methodicalplatforms.rentforecast.response.RentResponse;
import com.methodicalplatforms.rentforecast.response.UnitTypeForecastMonthly;
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
import java.util.concurrent.CompletableFuture;

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

    /**
     * forecasts rents
     *
     * @param rentForecastRequest - details on what/how to forecast
     * @return - forecast response
     */
    public RentResponse forecastRents(RentForecastRequest rentForecastRequest) {
        Map<String, UnitTypeForecastMonthly> rentByMonths = forecastRentsForAllUnitTypes(rentForecastRequest.getUnitTypeForecastList());

        RentResponse.RentResponseBuilder rentResponseBuilder = RentResponse.builder();
        RentForecastOptions options = rentForecastRequest.getOptions();
        if (options != null && rentForecastRequest.getOptions().getSummarizeByYear()) {
            // Summarize by Year
//            Map<String, UnitTypeForecastYearly> rentByYears = summarizeYearsForAllUnitTypes(rentByMonths);
//            if (options.getSummarizeByUnitType()) {
//                // Summarize by unit type
//                rentByYears = yearlySummaryByUnitType(rentByYears);
//            }
//            rentResponseBuilder.unitTypeMarketRentYears(rentByYears);
        } else {
            // Monthly summary
            if (options != null && options.getSummarizeByUnitType()) {
                // Summarize by unit type
                summarizeAllUnitTypes(rentByMonths);
            }
            rentResponseBuilder.unitTypeMarketRentMonths(rentByMonths);
        }

        return rentResponseBuilder.build();
    }

    /**
     * Summarizes the data for all unit types on a monthly basis
     *
     * @param rentMonthsByUnitType - map of forecasts by unit type
     */
    private void summarizeAllUnitTypes(Map<String, UnitTypeForecastMonthly> rentMonthsByUnitType) {
        UnitTypeForecastMonthly allUnitsForecastSummary = UnitTypeForecastMonthly.builder()
                .unitTypeForecast(new ArrayList<>()).build();

        rentMonthsByUnitType.forEach((ignored, unitTypeForecastMonthly) -> {
            List<RentForecastMonth> allUnitsForecasts = allUnitsForecastSummary.getUnitTypeForecast();
            List<RentForecastMonth> unitTypeForecast = unitTypeForecastMonthly.getUnitTypeForecast();
            for (int i = 0; i < unitTypeForecast.size(); i++) {
                RentForecastMonth unitTypeRentForecastMonth = unitTypeForecast.get(i);
                if (allUnitsForecasts.size() <= i) {
                    allUnitsForecasts.add(RentForecastMonth.builder().month(unitTypeRentForecastMonth.getMonth())
                            .actualRent(BigDecimal.ZERO).marketRent(BigDecimal.ZERO)
                            .year(unitTypeRentForecastMonth.getYear()).build());
                }
                RentForecastMonth allUnitsForecastMonth = allUnitsForecasts.get(i);
                BigDecimal unitTypeForecastMonthMarketRent = unitTypeRentForecastMonth.getMarketRent();
                BigDecimal unitTypeForecastMonthActualRent = unitTypeRentForecastMonth.getActualRent();
                allUnitsForecastMonth.setActualRent(allUnitsForecastMonth.getActualRent().add(unitTypeForecastMonthActualRent));
                allUnitsForecastMonth.setMarketRent(allUnitsForecastMonth.getMarketRent().add(unitTypeForecastMonthMarketRent));

            }
        });

        rentMonthsByUnitType.put(ALL_UNITS, allUnitsForecastSummary);
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

    /**
     * Forecast rents for each unit type
     *
     * @param unitTypeForecastList - the list of unit types and their corresponding forecast data
     * @return - a map of forecasts by unit type
     */
    private Map<String, UnitTypeForecastMonthly> forecastRentsForAllUnitTypes(List<UnitTypeForecast> unitTypeForecastList) {
        Map<String, UnitTypeForecastMonthly> forecastDataByUnitTypeMonthly = new HashMap<>();

        List<CompletableFuture<Void>> futures = unitTypeForecastList.stream().map(unitTypeForecast -> CompletableFuture.supplyAsync(() -> {
            Map<String, List<RentForecastMonth>> unitForecasts = forecastMonthlyRentsForAllUnits(unitTypeForecast);
            List<RentForecastMonth> unitTypeSummary = summarizeUnitType(unitForecasts);

            UnitTypeForecastMonthly unitTypeForecastMonthly = UnitTypeForecastMonthly.builder().unitTypeForecast(unitTypeSummary).unitForecasts(unitForecasts).build();
            return Map.entry(unitTypeForecast.getUnitType(), unitTypeForecastMonthly);
        })).map(future -> future.thenAcceptAsync(entry -> forecastDataByUnitTypeMonthly.put(entry.getKey(), entry.getValue()))).toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        return forecastDataByUnitTypeMonthly;
    }

    public List<RentForecastMonth> summarizeUnitType(Map<String, List<RentForecastMonth>> unitForecastData) {
        List<RentForecastMonth> unitTypeSummary = new ArrayList<>();

        unitForecastData.forEach((unitName, rentForecastMonths) -> {
            for (int i = 0; i < rentForecastMonths.size(); i++) {
                RentForecastMonth rentForecastMonth = rentForecastMonths.get(i);
                if (unitTypeSummary.size() <= i) {
                    unitTypeSummary.add(rentForecastMonth);
                } else {
                    RentForecastMonth unitTypeRentforecastMonth = unitTypeSummary.get(i);
                    unitTypeRentforecastMonth.setActualRent(unitTypeRentforecastMonth.getActualRent().add(rentForecastMonth.getActualRent()));
                    unitTypeRentforecastMonth.setMarketRent(unitTypeRentforecastMonth.getMarketRent().add(rentForecastMonth.getMarketRent()));
                }
            }

        });
        return unitTypeSummary;
    }

    private Map<String, List<RentForecastMonth>> forecastMonthlyRentsForAllUnits(UnitTypeForecast unitTypeForecast) {
        Map<String, List<RentForecastMonth>> unitForecasts = new HashMap<>();

        // Sort the escalation months
        List<ForecastMonth> sortedForecastMonths = unitTypeForecast.getForecastMonthData().stream().sorted(Comparator.comparingInt(ForecastMonth::getYear).thenComparingInt(ForecastMonth::getMonth)).toList();

        // Process Units concurrently
        List<CompletableFuture<Void>> futures = unitTypeForecast.getUnitDetails().entrySet().stream().map(entry -> CompletableFuture.supplyAsync(() -> {
            UnitDetails unitDetails = entry.getValue();
            List<RentForecastMonth> forecastedRentsByMonth = forecastRentsByMonthForUnit(sortedForecastMonths, unitTypeForecast.getExcessRentAdjustmentRate(), unitDetails);
            return Map.entry(entry.getKey(), forecastedRentsByMonth);
        })).map(future -> future.thenAcceptAsync(entry -> unitForecasts.put(entry.getKey(), entry.getValue()))).toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        return unitForecasts;
    }


    /**
     * forecast the rents for all months/years for a specific unit
     *
     * @param forecastMonths           - contains the data for each month in question
     * @param excessRentAdjustmentRate - the rate in which to modify loss to lease
     * @param unitDetails              - the details for a particular unit
     * @return - forecasted data for a unit
     */
    private List<RentForecastMonth> forecastRentsByMonthForUnit(List<ForecastMonth> forecastMonths, BigDecimal
            excessRentAdjustmentRate, UnitDetails unitDetails) {
        List<RentForecastMonth> forecastedRentsByMonth = new ArrayList<>();

        // Track the rent values for the unit type
        BigDecimal marketRent = Objects.requireNonNullElse(unitDetails.getStartingMarketRent(), BigDecimal.ZERO);
        BigDecimal actualRent = Objects.requireNonNullElse(unitDetails.getStartingActualRent(), BigDecimal.ZERO);
        BigDecimal compoundedActualEscalationRate = BigDecimal.ONE;
        BigDecimal marketEscalationRate = BigDecimal.ONE;


        for (int i = 0; i < forecastMonths.size(); i++) {
            // Get current month
            ForecastMonth forecastMonth = forecastMonths.get(i);

            BigDecimal currentMonthActualEscalationRate = BigDecimal.ONE;
            // Only escalate actual if it's the month after contract end
            if (isEscalationMonthForActual(unitDetails, i)) {
                currentMonthActualEscalationRate = compoundedActualEscalationRate;
            }

            // Forecast rents for month in question
            BigDecimal forecastedActualRent = actualRentForecastService.calculateActualRentForMonth(unitDetails.getStartingActualRent(), actualRent, currentMonthActualEscalationRate);
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

    /**
     * Should we escalate this month in question?
     *
     * @param unitDetails  - details for a unit
     * @param currentMonth - current month index
     * @return - true or false, whether or not we should escalate
     */
    private boolean isEscalationMonthForActual(UnitDetails unitDetails, int currentMonth) {
        // this works because our array is 0 indexed, so if we renew every 6 months, index 6 would actually be the 7th month
        return unitDetails.getContractTerm() != null && (currentMonth % unitDetails.getContractTerm()) == 0;
    }

}
