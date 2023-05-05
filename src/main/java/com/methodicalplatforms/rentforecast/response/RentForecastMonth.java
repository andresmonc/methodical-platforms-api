package com.methodicalplatforms.rentforecast.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RentForecastMonth {
    private int year;
    private int month;
    private BigDecimal marketRent;
    private BigDecimal actualRent;
}


