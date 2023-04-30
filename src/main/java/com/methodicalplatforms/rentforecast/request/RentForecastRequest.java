package com.methodicalplatforms.rentforecast.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RentForecastRequest {
    @Builder.Default
    private RentForecastOptions options = new RentForecastOptions();
    private List<UnitTypeForecast> unitTypeForecastList;
}
