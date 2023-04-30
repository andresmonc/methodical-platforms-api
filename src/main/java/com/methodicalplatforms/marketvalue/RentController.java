package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.RentRequest;
import com.methodicalplatforms.marketvalue.response.RentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rent-forecast")
public class RentController {

    private final RentValueService rentValueService;

    @Autowired
    public RentController(RentValueService rentValueService) {
        this.rentValueService = rentValueService;
    }

    @PostMapping
    public RentResponse getRentsYearly(@RequestBody RentRequest rentRequest) {
        return rentValueService.calculateMarketRent(rentRequest);
    }

}
