package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EscalationMonth {
    public String unitType;
    public int month;
    public int year;
    public BigDecimal escalationRate;
}
