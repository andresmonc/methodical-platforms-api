package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.EscalationMonth;
import com.methodicalplatforms.marketvalue.request.MarketRentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MarketValueServiceTest {

    public MarketValueService marketValueService;

    @BeforeEach
    public void setup() {
        marketValueService = new MarketValueService();
    }

    @Test
    void calculateMarketRentMonthlySingleUnitType() {
        var unitType = "1 BR 1 BATH";
        var request = MarketRentRequest.builder()
                .escalationMonths(List.of(
                        EscalationMonth.builder().unitType(unitType).month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                        EscalationMonth.builder().unitType(unitType).month(1).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType).month(2).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType).month(3).escalationRate(BigDecimal.ZERO).build()
                ))
                .yearlySummaryEnabled(false)
                .marketRent(BigDecimal.valueOf(1000))
                .build();


        var marketValueResponse = marketValueService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentMonths().size());

        var marketRentMonths = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType);
        assertEquals(4, marketRentMonths.size());
    }

    @Test
    void calculateMarketRentMonthlyMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = MarketRentRequest.builder()
                .escalationMonths(List.of(
                        // First Unity
                        EscalationMonth.builder().unitType(unitType1).month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                        EscalationMonth.builder().unitType(unitType1).month(1).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType1).month(2).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType1).month(3).escalationRate(BigDecimal.ZERO).build(),
                        // Second Unit
                        EscalationMonth.builder().unitType(unitType2).month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                        EscalationMonth.builder().unitType(unitType2).month(1).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType2).month(2).escalationRate(BigDecimal.ZERO).build()
                ))
                .yearlySummaryEnabled(false)
                .marketRent(BigDecimal.valueOf(1000))
                .build();


        var marketValueResponse = marketValueService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeMarketRentMonths().size());

        var marketRentMonths1 = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType1);
        assertEquals(4, marketRentMonths1.size());

        var marketRentMonths2 = marketValueResponse.getUnitTypeMarketRentMonths().get(unitType2);
        assertEquals(3, marketRentMonths2.size());
    }

    @Test
    void calculateMarketRentYearlySummarySingleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var request = MarketRentRequest.builder()
                .escalationMonths(List.of(
                        // First Unity
                        EscalationMonth.builder().unitType(unitType1).month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                        EscalationMonth.builder().unitType(unitType1).month(1).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType1).month(2).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType1).month(3).escalationRate(BigDecimal.ZERO).build()
                ))
                .yearlySummaryEnabled(true)
                .marketRent(BigDecimal.valueOf(1000))
                .build();

        var marketValueResponse = marketValueService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(1, marketValueResponse.getUnitTypeMarketRentYears().size());
    }

    @Test
    void calculateMarketRentYearlySummaryMultipleUnitType() {
        var unitType1 = "1 BR 1 BATH";
        var unitType2 = "2 BR 2 BATH";
        var request = MarketRentRequest.builder()
                .escalationMonths(List.of(
                        // First Unity
                        EscalationMonth.builder().unitType(unitType1).month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                        EscalationMonth.builder().unitType(unitType1).month(1).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType1).month(2).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType1).month(3).escalationRate(BigDecimal.ZERO).build(),
                        // Second Unit
                        EscalationMonth.builder().unitType(unitType2).month(0).escalationRate(BigDecimal.valueOf(.10)).build(),
                        EscalationMonth.builder().unitType(unitType2).month(1).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType2).month(2).escalationRate(BigDecimal.ZERO).build(),
                        EscalationMonth.builder().unitType(unitType2).month(3).escalationRate(BigDecimal.ZERO).build()
                ))
                .yearlySummaryEnabled(true)
                .marketRent(BigDecimal.valueOf(1000))
                .build();

        var marketValueResponse = marketValueService.calculateMarketRent(request);

        assertNotNull(marketValueResponse);
        assertEquals(2, marketValueResponse.getUnitTypeMarketRentYears().size());
    }
}