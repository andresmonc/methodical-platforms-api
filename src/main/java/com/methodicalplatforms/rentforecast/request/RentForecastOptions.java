package com.methodicalplatforms.rentforecast.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RentForecastOptions {
    @Builder.Default
    private Boolean summarizeByYear = false;
    @Builder.Default
    private Boolean summarizeByUnitType = false;
}
