package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.market.MarketRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.request.UnitDetails;
import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import com.methodicalplatforms.rentforecast.response.RentForecastMonth;
import com.methodicalplatforms.rentforecast.response.RentForecastYear;
import com.methodicalplatforms.rentforecast.response.RentResponse;
import com.methodicalplatforms.rentforecast.response.UnitTypeForecastMonthly;
import com.methodicalplatforms.rentforecast.response.UnitTypeForecastYearly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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

    /**
     * forecasts rents
     *
     * @param rentForecastRequest - details on what/how to forecast
     * @return - forecast response
     */
    public RentResponse forecastRents(RentForecastRequest rentForecastRequest) {
        Map<String, UnitTypeForecastMonthly> rentByMonths = forecastRentsForAllUnitTypes(rentForecastRequest.getUnitTypeForecastList(), rentForecastRequest.getClosingDate());

        RentResponse.RentResponseBuilder rentResponseBuilder = RentResponse.builder();
        // Summarize by Year
        Map<String, UnitTypeForecastYearly> rentByYears = summarizeYearsForAllUnitTypes(rentByMonths);
        yearlySummaryByUnitType(rentByYears);
        rentResponseBuilder.unitTypeUnitStatusView(summarizeByUnitStatus(rentByYears, rentForecastRequest.getUnitTypeForecastList()));
        rentResponseBuilder.unitTypeForecastRentYears(rentByYears);
        // Summarize by unit type
        summarizeAllUnitTypes(rentByMonths);
        rentResponseBuilder.unitTypeForecastRentMonths(rentByMonths);

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

    /**
     * Summarize all unit types at a yearly level
     *
     * @param rentYearsByUnitType - the yearly summaries for individual units
     */
    private void yearlySummaryByUnitType(Map<String, UnitTypeForecastYearly> rentYearsByUnitType) {
        UnitTypeForecastYearly allUnitTypeForecastYearly = new UnitTypeForecastYearly();
        List<RentForecastYear> unitTypeSummary = new ArrayList<>();

        rentYearsByUnitType.forEach((unitName, rentForecastMonths) -> {
            for (int i = 0; i < rentForecastMonths.getUnitTypeForecast().size(); i++) {
                RentForecastYear rentForecastMonth = rentForecastMonths.getUnitTypeForecast().get(i);
                if (unitTypeSummary.size() <= i) {
                    unitTypeSummary.add(rentForecastMonth);
                } else {
                    RentForecastYear unitTypeForecastYear = unitTypeSummary.get(i);
                    unitTypeForecastYear.setActualRent(unitTypeForecastYear.getActualRent().add(rentForecastMonth.getActualRent()));
                    unitTypeForecastYear.setMarketRent(unitTypeForecastYear.getMarketRent().add(rentForecastMonth.getMarketRent()));
                }
            }
        });
        allUnitTypeForecastYearly.setUnitTypeForecast(unitTypeSummary);
        rentYearsByUnitType.put(ALL_UNITS, allUnitTypeForecastYearly);
    }

    /**
     * Forecast rents for each unit type
     *
     * @param unitTypeForecastList - the list of unit types and their corresponding forecast data
     * @return - a map of forecasts by unit type
     */
    private Map<String, UnitTypeForecastMonthly> forecastRentsForAllUnitTypes(List<UnitTypeForecast> unitTypeForecastList, LocalDate closingDate) {
        return unitTypeForecastList.parallelStream()
                .map(unitTypeForecast -> {
                    Map<String, List<RentForecastMonth>> unitForecasts = forecastMonthlyRentsForAllUnits(unitTypeForecast, closingDate);
                    List<RentForecastMonth> unitTypeSummary = summarizeUnitType(unitForecasts);

                    UnitTypeForecastMonthly unitTypeForecastMonthly = UnitTypeForecastMonthly.builder()
                            .unitTypeForecast(unitTypeSummary)
                            .unitForecasts(unitForecasts)
                            .build();

                    return Map.entry(unitTypeForecast.getUnitType(), unitTypeForecastMonthly);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    /**
     * Sum up individual units for a given Unit Type to provide a summary
     *
     * @param unitForecastData - the forecast details for a unit type
     * @return - the summed up monthly values for a unit type
     */
    public List<RentForecastMonth> summarizeUnitType(Map<String, List<RentForecastMonth>> unitForecastData) {
        List<RentForecastMonth> unitTypeSummary = new ArrayList<>();

        unitForecastData.forEach((unitName, rentForecastMonths) -> {
            for (int i = 0; i < rentForecastMonths.size(); i++) {
                RentForecastMonth rentForecastMonth = rentForecastMonths.get(i);
                if (unitTypeSummary.size() <= i) {
                    unitTypeSummary.add(RentForecastMonth.builder().marketRent(BigDecimal.ZERO).actualRent(BigDecimal.ZERO).month(rentForecastMonth.getMonth()).year(rentForecastMonth.getYear()).build());
                }
                RentForecastMonth unitTypeRentforecastMonth = unitTypeSummary.get(i);
                unitTypeRentforecastMonth.setActualRent(unitTypeRentforecastMonth.getActualRent().add(rentForecastMonth.getActualRent()));
                unitTypeRentforecastMonth.setMarketRent(unitTypeRentforecastMonth.getMarketRent().add(rentForecastMonth.getMarketRent()));
            }

        });
        return unitTypeSummary;
    }

    /**
     * Forecast rents for all units for a given unit type
     *
     * @param unitTypeForecast - the unit type forecast details
     * @return - unit forecasts
     */
    private Map<String, List<RentForecastMonth>> forecastMonthlyRentsForAllUnits(UnitTypeForecast unitTypeForecast, LocalDate closingDate) {
        // Sort the escalation months
        List<ForecastMonth> sortedForecastMonths = unitTypeForecast.getForecastMonthData().stream()
                .sorted(Comparator.comparingInt(ForecastMonth::getYear).thenComparingInt(ForecastMonth::getMonth))
                .collect(Collectors.toList());

        // Process units using stream
        return unitTypeForecast.getUnitDetails().entrySet().parallelStream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    UnitDetails unitDetails = entry.getValue();

                    return forecastRentsByMonthForUnit(
                            sortedForecastMonths,
                            unitTypeForecast.getExcessRentAdjustmentRate(),
                            unitDetails, closingDate
                    );
                }));
    }


    /**
     * forecast the rents for all months/years for a specific unit
     *
     * @param forecastMonths           - contains the data for each month in question
     * @param excessRentAdjustmentRate - the rate in which to modify loss to lease
     * @param unitDetails              - the details for a particular unit
     * @param closingDate              - the closing date for the property
     * @return - forecasted data for a unit
     */
    private List<RentForecastMonth> forecastRentsByMonthForUnit(List<ForecastMonth> forecastMonths, BigDecimal
            excessRentAdjustmentRate, UnitDetails unitDetails, LocalDate closingDate) {
        List<RentForecastMonth> forecastedRentsByMonth = new ArrayList<>();

        // Track values during calculation
        BigDecimal marketRent = Objects.requireNonNullElse(unitDetails.getStartingMarketRent(), BigDecimal.ZERO);
        BigDecimal actualRent = Objects.requireNonNullElse(unitDetails.getStartingActualRent(), BigDecimal.ZERO);
        BigDecimal compoundedActualEscalationRate = BigDecimal.ONE;
        BigDecimal marketEscalationRate = BigDecimal.ONE;
        boolean unitStarted = false;


        for (int i = 0; i < forecastMonths.size(); i++) {
            // Get current month
            ForecastMonth forecastMonth = forecastMonths.get(i);
            int month = forecastMonth.getMonth();
            int year = forecastMonth.getYear();
            boolean isStartMonth = isStartMonth(unitDetails, month, year, closingDate);

            if (!unitStarted) {
                if (isUnitStarted(forecastMonth, closingDate, unitDetails.getStartDate())) {
                    unitStarted = true;
                }
            }

            BigDecimal currentMonthActualEscalationRate = BigDecimal.ONE;
            // Only escalate actual if it's the month after contract end
            if (unitStarted && (isEscalationMonthForActual(unitDetails, month, year, closingDate) || isStartMonth)) {
                currentMonthActualEscalationRate = compoundedActualEscalationRate;
            }

            // Forecast rents for month in question
            BigDecimal forecastedActualRent = BigDecimal.ZERO;
            if (isStartMonth) {
                forecastedActualRent = Objects.requireNonNullElse(unitDetails.getStartingActualRent(), BigDecimal.ZERO);
            } else if (unitStarted) {
                forecastedActualRent = actualRentForecastService.calculateActualRentForMonth(unitDetails.getStartingActualRent(), actualRent, currentMonthActualEscalationRate);
            }
            BigDecimal forecastedMarketRent = marketRentForecastService.calculateMarketRentForMonth(marketEscalationRate, marketRent, forecastedActualRent, excessRentAdjustmentRate);


            // Compound actual escalation and determine actual escalation rate for next month
            compoundedActualEscalationRate = compoundedActualEscalationRate
                    .multiply(BigDecimal.ONE.add(
                                    Objects.requireNonNullElse(forecastMonth.getActualEscalationRate(), BigDecimal.ZERO)
                            )
                    );

            // We want to escalate the market rents the following month, last month/last year discarded if it exists
            marketEscalationRate = BigDecimal.ONE.add(forecastMonth.getMarketEscalationRate());

            // Update the tracker for current market rents for the unit type
            forecastedRentsByMonth.add(newRentForecastMonth(year, month, setScale(forecastedMarketRent), setScale(forecastedActualRent)));
            marketRent = forecastedMarketRent;
            actualRent = forecastedActualRent;
        }
        return forecastedRentsByMonth;
    }

    private RentForecastMonth newRentForecastMonth(int year, int month, BigDecimal marketRent, BigDecimal actualRent) {
        return RentForecastMonth.builder()
                .month(month)
                .year(year)
                .marketRent(marketRent)
                .actualRent(actualRent)
                .build();
    }

    private BigDecimal setScale(BigDecimal bigDecimal) {
        if (BigDecimal.ZERO.compareTo(bigDecimal) == 0) {
            return bigDecimal;
        }
        return bigDecimal.setScale(DECIMAL_PLACES, RoundingMode.HALF_EVEN);
    }


    /**
     * Provide a summary for each unit type at a yearly level
     *
     * @param forecastedRentMonthsByUnitType
     * @return
     */
    private Map<String, UnitTypeForecastYearly> summarizeYearsForAllUnitTypes(Map<String, UnitTypeForecastMonthly> forecastedRentMonthsByUnitType) {
        Map<String, UnitTypeForecastYearly> unitTypeForecastYearlyMap = new HashMap<>();

        // Summarize the year for each unit type
        forecastedRentMonthsByUnitType.forEach((unitType, unitTypeForecastMonthly) -> {
            UnitTypeForecastYearly unitTypeForecastYearly = new UnitTypeForecastYearly();
            Map<String, List<RentForecastYear>> unitForecastsYearly = new HashMap<>();
            // Summarize the years for each unit
            unitTypeForecastMonthly.getUnitForecasts().forEach((unitId, rentForecastMonths) -> {
                List<RentForecastYear> rentYearSummaryForUnitType = summarizeYearsForAUnitType(rentForecastMonths);
                unitForecastsYearly.put(unitId, rentYearSummaryForUnitType);
            });
            List<RentForecastYear> rentYearSummaryForUnitType = summarizeYearsForAUnitType(unitTypeForecastMonthly.getUnitTypeForecast());

            // Save results
            unitTypeForecastYearly.setUnitForecasts(unitForecastsYearly);
            unitTypeForecastYearly.setUnitTypeForecast(rentYearSummaryForUnitType);
            unitTypeForecastYearlyMap.put(unitType, unitTypeForecastYearly);
        });

        return unitTypeForecastYearlyMap;
    }

    /**
     * summarize yearly data for units by status
     *
     * @param yearlySummaries   - yearly summary data for all units
     * @param unitTypeForecasts - unit type forecast data
     * @return
     */
    private Map<String, UnitTypeForecastYearly> summarizeByUnitStatus(Map<String, UnitTypeForecastYearly> yearlySummaries, List<UnitTypeForecast> unitTypeForecasts) {
        Map<String, UnitTypeForecastYearly> unitTypeForecastYearlyMap = new HashMap<>();
        unitTypeForecasts.forEach(unitTypeForecast -> {
            String unitType = unitTypeForecast.getUnitType();
            UnitTypeForecastYearly unitTypeForecastYearly = new UnitTypeForecastYearly();
            Map<String, List<RentForecastYear>> unitStatusForecasts = new HashMap<>();
            unitTypeForecast.getUnitDetails().forEach((unitId, unitDetails) -> {
                String unitStatus = unitDetails.getUnitStatus();
                if (!unitStatusForecasts.containsKey(unitStatus)) {
                    unitStatusForecasts.put(unitStatus, new ArrayList<>());
                }
                List<RentForecastYear> rentForecastYearsForUnitId = yearlySummaries.get(unitType).getUnitForecasts().get(unitId);
                sumTwoForecastYears(unitStatusForecasts.get(unitStatus), rentForecastYearsForUnitId);
                unitTypeForecastYearly.setUnitForecasts(unitStatusForecasts);
            });
            unitTypeForecastYearlyMap.put(unitTypeForecast.getUnitType(), unitTypeForecastYearly);
        });
        return unitTypeForecastYearlyMap;
    }

    /**
     * Sum 2 yearly forecast summaries
     *
     * @param forecastYearsToAddTo   - the list of years to add to
     * @param forecastYearsToAddFrom - the list of years to add from
     */
    private void sumTwoForecastYears(List<RentForecastYear> forecastYearsToAddTo, List<RentForecastYear> forecastYearsToAddFrom) {
        for (int i = 0; i < forecastYearsToAddFrom.size(); i++) {
            RentForecastYear forecastYearFrom = forecastYearsToAddFrom.get(i);
            if (forecastYearsToAddTo.size() <= i) {
                forecastYearsToAddTo.add(RentForecastYear.builder().year(forecastYearFrom.getYear()).marketRent(BigDecimal.ZERO).actualRent(BigDecimal.ZERO).build());
            }
            RentForecastYear forecastYearTo = forecastYearsToAddTo.get(i);

            forecastYearTo.setActualRent(forecastYearTo.getActualRent().add(forecastYearFrom.getActualRent()));
            forecastYearTo.setMarketRent(forecastYearTo.getMarketRent().add(forecastYearFrom.getMarketRent()));
        }
    }

    /**
     * yearly summary for an individual unit type
     *
     * @param rentMonths
     * @return
     */
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
        return yearSummaries.values().stream().collect(Collectors.toList());
    }

    /**
     * Should we escalate this month in question?
     *
     * @param unitDetails  - details for a unit
     * @param currentMonth - current month index
     * @return - true or false, whether we should escalate
     */
    private boolean isEscalationMonthForActual(UnitDetails unitDetails, int currentMonth, int currentYear, LocalDate closingDate) {
        if (closingDate == null || unitDetails.getStartDate() == null) {
            // this works because our array is 0 indexed, so if we renew every 6 months, index 6 would actually be the 7th month
            return unitDetails.getContractTerm() != null && (currentMonth % unitDetails.getContractTerm()) == 0;
        }
        LocalDate currentDate = LocalDate.of(closingDate.getYear() + currentYear, currentMonth, 1);
        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(currentDate),
                YearMonth.from(unitDetails.getStartDate()
                )
        );
        long remainder = monthsBetween % unitDetails.getContractTerm() + 1;
        return remainder == 0;
    }

    /**
     * Determine if the current month we're forecasting is the start month
     *
     * @param unitDetails
     * @param currentMonth
     * @param currentYear
     * @param closingDate
     * @return
     */
    private boolean isStartMonth(UnitDetails unitDetails, int currentMonth, int currentYear, LocalDate closingDate) {
        if (closingDate == null) {
            return false;
        }
        LocalDate startDate = unitDetails.getStartDate();
        if (unitDetails.getStartDate() == null) {
            startDate = closingDate;
        }
        LocalDate currentDate = LocalDate.of(closingDate.getYear() + currentYear, currentMonth, 1);
        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(currentDate),
                YearMonth.from(startDate)
        );
        return monthsBetween == -1;
    }

    /**
     * Determine whether or not the unit is already started
     *
     * @param forecastMonth
     * @param closingDate
     * @param unitStartDate
     * @return
     */
    private boolean isUnitStarted(ForecastMonth forecastMonth, LocalDate closingDate, LocalDate unitStartDate) {
        if (closingDate == null) {
            return true;
        }

        // Calculate the calculation date and set it to the last day of the month
        // this is to make sure that if a unit starts on the last day of the month or middle of the month
        // it will reflect in following month actual
        // todo: in v2 we would prorate and this would need to change
        LocalDate calcDate = closingDate.withMonth(forecastMonth.getMonth())
                .plusYears(forecastMonth.getYear());
        LocalDate calcDateLastDayOfMonth = calcDate.withDayOfMonth(calcDate.lengthOfMonth());
        // if calcDate is before closing date we can't start calculating actuals
        if (!(calcDateLastDayOfMonth.isEqual(closingDate) || calcDateLastDayOfMonth.isAfter(closingDate))) {
            return false;
        }

        if (unitStartDate == null) {
            unitStartDate = closingDate;
        }


        return calcDateLastDayOfMonth.isAfter(unitStartDate.withDayOfMonth(unitStartDate.lengthOfMonth()));
    }

}
