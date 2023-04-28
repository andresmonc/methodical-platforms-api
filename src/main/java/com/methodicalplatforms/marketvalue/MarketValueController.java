package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.RentRequest;
import com.methodicalplatforms.marketvalue.response.MarketRentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/market-values")
public class MarketValueController {

    private final MarketValueService marketValueService;

    @Autowired
    public MarketValueController(MarketValueService marketValueService) {
        this.marketValueService = marketValueService;
    }

    @GetMapping("/yearly")
    public MarketRentResponse getRentsYearly(@RequestBody RentRequest rentRequest) {
        return marketValueService.calculateMarketRent(rentRequest);
    }


    @GetMapping("/monthly")
    public MarketRentResponse getRentsMonthly(@RequestBody RentRequest rentRequest) {
        return marketValueService.calculateMarketRent(rentRequest);
    }

    @GetMapping("/yearly/summary")
    public MarketRentResponse getYearlyRentSummary(@RequestBody RentRequest rentRequest) {
        return marketValueService.calculateMarketRent(rentRequest);
    }


    @GetMapping("/monthly/summary")
    public MarketRentResponse getMonthlyRentSummary(@RequestBody RentRequest rentRequest) {
        return marketValueService.calculateMarketRent(rentRequest);
    }
}
