package com.methodicalplatforms.rentforecast.actual;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ActualRentForecastService {

    @Autowired
    public ActualRentForecastService() {

    }

    public BigDecimal calculateActualRentForMonth(BigDecimal startingActualRent, BigDecimal currentActualRent, BigDecimal actualEscalationRate) {
        // If we're not escalating just return the previous actual rent
        if (actualEscalationRate.compareTo(BigDecimal.ONE) == 0) {
            return currentActualRent;
        }

        return Optional
                .ofNullable(startingActualRent).
                orElse(BigDecimal.ZERO)
                .multiply(actualEscalationRate);
    }
}
