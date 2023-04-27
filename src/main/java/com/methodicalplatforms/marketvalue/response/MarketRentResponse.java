package com.methodicalplatforms.marketvalue.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class MarketRentResponse {
    private Map<String, List<MarketRentMonth>> unitTypeMarketRentMonths;
    private Map<String, List<MarketRentYear>> unitTypeMarketRentYears;
}
