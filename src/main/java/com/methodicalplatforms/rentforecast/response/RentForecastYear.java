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
    @Builder.Default
    private BigDecimal marketRent = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal actualRent = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fiscalMarketRent = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fiscalActualRent = BigDecimal.ZERO;
}
