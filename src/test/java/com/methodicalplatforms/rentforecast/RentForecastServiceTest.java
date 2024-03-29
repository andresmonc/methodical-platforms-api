package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.market.MarketRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.request.UnitDetails;
import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import com.methodicalplatforms.rentforecast.summary.RentForecastSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentForecastServiceTest {

    public RentForecastService rentForecastService;
    ActualRentForecastService actualRentForecastService = new ActualRentForecastService();
    private static final String UNIT_TYPE_1BR_1BATH = "1 BR 1 BATH";
    private static final String UNIT_101 = "101";
    private static final String UNIT_102 = "102";
    private static final String READY = "READY";
    private static final String NOT_READY = "NOT READY";


    @BeforeEach
    public void setup() {
        rentForecastService = new RentForecastService(actualRentForecastService, new MarketRentForecastService(), new RentForecastSummaryService());
    }

    @Test
    void forecastRentsMonthlySingleUnitType() {
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(UNIT_101, createUnitDetails(6, "", 1000, 700, LocalDate.of(2020, 1, 1))))
                        .excessRentAdjustmentRate(BigDecimal.valueOf(.15))
                        .forecastMonthData(forecastMonthTestData())
                        .build()
        );
        request.setClosingDate(LocalDate.of(2020,1,1));

        var marketValueResponse = rentForecastService.forecastRents(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeForecastRentMonths().size());

        var marketRentMonths = marketValueResponse.getUnitTypeForecastRentMonths().get(UNIT_TYPE_1BR_1BATH).getUnitForecasts().get(UNIT_101);
        assertEquals(36, marketRentMonths.size());
        assertEquals(0, BigDecimal.valueOf(1050.00).compareTo(marketRentMonths.get(4).getMarketRent()));
        assertEquals(2, marketRentMonths.get(1).getMonth(), "Response is not sorted");
        assertEquals(0, BigDecimal.valueOf(910.00).compareTo(marketRentMonths.get(7).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1102.50).compareTo(marketRentMonths.get(8).getMarketRent()));
        assertEquals(0, BigDecimal.valueOf(910.00).compareTo(marketRentMonths.get(11).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1537.90).compareTo(marketRentMonths.get(13).getMarketRent()));
        assertEquals(0, BigDecimal.valueOf(1599.42).compareTo(marketRentMonths.get(16).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1537.90).compareTo(marketRentMonths.get(15).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1663.39).compareTo(marketRentMonths.get(20).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1599.42).compareTo(marketRentMonths.get(19).getActualRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1746.56).compareTo(marketRentMonths.get(24).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1599.42).compareTo(marketRentMonths.get(23).getActualRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1833.89).compareTo(marketRentMonths.get(28).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1729.93).compareTo(marketRentMonths.get(27).getActualRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1925.58).compareTo(marketRentMonths.get(32).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1781.83).compareTo(marketRentMonths.get(31).getActualRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1925.58).compareTo(marketRentMonths.get(35).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1781.83).compareTo(marketRentMonths.get(35).getActualRent().setScale(2, RoundingMode.HALF_EVEN)));
    }

    @Test
    void calculateMarketRentMonthlyMultipleUnitType() {
        var unitType2 = "2 BR 2 BATH";
        var request = createMarketRentRequest(
                // First Unit
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(UNIT_101, createUnitDetails(0, "", 1000, 0)))
                        .forecastMonthData(List.of(
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build(),
                // Second Unit
                UnitTypeForecast.builder()
                        .unitType(unitType2)
                        .unitDetails(Map.of("102", createUnitDetails(0, "", 2000, 0)))
                        .forecastMonthData(List.of(
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()

        );

        var forecastResponse = rentForecastService.forecastRents(request);

        assertNotNull(forecastResponse);
        assertEquals(3, forecastResponse.getUnitTypeForecastRentMonths().size());

        var marketRentMonths1 = forecastResponse.getUnitTypeForecastRentMonths().get(UNIT_TYPE_1BR_1BATH).getUnitForecasts().get(UNIT_101);
        assertEquals(4, marketRentMonths1.size());
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(marketRentMonths1.get(1).getMarketRent()));

        var marketRentMonths2 = forecastResponse.getUnitTypeForecastRentMonths().get(unitType2).getUnitForecasts().get("102");
        assertEquals(3, marketRentMonths2.size());
        assertEquals(0, BigDecimal.valueOf(2200).compareTo(marketRentMonths2.get(1).getMarketRent()));
    }

    @Test
    void calculateMarketRentYearlySummarySingleUnitType() {
        var unitType = "1 BR 1 BATH";
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(unitType)
                        .unitDetails(Map.of(UNIT_101, createUnitDetails(0, "", 1000, 0)))
                        .forecastMonthData(
                                List.of(
                                        createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                        createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                        createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                        createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                                )
                        )
                        .build()
        );

        var marketValueResponse = rentForecastService.forecastRents(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeForecastRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeForecastRentYears().get(unitType).getUnitForecasts().get(UNIT_101);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4300)));
    }

    @Test
    void forecastRentsMonthlySingleUnitTypeClosingDateAndStartDate() {
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(UNIT_101, createUnitDetails(6, "", 1000, 700, LocalDate.of(2023, 8, 15)))
                        )
                        .excessRentAdjustmentRate(BigDecimal.valueOf(.15))
                        .forecastMonthData(forecastMonthTestData())
                        .build()
        );
        request.setClosingDate(LocalDate.of(2023, 6, 10));

        var marketValueResponse = rentForecastService.forecastRents(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeForecastRentMonths().size());

        var forecastMonths = marketValueResponse.getUnitTypeForecastRentMonths().get(UNIT_TYPE_1BR_1BATH).getUnitForecasts().get(UNIT_101);
        assertEquals(36, forecastMonths.size());
        assertEquals(0, BigDecimal.valueOf(0).compareTo(forecastMonths.get(4).getMarketRent()));
        assertEquals(2, forecastMonths.get(1).getMonth(), "Response is not sorted");
        assertEquals(0, BigDecimal.ZERO.compareTo(forecastMonths.get(5).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1000.00).compareTo(forecastMonths.get(7).getMarketRent()));
        assertEquals(0, BigDecimal.valueOf(700).compareTo(forecastMonths.get(7).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1050).compareTo(forecastMonths.get(11).getMarketRent()));
        assertEquals(0, BigDecimal.valueOf(1599.42).compareTo(forecastMonths.get(16).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1537.90).compareTo(forecastMonths.get(15).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1663.39).compareTo(forecastMonths.get(20).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1599.42).compareTo(forecastMonths.get(19).getActualRent().setScale(2, RoundingMode.HALF_EVEN)));
        assertEquals(0, BigDecimal.valueOf(1746.56).compareTo(forecastMonths.get(24).getMarketRent().setScale(2, RoundingMode.HALF_EVEN)));
    }

    @Test
    void calculateMarketRentYearlySummaryMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(unitType1)
                        .unitDetails(Map.of("101", createUnitDetails(0, "", 1000, 0)))
                        .forecastMonthData(List.of(
                                // First Unit
                                createForecastMonth(0, 1, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 4, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build(),
                UnitTypeForecast.builder()
                        .unitType(unitType2)
                        .unitDetails(Map.of(UNIT_102, createUnitDetails(0, "", 2000, 0)))
                        .forecastMonthData(List.of(
                                // Second Unit
                                createForecastMonth(0, 1, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()
        );

        var marketValueResponse = rentForecastService.forecastRents(request);

        assertNotNull(marketValueResponse);
        assertEquals(3, marketValueResponse.getUnitTypeForecastRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeForecastRentYears().get(unitType1).getUnitForecasts().get(UNIT_101);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4300)));

        var unitType2MarketYear = marketValueResponse.getUnitTypeForecastRentYears().get(unitType2).getUnitForecasts().get(UNIT_102);
        assertEquals(1, unitType2MarketYear.size());
        year0MarketValue = unitType2MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(6400)));
    }

    @Test
    void unitStatusViewYearly() {
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(
                                UNIT_101, createUnitDetails(0, READY, 1000, 1000),
                                UNIT_102, createUnitDetails(0, NOT_READY, 2000, 1000),
                                "103", createUnitDetails(0, NOT_READY, 2000, 1000)

                        ))
                        .forecastMonthData(List.of(
                                // First Unit
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()
        );

        var forecastResponse = rentForecastService.forecastRents(request);
        var unitTypeUnitStatusView = forecastResponse.getUnitTypeUnitStatusView().get(UNIT_TYPE_1BR_1BATH);
        var unitTypeUnitStatus1Br1Bath = unitTypeUnitStatusView.getUnitForecasts();
        var readySummary = unitTypeUnitStatus1Br1Bath.get(READY);
        var notReadySummary = unitTypeUnitStatus1Br1Bath.get(NOT_READY);

        assertNotEquals(BigDecimal.ZERO, notReadySummary.get(0).getActualRent());
        assertNotEquals(BigDecimal.ZERO, notReadySummary.get(0).getMarketRent());
        assertNotEquals(BigDecimal.ZERO, readySummary.get(0).getActualRent());
        assertNotEquals(BigDecimal.ZERO, readySummary.get(0).getMarketRent());

        // test year 0
        var year0Ready = readySummary.get(0);
        assertEquals(0, year0Ready.getMarketRent().compareTo(BigDecimal.valueOf(4300)));
        assertEquals(0, year0Ready.getActualRent().compareTo(BigDecimal.valueOf(4000)));
        var year0NotReady = notReadySummary.get(0);
        assertEquals(0, year0NotReady.getMarketRent().compareTo(BigDecimal.valueOf(17200)));
        assertEquals(0, year0NotReady.getActualRent().compareTo(BigDecimal.valueOf(8000)));
    }


    @Test
    void unitStatusViewYearlyFiscal() {
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(
                                UNIT_101, createUnitDetails(0, READY, 1000, 1000),
                                UNIT_102, createUnitDetails(0, NOT_READY, 2000, 1000),
                                "103", createUnitDetails(0, NOT_READY, 2000, 1000)

                        ))
                        .forecastMonthData(List.of(
                                // First Unit
                                createForecastMonth(1, 6, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 7, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 8, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 9, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 10, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 11, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 12, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(2, 1, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(2, 2, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10))

                        )).build()
        );
        // Closing date required for fiscal
        request.setClosingDate(LocalDate.of(2023,6,6));

        var forecastResponse = rentForecastService.forecastRents(request);
        var unitTypeUnitStatusView = forecastResponse.getUnitTypeUnitStatusView().get(UNIT_TYPE_1BR_1BATH);
        var unitTypeUnitStatus1Br1Bath = unitTypeUnitStatusView.getUnitForecasts();
        var readySummary = unitTypeUnitStatus1Br1Bath.get(READY);
        var notReadySummary = unitTypeUnitStatus1Br1Bath.get(NOT_READY);

        assertNotEquals(BigDecimal.ZERO, notReadySummary.get(0).getActualRent());
        assertNotEquals(BigDecimal.ZERO, notReadySummary.get(0).getMarketRent());
        assertNotEquals(BigDecimal.ZERO, readySummary.get(0).getActualRent());
        assertNotEquals(BigDecimal.ZERO, readySummary.get(0).getMarketRent());

        // test year 1
        var year1Ready = readySummary.get(0);
        assertEquals(0, year1Ready.getMarketRent().compareTo(BigDecimal.valueOf(7715.610000000000000)));
        assertEquals(0, year1Ready.getActualRent().compareTo(BigDecimal.valueOf(7000)));
        assertEquals(0, year1Ready.getFiscalMarketRent().compareTo(BigDecimal.valueOf(11435.888100000000000)));
        assertEquals(0, year1Ready.getFiscalActualRent().compareTo(BigDecimal.valueOf(8000)));
        var year1NotReady = notReadySummary.get(0);
        assertEquals(0, year1NotReady.getMarketRent().compareTo(BigDecimal.valueOf(30862.440000000000000)));
        assertEquals(0, year1NotReady.getActualRent().compareTo(BigDecimal.valueOf(14000.000000000000000)));
        assertEquals(0, year1NotReady.getFiscalMarketRent().compareTo(BigDecimal.valueOf(45743.552400000000000)));
        assertEquals(0, year1NotReady.getFiscalActualRent().compareTo(BigDecimal.valueOf(16000)));

        // test year 1
        var year2Ready = readySummary.get(1);
        assertEquals(0, year2Ready.getFiscalMarketRent().compareTo(BigDecimal.ZERO));
        assertEquals(0, year2Ready.getFiscalActualRent().compareTo(BigDecimal.ZERO));
        var year2NotReady = notReadySummary.get(1);
        assertEquals(0, year2NotReady.getFiscalMarketRent().compareTo(BigDecimal.ZERO));
        assertEquals(0, year2NotReady.getFiscalActualRent().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void multipleUnitsMonthlySummaryCorrectAddition() {
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(
                                UNIT_101, createUnitDetails(0, READY, 2000, 1000),
                                UNIT_102, createUnitDetails(0, NOT_READY, 2000, 1000),
                                "103", createUnitDetails(0, NOT_READY, 2000, 1000)

                        ))
                        .forecastMonthData(List.of(
                                // First Unit
                                createForecastMonth(1, 1, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(1, 4, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()
        );

        var forecastResponse = rentForecastService.forecastRents(request);

        var year1Month1Forecast = forecastResponse.getUnitTypeForecastRentMonths().get(UNIT_TYPE_1BR_1BATH).getUnitTypeForecast().get(0);
        assertEquals(0, year1Month1Forecast.getMarketRent().compareTo(BigDecimal.valueOf(6000)));
        assertEquals(0, year1Month1Forecast.getActualRent().compareTo(BigDecimal.valueOf(3000)));
    }

    @Test
    public void testSummaryValuesMatch() {
        var fourBed = "4BR/2.5BA";
        var newConstruction = "New Construction";
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(
                                        UNIT_101, createUnitDetails(6, "REHAB", 6000, 700),
                                        UNIT_102, createUnitDetails(6, "REHAB", 6000, 700),
                                        "103", createUnitDetails(6, newConstruction, 6000, 700)
                                )
                        )
                        .excessRentAdjustmentRate(BigDecimal.valueOf(.15))
                        .forecastMonthData(forecastMonthTestData())
                        .build(),
                UnitTypeForecast.builder()
                        .unitType(fourBed)
                        .unitDetails(Map.of(
                                        UNIT_101, createUnitDetails(6, newConstruction, 56000, 700),
                                        UNIT_102, createUnitDetails(6, newConstruction, 56000, 700)
                                )
                        )
                        .forecastMonthData(forecastMonthTestData())
                        .build()

        );
        var forecastResponse = rentForecastService.forecastRents(request);
        var year1Summary4Bed = forecastResponse.getUnitTypeForecastRentYears().get(fourBed).getUnitTypeForecast().get(0);
        var year1Summary4BedNewConstruction = forecastResponse.getUnitTypeUnitStatusView().get(fourBed).getUnitForecasts().get(newConstruction).get(0);
        var year1Summary4BedMarketRent = forecastResponse.getUnitTypeForecastRentYears().get(fourBed).getUnitTypeForecast().get(0).getMarketRent();
        var year1Summary4BedNewConstructionMarketRent = forecastResponse.getUnitTypeUnitStatusView().get(fourBed).getUnitForecasts().get(newConstruction).get(0).getMarketRent();

        assertEquals(1, year1Summary4Bed.getYear());
        assertEquals(1, year1Summary4BedNewConstruction.getYear());

        assertEquals(0, year1Summary4BedNewConstructionMarketRent.compareTo(BigDecimal.valueOf(1412320)));
        assertEquals(0, year1Summary4BedMarketRent.compareTo(BigDecimal.valueOf(1412320)));
    }

    @Test
    public void testClosingDateRespected() {
        var request = createMarketRentRequest(
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(
                                        UNIT_101, createUnitDetails(6, "REHAB", 6000, 700)
                                )
                        )
                        .excessRentAdjustmentRate(BigDecimal.valueOf(.15))
                        .forecastMonthData(forecastMonthTestData())
                        .build()
        );
        request.setClosingDate(LocalDate.of(2023, 6, 28));
        var forecastResponse = rentForecastService.forecastRents(request);
        var unitsBreakDown = forecastResponse.getUnitTypeForecastRentMonths().get(UNIT_TYPE_1BR_1BATH).getUnitForecasts();
        var unitMonthly = unitsBreakDown.get(UNIT_101);
        assertEquals(BigDecimal.ZERO, unitMonthly.get(0).getMarketRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(0).getActualRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(1).getMarketRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(1).getActualRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(2).getMarketRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(2).getActualRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(3).getMarketRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(3).getActualRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(4).getMarketRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(4).getActualRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(4).getMarketRent());
        assertEquals(BigDecimal.ZERO, unitMonthly.get(4).getActualRent());
        assertEquals(0, unitMonthly.get(6).getMarketRent().compareTo(BigDecimal.valueOf(6000)));
        assertEquals(0, unitMonthly.get(6).getActualRent().compareTo(BigDecimal.valueOf(700)));
    }


    private RentForecastRequest createMarketRentRequest(UnitTypeForecast... unitTypeForecastList) {
        var request = new RentForecastRequest();
        request.setUnitTypeForecastList(List.of(unitTypeForecastList));
        return request;
    }

    private ForecastMonth createForecastMonth(int year, int month, BigDecimal marketEscalationRate, BigDecimal actualEscalationRate) {
        return ForecastMonth.builder().month(month).year(year).marketEscalationRate(marketEscalationRate).actualEscalationRate(actualEscalationRate).build();
    }

    private UnitDetails createUnitDetails(int contractTerm, String unitStatus, int startingMarketRent, int startingActualRent, LocalDate startDate) {
        var unitDetails = createUnitDetails(contractTerm, unitStatus, startingMarketRent, startingActualRent);
        unitDetails.setStartDate(startDate);
        return unitDetails;
    }


    private UnitDetails createUnitDetails(int contractTerm, String unitStatus, int startingMarketRent, int startingActualRent) {
        var unitDetails = new UnitDetails();
        unitDetails.setUnitStatus(unitStatus);
        if (contractTerm != 0) {
            unitDetails.setContractTerm(contractTerm);
        }
        unitDetails.setStartingActualRent(BigDecimal.valueOf(startingActualRent));
        unitDetails.setStartingMarketRent(BigDecimal.valueOf(startingMarketRent));
        return unitDetails;
    }

    private List<ForecastMonth> forecastMonthTestData() {
        return List.of(
                createForecastMonth(1, 1, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 3, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 2, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 4, BigDecimal.valueOf(.05), BigDecimal.valueOf(.30)),
                createForecastMonth(1, 5, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 6, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 7, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 8, BigDecimal.valueOf(.05), BigDecimal.valueOf(.30)),
                createForecastMonth(1, 9, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 10, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 11, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(1, 12, BigDecimal.valueOf(.04), BigDecimal.valueOf(.30)),
                createForecastMonth(2, 1, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 2, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 3, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 4, BigDecimal.valueOf(.04), BigDecimal.valueOf(.04)),
                createForecastMonth(2, 5, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 6, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 7, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 8, BigDecimal.valueOf(.04), BigDecimal.valueOf(.04)),
                createForecastMonth(2, 9, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 10, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 11, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(2, 12, BigDecimal.valueOf(.05), BigDecimal.valueOf(.04)),
                createForecastMonth(3, 1, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 2, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 3, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 4, BigDecimal.valueOf(.05), BigDecimal.valueOf(.03)),
                createForecastMonth(3, 5, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 6, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 7, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 8, BigDecimal.valueOf(.05), BigDecimal.valueOf(.03)),
                createForecastMonth(3, 9, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 10, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 11, BigDecimal.ZERO, BigDecimal.ZERO),
                createForecastMonth(3, 12, BigDecimal.valueOf(.02), BigDecimal.valueOf(.03)));
    }

}