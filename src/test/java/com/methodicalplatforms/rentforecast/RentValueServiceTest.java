package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.market.MarketRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import com.methodicalplatforms.rentforecast.request.RentForecastOptions;
import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentValueServiceTest {

    public RentForecastService rentForecastService;
    ActualRentForecastService actualRentForecastService = new ActualRentForecastService();

    @BeforeEach
    public void setup() {
        rentForecastService = new RentForecastService(actualRentForecastService, new MarketRentForecastService(actualRentForecastService));
    }

    @Test
    void forecastRentsMonthlySingleUnitType() {
        var unitType = "1 BR 1 BATH";
        var request = createMarketRentRequest(
                false,
                UnitTypeForecast.builder()
                        .unitType(unitType)
                        .startingMarketRent(BigDecimal.valueOf(1000))
                        .startingActualRent(BigDecimal.valueOf(700))
                        .excessRentAdjustmentRate(BigDecimal.valueOf(.15))
                        .forecastMonthData(
                                List.of(
                                        createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.ZERO),
                                        createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.30)),
                                        createForecastMonth(0, 4, BigDecimal.valueOf(.10), BigDecimal.ZERO),
                                        createForecastMonth(0, 5, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 6, BigDecimal.ZERO, BigDecimal.ZERO),
                                        createForecastMonth(0, 7, BigDecimal.ZERO, BigDecimal.valueOf(.69))
                                )
                        )
                        .build()
        );

        var marketValueResponse = rentForecastService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentMonths().size());

        var marketRentMonths = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType);
        assertEquals(8, marketRentMonths.size());
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(marketRentMonths.get(0).getMarketRent()));
        assertEquals(1, marketRentMonths.get(1).getMonth(), "Response is not sorted");
        assertEquals(0, BigDecimal.valueOf(910).compareTo(marketRentMonths.get(3).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1537.90).compareTo(marketRentMonths.get(7).getActualRent()));
        assertEquals(0, BigDecimal.valueOf(1537.90).compareTo(marketRentMonths.get(7).getMarketRent()));

    }

    @Test
    void calculateMarketRentMonthlyMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = createMarketRentRequest(
                false,
                // First Unit
                UnitTypeForecast.builder()
                        .unitType(unitType1)
                        .startingMarketRent(BigDecimal.valueOf(1000))
                        .forecastMonthData(List.of(
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build(),
                // Second Unit
                UnitTypeForecast.builder()
                        .unitType(unitType2)
                        .startingMarketRent(BigDecimal.valueOf(2000))
                        .forecastMonthData(List.of(
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()

        );

        var marketValueResponse = rentForecastService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeMarketRentMonths().size());

        var marketRentMonths1 = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType1);
        assertEquals(4, marketRentMonths1.size());
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(marketRentMonths1.get(0).getMarketRent()));

        var marketRentMonths2 = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType2);
        assertEquals(3, marketRentMonths2.size());
        assertEquals(0, BigDecimal.valueOf(2200).compareTo(marketRentMonths2.get(0).getMarketRent()));
    }

    @Test
    void calculateMarketRentYearlySummarySingleUnitType() {
        var unitType = "1 BR 1 BATH";
        var request = createMarketRentRequest(
                true,
                UnitTypeForecast.builder()
                        .unitType(unitType)
                        .startingMarketRent(BigDecimal.valueOf(1000))
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

        var marketValueResponse = rentForecastService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4400)));
    }

    @Test
    void calculateMarketRentYearlySummaryMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = createMarketRentRequest(
                true,
                UnitTypeForecast.builder()
                        .unitType(unitType1)
                        .startingMarketRent(BigDecimal.valueOf(1000))
                        .forecastMonthData(List.of(
                                // First Unit
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 3, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build(),
                UnitTypeForecast.builder()
                        .unitType(unitType2)
                        .startingMarketRent(BigDecimal.valueOf(2000))
                        .forecastMonthData(List.of(
                                // Second Unit
                                createForecastMonth(0, 0, BigDecimal.valueOf(.10), BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 1, BigDecimal.ZERO, BigDecimal.valueOf(.10)),
                                createForecastMonth(0, 2, BigDecimal.ZERO, BigDecimal.valueOf(.10))
                        )).build()
        );

        var marketValueResponse = rentForecastService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeMarketRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType1);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4400)));

        var unitType2MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType2);
        assertEquals(1, unitType2MarketYear.size());
        year0MarketValue = unitType2MarketYear.get(0).getMarketRent();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(6600)));
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