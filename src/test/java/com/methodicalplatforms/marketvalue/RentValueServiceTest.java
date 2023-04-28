package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.EscalationMonth;
import com.methodicalplatforms.marketvalue.request.RentOptions;
import com.methodicalplatforms.marketvalue.request.RentRequest;
import com.methodicalplatforms.marketvalue.request.UnitTypeEscalationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentValueServiceTest {

    public RentValueService rentValueService;

    @BeforeEach
    public void setup() {
        rentValueService = new RentValueService();
    }

    @Test
    void calculateMarketRentMonthlySingleUnitType() {
        var unitType = "1 BR 1 BATH";
        var request = createMarketRentRequest(
                false,
                UnitTypeEscalationData.builder()
                        .unitType(unitType)
                        .startingMarketValue(BigDecimal.valueOf(1000))
                        .escalationMonthData(
                                List.of(
                                        EscalationMonth.builder().month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                                        EscalationMonth.builder().month(1).escalationRate(BigDecimal.ZERO).build(),
                                        EscalationMonth.builder().month(2).escalationRate(BigDecimal.ZERO).build(),
                                        EscalationMonth.builder().month(3).escalationRate(BigDecimal.ZERO).build()
                                )
                        )
                        .build()
        );

        var marketValueResponse = rentValueService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentMonths().size());

        var marketRentMonths = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType);
        assertEquals(4, marketRentMonths.size());
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(marketRentMonths.get(0).getMarketRent()));
    }

    @Test
    void calculateMarketRentMonthlyMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = createMarketRentRequest(
                false,
                // First Unit
                UnitTypeEscalationData.builder()
                        .unitType(unitType1)
                        .startingMarketValue(BigDecimal.valueOf(1000))
                        .escalationMonthData(List.of(
                                EscalationMonth.builder().month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                                EscalationMonth.builder().month(1).escalationRate(BigDecimal.ZERO).build(),
                                EscalationMonth.builder().month(2).escalationRate(BigDecimal.ZERO).build(),
                                EscalationMonth.builder().month(3).escalationRate(BigDecimal.ZERO).build()
                        )).build(),
                // Second Unit
                UnitTypeEscalationData.builder()
                        .unitType(unitType2)
                        .startingMarketValue(BigDecimal.valueOf(2000))
                        .escalationMonthData(List.of(
                                EscalationMonth.builder().month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                                EscalationMonth.builder().month(1).escalationRate(BigDecimal.ZERO).build(),
                                EscalationMonth.builder().month(2).escalationRate(BigDecimal.ZERO).build())).build()

        );

        var marketValueResponse = rentValueService.calculateMarketRent(request);

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
                UnitTypeEscalationData.builder()
                        .unitType(unitType)
                        .startingMarketValue(BigDecimal.valueOf(1000))
                        .escalationMonthData(
                                List.of(
                                        EscalationMonth.builder().month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                                        EscalationMonth.builder().month(1).escalationRate(BigDecimal.ZERO).build(),
                                        EscalationMonth.builder().month(2).escalationRate(BigDecimal.ZERO).build(),
                                        EscalationMonth.builder().month(3).escalationRate(BigDecimal.ZERO).build()
                                )
                        )
                        .build()

        );

        var marketValueResponse = rentValueService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketValue();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4400)));
    }

    @Test
    void calculateMarketRentYearlySummaryMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = createMarketRentRequest(
                true,
                UnitTypeEscalationData.builder()
                        .unitType(unitType1)
                        .startingMarketValue(BigDecimal.valueOf(1000))
                        .escalationMonthData(List.of(
                                // First Unit
                                EscalationMonth.builder().month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                                EscalationMonth.builder().month(1).escalationRate(BigDecimal.ZERO).build(),
                                EscalationMonth.builder().month(2).escalationRate(BigDecimal.ZERO).build(),
                                EscalationMonth.builder().month(3).escalationRate(BigDecimal.ZERO).build()
                        )).build(),
                UnitTypeEscalationData.builder()
                        .unitType(unitType2)
                        .startingMarketValue(BigDecimal.valueOf(2000))
                        .escalationMonthData(List.of(
                                // Second Unit
                                EscalationMonth.builder().month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                                EscalationMonth.builder().month(1).escalationRate(BigDecimal.ZERO).build(),
                                EscalationMonth.builder().month(2).escalationRate(BigDecimal.ZERO).build())).build()

        );

        var marketValueResponse = rentValueService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeMarketRentYears().size());

        var unitType1MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType1);
        assertEquals(1, unitType1MarketYear.size());
        var year0MarketValue = unitType1MarketYear.get(0).getMarketValue();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(4400)));

        var unitType2MarketYear = marketValueResponse.getUnitTypeMarketRentYears().get(unitType2);
        assertEquals(1, unitType2MarketYear.size());
        year0MarketValue = unitType2MarketYear.get(0).getMarketValue();
        assertEquals(0, year0MarketValue.compareTo(BigDecimal.valueOf(6600)));
    }


    private RentRequest createMarketRentRequest(boolean yearlySummaryEnabled, UnitTypeEscalationData... unitTypeEscalationDataList) {
        return RentRequest.builder()
                .options(RentOptions.builder()
                        .summarizeByYear(yearlySummaryEnabled)
                        .build())
                .unitTypeEscalationDataList(List.of(unitTypeEscalationDataList))
                .build();
    }

}