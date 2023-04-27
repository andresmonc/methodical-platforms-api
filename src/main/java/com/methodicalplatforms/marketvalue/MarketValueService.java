package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.EscalationMonth;
import com.methodicalplatforms.marketvalue.request.MarketRentRequest;
import com.methodicalplatforms.marketvalue.response.MarketRentMonth;
import com.methodicalplatforms.marketvalue.response.MarketRentResponse;
import com.methodicalplatforms.marketvalue.response.MarketRentYear;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketValueService {

    public MarketRentResponse calculateMarketRent(MarketRentRequest marketRentRequest) {
        Map<String, List<MarketRentMonth>> unitTypeMarketRentsByMonth = getMonthlyMarketRentsByUnitType(marketRentRequest.getMarketRent(), marketRentRequest.getEscalationMonths());

        MarketRentResponse.MarketRentResponseBuilder marketResponseBuilder = MarketRentResponse.builder();
        if (marketRentRequest.isYearlySummaryEnabled()) {
            Map<String, List<MarketRentYear>> yearSummaryByUnitType = summarizeYearsForUnitTypes(unitTypeMarketRentsByMonth);
            marketResponseBuilder.unitTypeMarketRentYears(yearSummaryByUnitType);
        } else {
            marketResponseBuilder.unitTypeMarketRentMonths(unitTypeMarketRentsByMonth);
        }

        return marketResponseBuilder.build();
    }

    private Map<String, List<MarketRentMonth>> getMonthlyMarketRentsByUnitType(BigDecimal startingMarketRent, List<EscalationMonth> escalationMonths) {
        // maintain order which we received
        Map<String, List<MarketRentMonth>> unitTypeMarketRentsByMonth = new LinkedHashMap<>();

        BigDecimal currentMarketRent = startingMarketRent;

        for (EscalationMonth escalationMonth : escalationMonths) {
            String unitType = escalationMonth.getUnitType();
            // If unit type not in map initialize it
            if (!unitTypeMarketRentsByMonth.containsKey(escalationMonth.getUnitType())) {
                unitTypeMarketRentsByMonth.put(unitType, new ArrayList<>());
            }

            // Calculate the market rent for the month in question
            MarketRentMonth marketRentMonth = calculateMarketRentMonth(escalationMonth, currentMarketRent);
            // Fetch the list of market rents for the specific unit type and append the new calc
            unitTypeMarketRentsByMonth.get(unitType).add(marketRentMonth);
            currentMarketRent = marketRentMonth.getMarketRent();
        }

        return unitTypeMarketRentsByMonth;
    }

    private MarketRentMonth calculateMarketRentMonth(EscalationMonth escalationMonth, BigDecimal currentMarketRent) {
        BigDecimal escalationFactor = BigDecimal.ONE.add(escalationMonth.getEscalationRate());
        BigDecimal marketRent = currentMarketRent.multiply(escalationFactor);

        return MarketRentMonth.builder()
                .month(escalationMonth.getMonth())
                .year(escalationMonth.getYear())
                .marketRent(marketRent)
                .build();
    }

    private Map<String, List<MarketRentYear>> summarizeYearsForUnitTypes(Map<String, List<MarketRentMonth>> marketRentMonthsByUnitType) {
        // maintaining order which we received
        Map<String, List<MarketRentYear>> yearSummaryByUnitType = new LinkedHashMap<>();

        // Summarize the year for each unit type
        marketRentMonthsByUnitType.forEach((unitType, marketRentMonths) -> {
            List<MarketRentYear> marketRentYearSummaryForUnitType = summarizeYearsForUnitType(marketRentMonths);
            yearSummaryByUnitType.put(unitType, marketRentYearSummaryForUnitType);
        });

        return yearSummaryByUnitType;
    }

    private List<MarketRentYear> summarizeYearsForUnitType(List<MarketRentMonth> marketRentMonths) {
        // maintaining order which we received
        LinkedHashMap<Integer, MarketRentYear> yearMarketValue = new LinkedHashMap<>();

        for (MarketRentMonth marketRentMonth : marketRentMonths) {
            int year = marketRentMonth.getYear();
            BigDecimal marketRentForMonth = marketRentMonth.getMarketRent();

            // If year doesn't exist in linked map then initialize it
            if (!yearMarketValue.containsKey(marketRentMonth.getYear())) {
                yearMarketValue.put(year, MarketRentYear.builder().year(year).marketValue(BigDecimal.ZERO).build());
            }

            // Update market value for year
            BigDecimal newMarketRentValue = yearMarketValue.get(year).getMarketValue().add(marketRentForMonth);
            yearMarketValue.get(year).setMarketValue(newMarketRentValue);
        }
        return new ArrayList<>(yearMarketValue.values());
    }
}
