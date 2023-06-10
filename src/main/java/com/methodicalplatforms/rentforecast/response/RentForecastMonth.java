package com.methodicalplatforms.rentforecast.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentForecastMonth {
    private int year;
    private int month;
    private BigDecimal marketRent;
    private BigDecimal actualRent;
}


