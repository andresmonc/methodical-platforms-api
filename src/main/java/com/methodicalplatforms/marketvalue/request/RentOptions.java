package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RentOptions {
    @Builder.Default
    private Boolean summarizeByYear = false;
    @Builder.Default
    private Boolean summarizeByUnitType = false;
}
