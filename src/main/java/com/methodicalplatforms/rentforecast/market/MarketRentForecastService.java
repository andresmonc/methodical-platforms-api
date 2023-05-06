package com.methodicalplatforms.rentforecast.market;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

@Service
public class MarketRentForecastService {

    private final ActualRentForecastService actualRentForecastService;

    @Autowired
    public MarketRentForecastService(ActualRentForecastService actualRentForecastService) {
        this.actualRentForecastService = actualRentForecastService;
    }

    public BigDecimal calculateMarketRentForMonth(ForecastMonth forecastMonth, BigDecimal priorMarketRent,
                                                  BigDecimal forecastedActualRent) {
        BigDecimal escalationFactor = BigDecimal.ONE.add(forecastMonth.getMarketEscalationRate());
        BigDecimal forecastedMarketRent = priorMarketRent.multiply(escalationFactor);

        BigDecimal lossToLeasePercent = forecastedActualRent.subtract(forecastedMarketRent).divide(forecastedActualRent, HALF_EVEN);

        // If loss to lease percent exceed set market rent to actual rent, we're now the market leader
        if (lossToLeasePercent.compareTo(forecastMonth.getExcessRentAdjustmentRate()) > 0) { // todo: need to figure out compare to
            forecastedMarketRent = BigDecimal.ZERO.add(forecastedActualRent);
        }

        return priorMarketRent.multiply(escalationFactor);
    }
}
