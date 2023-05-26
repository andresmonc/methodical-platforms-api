package com.methodicalplatforms.rentforecast.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class UnitTypeForecastMonthly {
    private List<RentForecastMonth> unitTypeForecast;
    private Map<String, List<RentForecastMonth>> unitForecasts;
}