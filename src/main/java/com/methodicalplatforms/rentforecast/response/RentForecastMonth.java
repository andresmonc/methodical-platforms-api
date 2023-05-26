package com.methodicalplatforms.rentforecast.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class RentForecastMonth {
    private int year;
    private int month;
    private BigDecimal marketRent;
    private BigDecimal actualRent;
}


