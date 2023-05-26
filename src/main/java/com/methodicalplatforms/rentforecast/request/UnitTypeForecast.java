package com.methodicalplatforms.rentforecast.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UnitTypeForecast {
        private String unitType;
        private List<ForecastMonth> forecastMonthData;
        public BigDecimal excessRentAdjustmentRate;
        public Map<String, UnitDetails> unitDetails;
}
