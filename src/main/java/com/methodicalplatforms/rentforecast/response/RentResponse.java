package com.methodicalplatforms.rentforecast.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentResponse {
    private Map<String, UnitTypeForecastMonthly> unitTypeForecastRentMonths;
    // These should be under a separate endpoint but since input comes from front end it doesn't make sense to recalculate everything multiple times
    private Map<String, UnitTypeForecastYearly> unitTypeForecastRentYears;
    private Map<String, UnitTypeForecastYearly> unitTypeUnitStatusView;
}
