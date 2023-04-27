package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class UnitTypeEscalationData {
        private String unitType;
        private List<EscalationMonth> escalationMonthData;
        private BigDecimal startingMarketValue;
}
