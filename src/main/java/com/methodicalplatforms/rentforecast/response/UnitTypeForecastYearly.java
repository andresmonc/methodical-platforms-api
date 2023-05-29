package com.methodicalplatforms.rentforecast.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnitTypeForecastYearly {
    private List<RentForecastYear> unitTypeForecast;
    private Map<String, List<RentForecastYear>> unitMarketRentYears;
}