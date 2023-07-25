package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.market.MarketRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.request.UnitDetails;
import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import com.methodicalplatforms.rentforecast.response.RentForecastMonth;
import com.methodicalplatforms.rentforecast.response.RentResponse;
import com.methodicalplatforms.rentforecast.response.UnitTypeForecastMonthly;
import com.methodicalplatforms.rentforecast.response.UnitTypeForecastYearly;
import com.methodicalplatforms.rentforecast.summary.RentForecastSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RentForecastService {

    private final ActualRentForecastService actualRentForecastService;
    private final MarketRentForecastService marketRentForecastService;
    private final RentForecastSummaryService rentForecastSummaryService;
    private static final int DECIMAL_PLACES = 15;

    @Autowired
    public RentForecastService(ActualRentForecastService actualRentForecastService, MarketRentForecastService marketRentForecastService, RentForecastSummaryService rentForecastSummaryService) {
        this.actualRentForecastService = actualRentForecastService;
        this.marketRentForecastService = marketRentForecastService;
        this.rentForecastSummaryService = rentForecastSummaryService;
    }

    /**
     * forecasts rents
     *
     * @param rentForecastRequest - details on what/how to forecast
     * @return - forecast response
     */
    public RentResponse forecastRents(RentForecastRequest rentForecastRequest) {
        LocalDate closingDate = rentForecastRequest.getClosingDate();
        Map<String, UnitTypeForecastMonthly> rentByMonths = forecastRentsForAllUnitTypes(rentForecastRequest.getUnitTypeForecastList(), closingDate);

        RentResponse.RentResponseBuilder rentResponseBuilder = RentResponse.builder();
        // Summarize by Year
        Map<String, UnitTypeForecastYearly> rentByYears = rentForecastSummaryService.summarizeYearsForAllUnitTypes(rentByMonths);
        rentForecastSummaryService.calculateFiscalYearSummaries(rentByMonths,rentByYears, closingDate);
        rentForecastSummaryService.yearlySummaryByUnitType(rentByYears);
        rentResponseBuilder.unitTypeUnitStatusView(rentForecastSummaryService.summarizeByUnitStatus(rentByYears, rentForecastRequest.getUnitTypeForecastList()));
        rentResponseBuilder.unitTypeForecastRentYears(rentByYears);
        // Summarize by unit type
        rentForecastSummaryService.summarizeAllUnitTypes(rentByMonths);
        rentResponseBuilder.unitTypeForecastRentMonths(rentByMonths);

        return rentResponseBuilder.build();
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
                    List<RentForecastMonth> unitTypeSummary = rentForecastSummaryService.summarizeUnitType(unitForecasts);

                    UnitTypeForecastMonthly unitTypeForecastMonthly = UnitTypeForecastMonthly.builder()
                            .unitTypeForecast(unitTypeSummary)
                            .unitForecasts(unitForecasts)
                            .build();

                    return Map.entry(unitTypeForecast.getUnitType(), unitTypeForecastMonthly);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
        boolean propertyStarted = false;

        for (ForecastMonth forecastMonth : forecastMonths) {
            // Get current month
            int month = forecastMonth.getMonth();
            int year = forecastMonth.getYear();
            boolean isUnitStartMonth = isStartMonth(unitDetails, month, year, closingDate);

            if (!unitStarted) {
                if (isUnitStarted(forecastMonth, closingDate, unitDetails.getStartDate())) {
                    unitStarted = true;
                }
            }

            if (!propertyStarted) {
                if (isPropertyStarted(forecastMonth, closingDate)) {
                    propertyStarted = true;
                }
            }

            BigDecimal currentMonthActualEscalationRate = getCurrentMonthActualEscalationRate(unitStarted, isEscalationMonthForActual(unitDetails, month, year, closingDate), isUnitStartMonth, compoundedActualEscalationRate);
            BigDecimal forecastedActualRent = calculateForecastedActualRent(isUnitStartMonth, unitStarted, unitDetails.getStartingActualRent(), actualRent, currentMonthActualEscalationRate);
            BigDecimal forecastedMarketRent = calculateForecastedMarketRent(propertyStarted, marketRent, unitDetails.getStartingMarketRent(), marketEscalationRate, forecastedActualRent, excessRentAdjustmentRate);
            compoundedActualEscalationRate = calculateCompoundedActualEscalationRate(compoundedActualEscalationRate, forecastMonth.getActualEscalationRate());
            marketEscalationRate = calculateMarketEscalationRate(forecastMonth.getMarketEscalationRate());
            forecastedRentsByMonth.add(newRentForecastMonth(year, month, setScale(forecastedMarketRent), setScale(forecastedActualRent)));
            marketRent = forecastedMarketRent;
            actualRent = forecastedActualRent;
        }
        return forecastedRentsByMonth;
    }

    private BigDecimal getCurrentMonthActualEscalationRate(boolean unitStarted, boolean isEscalationMonthForActual, boolean isUnitStartMonth, BigDecimal compoundedActualEscalationRate) {
        BigDecimal currentMonthActualEscalationRate = BigDecimal.ONE;
        if (unitStarted && (isEscalationMonthForActual || isUnitStartMonth)) {
            currentMonthActualEscalationRate = compoundedActualEscalationRate;
        }
        return currentMonthActualEscalationRate;
    }

    private BigDecimal calculateForecastedActualRent(boolean isUnitStartMonth, boolean unitStarted, BigDecimal startingActualRent, BigDecimal actualRent, BigDecimal currentMonthActualEscalationRate) {
        BigDecimal forecastedActualRent = BigDecimal.ZERO;
        if (isUnitStartMonth) {
            forecastedActualRent = Objects.requireNonNullElse(startingActualRent, BigDecimal.ZERO);
        } else if (unitStarted) {
            forecastedActualRent = actualRentForecastService.calculateActualRentForMonth(startingActualRent, actualRent, currentMonthActualEscalationRate);
        }
        return forecastedActualRent;
    }

    private BigDecimal calculateForecastedMarketRent(boolean propertyStarted, BigDecimal marketRent, BigDecimal startingMarketRent, BigDecimal marketEscalationRate, BigDecimal forecastedActualRent, BigDecimal excessRentAdjustmentRate) {
        BigDecimal forecastedMarketRent = BigDecimal.ZERO;
        if (propertyStarted && BigDecimal.ZERO.compareTo(marketRent) == 0) {
            forecastedMarketRent = Objects.requireNonNullElse(startingMarketRent, BigDecimal.ZERO);
        } else if (propertyStarted) {
            forecastedMarketRent = marketRentForecastService.calculateMarketRentForMonth(marketEscalationRate, marketRent, forecastedActualRent, excessRentAdjustmentRate);
        }
        return forecastedMarketRent;
    }

    private BigDecimal calculateCompoundedActualEscalationRate(BigDecimal compoundedActualEscalationRate, BigDecimal actualEscalationRate) {
        return compoundedActualEscalationRate.multiply(BigDecimal.ONE.add(Objects.requireNonNullElse(actualEscalationRate, BigDecimal.ZERO)));
    }

    private BigDecimal calculateMarketEscalationRate(BigDecimal marketEscalationRate) {
        return BigDecimal.ONE.add(marketEscalationRate);
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
        LocalDate currentDate = LocalDate.of(closingDate.getYear() + currentYear - 1, currentMonth, 1);
        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(unitDetails.getStartDate()),
                YearMonth.from(currentDate)
        );
        // If the unit start date hasn't occurred yet
        if (monthsBetween < 0) {
            return false;
        }
        long remainder = monthsBetween % unitDetails.getContractTerm();
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
        LocalDate currentDate = LocalDate.of(closingDate.getYear() + currentYear - 1, currentMonth, 1);
        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(currentDate),
                YearMonth.from(startDate)
        );
        return monthsBetween == 0;
    }

    private boolean isPropertyStarted(ForecastMonth forecastMonth, LocalDate closingDate) {
        if (closingDate == null) {
            return true;
        }

        LocalDate calcDate = closingDate.withMonth(forecastMonth.getMonth())
                .plusYears(forecastMonth.getYear() - 1);
        closingDate = closingDate.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate calcDateLastDayOfMonth = calcDate.withDayOfMonth(calcDate.lengthOfMonth())
                .with(TemporalAdjusters.lastDayOfMonth()); // Set to last day of the month
        return calcDateLastDayOfMonth.isAfter(closingDate);
    }


    /**
     * Determine whether the unit is already started
     *
     * @param forecastMonth - the forecastMonth in question to determine calc date
     * @param closingDate   - the closing date for the property
     * @param unitStartDate - the start date for the unit
     * @return - true if unit is started false otherwise
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
                .plusYears(forecastMonth.getYear()-1);
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
