package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RentRequest {
    @Builder.Default
    private RentOptions options = new RentOptions();
    private List<UnitTypeEscalationData> unitTypeEscalationDataList;
}
