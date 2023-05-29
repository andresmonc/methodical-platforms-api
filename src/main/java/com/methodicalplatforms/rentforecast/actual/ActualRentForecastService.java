package com.methodicalplatforms.rentforecast.actual;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class ActualRentForecastService {

    public BigDecimal calculateActualRentForMonth(BigDecimal startingActualRent, BigDecimal currentActualRent, BigDecimal actualEscalationRate) {
        // If we're not escalating just return the previous actual rent
        if (actualEscalationRate.compareTo(BigDecimal.ONE) == 0) {
            return currentActualRent;
        }
        return Objects.requireNonNullElse(startingActualRent, BigDecimal.ZERO).multiply(actualEscalationRate);
    }
}
