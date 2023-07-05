package com.methodicalplatforms.rentforecast.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UnitDetails {
    private BigDecimal startingMarketRent;
    private BigDecimal startingActualRent;
    @JsonFormat(pattern = "MM/dd/yyyy")
    private LocalDate startDate;
    private Integer contractTerm;
    private String unitStatus;
}
