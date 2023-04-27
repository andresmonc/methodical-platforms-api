package com.methodicalplatforms.marketvalue.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EscalationMonth {
    public String unitType;
    public int Month;
    public int year;
    public BigDecimal escalationRate;
}
