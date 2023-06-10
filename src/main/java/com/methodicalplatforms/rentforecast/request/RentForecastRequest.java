package com.methodicalplatforms.rentforecast.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RentForecastRequest {
    private List<UnitTypeForecast> unitTypeForecastList;
    @JsonFormat(pattern = "MM/dd/yyyy")
    private LocalDate closingDate;
}
