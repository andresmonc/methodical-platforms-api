package com.methodicalplatforms.marketvalue;

import lombok.Data;

import java.util.List;

@Data
public class MarketRentResponse {
    List<MarketRentMonth> marketRentMonths;
    List<MarketRentYear> marketRentYears;
}
