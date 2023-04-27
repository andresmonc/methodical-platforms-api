package com.methodicalplatforms.marketvalue;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MarketRentYear {
    private int year;
    private BigDecimal marketValue;
}
