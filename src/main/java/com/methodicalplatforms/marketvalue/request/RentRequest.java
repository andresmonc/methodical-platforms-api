package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RentRequest {
    private RentOptions options;
    private List<UnitTypeEscalationData> unitTypeEscalationDataList;
}
