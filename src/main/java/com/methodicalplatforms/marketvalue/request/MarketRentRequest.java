package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MarketRentRequest {
    private boolean yearlySummaryEnabled;
    private BigDecimal marketRent;
    private List<String> something;
    private List<EscalationMonth> escalationMonths;
}
