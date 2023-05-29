package com.methodicalplatforms.rentforecast.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UnitDetails {
    private BigDecimal startingMarketRent;
    private BigDecimal startingActualRent;
    private Integer contractTerm;
}
