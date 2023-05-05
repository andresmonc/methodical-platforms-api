package com.methodicalplatforms.rentforecast.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class UnitTypeForecast {
        private String unitType;
        private List<ForecastMonth> forecastMonthData;
        private BigDecimal startingMarketRent;
        private BigDecimal startingActualRent;
}
