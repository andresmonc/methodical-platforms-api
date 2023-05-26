package com.methodicalplatforms.rentforecast.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnitDetails {
    private BigDecimal startingMarketRent;
    private BigDecimal startingActualRent;
    private Integer contractTerm;
}
