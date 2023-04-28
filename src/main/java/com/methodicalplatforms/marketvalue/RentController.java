package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.RentRequest;
import com.methodicalplatforms.marketvalue.response.RentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/market-values")
public class RentController {

    private final RentValueService rentValueService;

    @Autowired
    public RentController(RentValueService rentValueService) {
        this.rentValueService = rentValueService;
    }

    @GetMapping("/yearly")
    public RentResponse getRentsYearly(@RequestBody RentRequest rentRequest) {
        return rentValueService.calculateMarketRent(rentRequest);
    }


    @GetMapping("/monthly")
    public RentResponse getRentsMonthly(@RequestBody RentRequest rentRequest) {
        return rentValueService.calculateMarketRent(rentRequest);
    }

    @GetMapping("/yearly/summary")
    public RentResponse getYearlyRentSummary(@RequestBody RentRequest rentRequest) {
        return rentValueService.calculateMarketRent(rentRequest);
    }


    @GetMapping("/monthly/summary")
    public RentResponse getMonthlyRentSummary(@RequestBody RentRequest rentRequest) {
        return rentValueService.calculateMarketRent(rentRequest);
    }
}
