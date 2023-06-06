package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.market.MarketRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import com.methodicalplatforms.rentforecast.request.RentForecastOptions;
import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.request.UnitDetails;
import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentForecastServiceTest {

    public RentForecastService rentForecastService;
    ActualRentForecastService actualRentForecastService = new ActualRentForecastService();
    private static final String UNIT_TYPE_1BR_1BATH = "1 BR 1 BATH";
    private static final String UNIT_101 = "101";
    private static final String UNIT_102 = "102";

    @BeforeEach
    public void setup() {
        rentForecastService = new RentForecastService(actualRentForecastService, new MarketRentForecastService());
    }

    @Test
    void forecastRentsMonthlySingleUnitType() {
        var request = createMarketRentRequest(
                false,
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(UNIT_101, UnitDetails.builder()
                                .contractTerm(6).startingMarketRent(BigDecimal.valueOf(1000))
                                .startingActualRent(BigDecimal.valueOf(700)).build())
                        )
                        .excessRentAdjustmentRate(BigDecimal.valueOf(.15))
                        .forecastMonthData(
                                List.of(
                                        createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 4, BigDecimal.valueOf(.05), BigDecimal.valueOf(.30)),
                                        createForecastMonth(0, 5, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 6, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 7, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 8, BigDecimal.valueOf(.05), BigDecimal.valueOf(.30)),
                                        createForecastMonth(0, 9, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 10, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 11, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 12, BigDecimal.valueOf(.04), BigDecimal.valueOf(.30)),
                                        createForecastMonth(1, 1, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 2, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 3, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 4, BigDecimal.valueOf(.04), BigDecimal.valueOf(.04)),
                                        createForecastMonth(1, 5, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 6, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 7, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 8, BigDecimal.valueOf(.04), BigDecimal.valueOf(.04)),
                                        createForecastMonth(1, 9, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 10, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 11, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(1, 12, BigDecimal.valueOf(.05), BigDecimal.valueOf(.04)),
                                        createForecastMonth(2, 1, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 2, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 3, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 4, BigDecimal.valueOf(.05), BigDecimal.valueOf(.03)),
                                        createForecastMonth(2, 5, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 6, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 7, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 8, BigDecimal.valueOf(.05), BigDecimal.valueOf(.03)),
                                        createForecastMonth(2, 9, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 10, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 11, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(2, 12, BigDecimal.valueOf(.02), BigDecimal.valueOf(.03))
                                )
                        )
                        .build()
        );

        var marketValueResponse = rentForecastService.forecastRents(request);

        assertNotNull(marketValueResponse);
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentMonths().size());

        var marketRentMonths = marketValueResponse.getUnitTypeMarketRentMonths().get(UNIT_TYPE_1BR_1BATH).getUnitForecasts().get(UNIT_101);
        assertEquals(36, marketRentMonths.size());
        assertEquals(0, BigDecimal.valueOf(1050.00).compareTo(marketRentMonths.get(4).getMarketRent()));
        assertEquals(2, marketRentMonths.get(1).getMonth(), "Response is not sorted");
        assertEquals(0, BigDecimal.valueOf(910.00).compareTo(marketRentMonths.get(7).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1102.50).compareTo(marketRentMonths.get(8).getMarketRent()));
        assertEquals(0, BigDecimal.valueOf(910.00).compareTo(marketRentMonths.get(11).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1537.90).compareTo(marketRentMonths.get(12).getMarketRent()));
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
                false,
                // First Unit
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(UNIT_101, UnitDetails.builder().startingMarketRent(BigDecimal.valueOf(1000)).build()))
                        .forecastMonthData(List.of(
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build(),
                // Second Unit
                UnitTypeForecast.builder()
                        .unitType(unitType2)
                        .unitDetails(Map.of("102", UnitDetails.builder().startingMarketRent(BigDecimal.valueOf(2000)).build()))
                        .forecastMonthData(List.of(
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()

        );

        var marketValueResponse = rentForecastService.forecastRents(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeMarketRentMonths().size());

        var marketRentMonths1 = marketValueResponse.getUnitTypeMarketRentMonths().get(UNIT_TYPE_1BR_1BATH).getUnitForecasts().get(UNIT_101);
        assertEquals(4, marketRentMonths1.size());
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(marketRentMonths1.get(1).getMarketRent()));

        var marketRentMonths2 = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType2).getUnitForecasts().get("102");
        assertEquals(3, marketRentMonths2.size());
        assertEquals(0, BigDecimal.valueOf(2200).compareTo(marketRentMonths2.get(1).getMarketRent()));
    }

    @Test
    void calculateMarketRentYearlySummarySingleUnitType() {
        var unitType = "1 BR 1 BATH";
        var request = createMarketRentRequest(
                true,
                UnitTypeForecast.builder()
                        .unitType(unitType)
                        .unitDetails(Map.of(UNIT_101, UnitDetails.builder().startingMarketRent(BigDecimal.valueOf(1000)).build()))
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
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType).getUnitMarketRentYears().get(UNIT_101);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4300)));
    }

    @Test
    void calculateMarketRentYearlySummaryMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = createMarketRentRequest(
                true,
                UnitTypeForecast.builder()
                        .unitType(unitType1)
                        .unitDetails(Map.of("101", UnitDetails.builder().startingMarketRent(BigDecimal.valueOf(1000)).build()))
                        .forecastMonthData(List.of(
                                // First Unit
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build(),
                UnitTypeForecast.builder()
                        .unitType(unitType2)
                        .unitDetails(Map.of(UNIT_102, UnitDetails.builder().startingMarketRent(BigDecimal.valueOf(2000)).build()))
                        .forecastMonthData(List.of(
                                // Second Unit
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()
        );

        var marketValueResponse = rentForecastService.forecastRents(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeMarketRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType1).getUnitMarketRentYears().get(UNIT_101);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4300)));

        var unitType2MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType2).getUnitMarketRentYears().get(UNIT_102);
        assertEquals(1, unitType2MarketYear.size());
        year0MarketValue = unitType2MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(6400)));
    }

    @Test
    void calculateMarketRentYearlySummaryMultipleUnitType2() {
        var request = createMarketRentRequest(
                true,
                UnitTypeForecast.builder()
                        .unitType(UNIT_TYPE_1BR_1BATH)
                        .unitDetails(Map.of(
                                UNIT_101, UnitDetails.builder().unitStatus("READY").startingActualRent(BigDecimal.valueOf(1000)).startingMarketRent(BigDecimal.valueOf(1000)).build(),
                                UNIT_102, UnitDetails.builder().unitStatus("NOT READY").startingActualRent(BigDecimal.valueOf(1000)).startingMarketRent(BigDecimal.valueOf(2000)).build()
                        ))
                        .forecastMonthData(List.of(
                                // First Unit
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()
        );

        var marketValueResponse = rentForecastService.forecastRents(request);
        var unitTypeUnitStatusView = marketValueResponse.getUnitTypeUnitStatusView().get(UNIT_TYPE_1BR_1BATH);
        var unitTypeUnitStatus1Br1Bath = unitTypeUnitStatusView.getUnitMarketRentYears();
        var readySummary = unitTypeUnitStatus1Br1Bath.get("READY");
        var notReadySummary = unitTypeUnitStatus1Br1Bath.get("READY");

        System.out.println(readySummary);
    }


    private RentForecastRequest createMarketRentRequest(boolean yearlySummaryEnabled, UnitTypeForecast... unitTypeForecastList) {
        return RentForecastRequest.builder()
                .options(RentForecastOptions.builder()
                        .summarizeByYear(yearlySummaryEnabled)
                        .build())
                .unitTypeForecastList(List.of(unitTypeForecastList))
                .build();
    }

    private ForecastMonth createForecastMonth(int year, int month, BigDecimal escalationRate, BigDecimal actualEscalationRate) {
        return ForecastMonth.builder().month(month).year(year).marketEscalationRate(escalationRate).actualEscalationRate(actualEscalationRate).build();
    }

}