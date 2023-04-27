package com.methodicalplatforms.marketvalue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MarketValueServiceTest {

    public MarketValueService marketValueService;

    @BeforeEach
    public void setup() {
        marketValueService = new MarketValueService();
    }

    @Test
    void calculateMarketRentMonthly() {
        var marketValueResponse = marketValueService.calculateMarketRent(null);

        assertNotNull(marketValueResponse);
    }

    @Test
    void calculateMarketRentYearlySummary() {
        var marketValueResponse = marketValueService.calculateMarketRent(null);

        assertNotNull(marketValueResponse);
    }
}