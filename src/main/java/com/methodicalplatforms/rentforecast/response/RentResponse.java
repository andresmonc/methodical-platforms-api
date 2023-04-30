package com.methodicalplatforms.rentforecast.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentResponse {
    private Map<String, List<RentForecastMonth>> unitTypeMarketRentMonths;
    private Map<String, List<RentForecastYear>> unitTypeMarketRentYears;
}
