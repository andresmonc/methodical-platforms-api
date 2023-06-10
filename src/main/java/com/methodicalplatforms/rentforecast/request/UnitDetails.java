package com.methodicalplatforms.rentforecast.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class UnitDetails {
    private BigDecimal startingMarketRent;
    private BigDecimal startingActualRent;
    private LocalDate startDate;
    private Integer contractTerm;
    private String unitStatus;
}
