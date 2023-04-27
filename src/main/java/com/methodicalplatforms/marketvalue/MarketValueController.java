package com.methodicalplatforms.marketvalue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public MarketRentResponse calculateMarketRent() {
        return marketValueService.calculateMarketRent();
    }
}
