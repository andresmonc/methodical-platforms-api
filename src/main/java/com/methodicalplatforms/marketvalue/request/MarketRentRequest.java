package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MarketRentRequest {
    private boolean yearlySummaryEnabled;
    private List<UnitTypeEscalationData> unitTypeEscalationDataList;

}
