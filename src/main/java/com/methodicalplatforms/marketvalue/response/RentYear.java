package com.methodicalplatforms.marketvalue.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RentYear {
    private int year;
    private BigDecimal marketValue;
}
