package com.methodicalplatforms.rentforecast.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ForecastMonth {
    public int month;
    public int year;
    public BigDecimal escalationRate;
}
