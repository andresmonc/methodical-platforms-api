package com.methodicalplatforms.rentforecast;

import com.methodicalplatforms.rentforecast.request.RentForecastRequest;
import com.methodicalplatforms.rentforecast.response.RentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rent-forecast")
public class RentForecastController {

    private final RentForecastService rentForecastService;

    @Autowired
    public RentForecastController(RentForecastService rentForecastService) {
        this.rentForecastService = rentForecastService;
    }

    @PostMapping
    public RentResponse getRentsYearly(@RequestBody RentForecastRequest rentForecastRequest) {
        return rentForecastService.calculateMarketRent(rentForecastRequest);
    }

}
