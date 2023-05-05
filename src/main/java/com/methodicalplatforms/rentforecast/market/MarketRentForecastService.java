package com.methodicalplatforms.rentforecast.market;

import com.methodicalplatforms.rentforecast.request.ForecastMonth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MarketRentForecastService {

    @Autowired
    public MarketRentForecastService() {

    }

    public BigDecimal calculateMarketRentForMonth(ForecastMonth forecastMonth, BigDecimal currentMarketRent) {
        BigDecimal escalationFactor = BigDecimal.ONE.add(forecastMonth.getMarketEscalationRate());
        return currentMarketRent.multiply(escalationFactor);
    }
}
