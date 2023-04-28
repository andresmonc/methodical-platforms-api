package com.methodicalplatforms.marketvalue.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RentOptions {
    private Boolean summarizeByYear = false;
    private Boolean summarizeByUnitType = false;
}
