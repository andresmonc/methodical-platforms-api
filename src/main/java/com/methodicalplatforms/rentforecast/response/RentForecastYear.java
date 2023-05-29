package com.methodicalplatforms.rentforecast.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentForecastYear {
    private int year;
    private BigDecimal marketRent;
    private BigDecimal actualRent;
}
