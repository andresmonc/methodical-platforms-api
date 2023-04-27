package com.methodicalplatforms.marketvalue.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class MarketRentResponse {
    Map<String, List<MarketRentMonth>> unitTypeMarketRentMonths;
    Map<String, List<MarketRentYear>> unitTypeMarketRentYears;


    List<MarketRentMonth> marketRentMonths;
    List<MarketRentYear> marketRentYears;
}
