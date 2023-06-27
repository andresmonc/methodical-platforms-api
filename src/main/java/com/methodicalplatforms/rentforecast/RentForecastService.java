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


        for (ForecastMonth forecastMonth : forecastMonths) {
            // Get current month
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
            BigDecimal forecastedMarketRent = BigDecimal.ZERO;
            if (isStartMonth) {
                forecastedActualRent = Objects.requireNonNullElse(unitDetails.getStartingActualRent(), BigDecimal.ZERO);
                forecastedMarketRent = Objects.requireNonNullElse(unitDetails.getStartingMarketRent(), BigDecimal.ZERO);
            } else if (unitStarted) {
                forecastedActualRent = actualRentForecastService.calculateActualRentForMonth(unitDetails.getStartingActualRent(), actualRent, currentMonthActualEscalationRate);
                forecastedMarketRent = marketRentForecastService.calculateMarketRentForMonth(marketEscalationRate, marketRent, forecastedActualRent, excessRentAdjustmentRate);
            }


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
        LocalDate currentDate = LocalDate.of(closingDate.getYear() + currentYear - 1, currentMonth, 1);
        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(currentDate),
                YearMonth.from(startDate)
        );
        return monthsBetween == -1;
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
