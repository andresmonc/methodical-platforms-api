package com.methodicalplatforms.marketvalue.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MarketRentMonth {
    private int year;
    private int month;
    private BigDecimal marketRent;
}
