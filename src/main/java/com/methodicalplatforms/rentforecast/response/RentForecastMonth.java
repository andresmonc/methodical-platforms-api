package com.methodicalplatforms.rentforecast.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentForecastMonth {
    private int year;
    private int month;
    private BigDecimal marketRent;
    private BigDecimal actualRent;
}


