package com.methodicalplatforms.rentforecast.actual;

import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ActualRentForecastService {

    @Autowired
    public ActualRentForecastService() {

    }

    public BigDecimal calculateActualRentForMonth(ForecastMonth forecastMonth, BigDecimal currentActualRent) {
        BigDecimal escalationFactor = Optional.ofNullable(forecastMonth.getActualEscalationRate())
                .orElse(BigDecimal.ZERO)
                .add(BigDecimal.ONE);
        return Optional
                .ofNullable(currentActualRent).
                orElse(BigDecimal.ZERO)
                .multiply(escalationFactor);
    }
}
