package com.methodicalplatforms.rentforecast.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RentForecastYear {
    private int year;
    private BigDecimal marketRent;
}
