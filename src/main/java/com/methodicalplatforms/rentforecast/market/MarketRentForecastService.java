package com.methodicalplatforms.rentforecast.market;

import com.methodicalplatforms.rentforecast.actual.ActualRentForecastService;
import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

@Service
public class MarketRentForecastService {

    public BigDecimal calculateMarketRentForMonth(BigDecimal marketEscalationRate, BigDecimal priorMarketRent,
                                                  BigDecimal forecastedActualRent, BigDecimal excessRateAdjustmentRate) {
        BigDecimal forecastedMarketRent = priorMarketRent.multiply(marketEscalationRate);

        // avoid division by zero;
        if (forecastedActualRent.compareTo(BigDecimal.ZERO) != 0 && excessRateAdjustmentRate != null && excessRateAdjustmentRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal lossToLeasePercent = forecastedActualRent.subtract(forecastedMarketRent).divide(forecastedActualRent, HALF_EVEN);
            // If loss to lease percent exceed set market rent to actual rent, we're now the market leader
            if (lossToLeasePercent.compareTo(excessRateAdjustmentRate) > 0) {
                forecastedMarketRent = BigDecimal.ZERO.add(forecastedActualRent);
            }
        }
        return forecastedMarketRent;
    }
}
