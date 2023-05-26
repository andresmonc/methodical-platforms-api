package com.methodicalplatforms.rentforecast.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UnitTypeForecastYearly {
    private List<RentForecastYear> unitTypeForecast;
    private Map<String, List<RentForecastYear>> unitMarketRentYears;
}