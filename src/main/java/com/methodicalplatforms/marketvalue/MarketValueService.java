package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.EscalationMonth;
import com.methodicalplatforms.marketvalue.request.RentRequest;
import com.methodicalplatforms.marketvalue.request.UnitTypeEscalationData;
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

    public MarketRentResponse calculateMarketRent(RentRequest rentRequest) {
        Map<String, List<MarketRentMonth>> unitTypeMarketRentsByMonth = getMonthlyMarketRentsForAllUnitTypes(rentRequest.getUnitTypeEscalationDataList());

        MarketRentResponse.MarketRentResponseBuilder marketResponseBuilder = MarketRentResponse.builder();
        if (rentRequest.getOptions() != null && rentRequest.getOptions().getSummarizeByYear()) {
            Map<String, List<MarketRentYear>> yearSummaryByUnitType = summarizeYearsForUnitTypes(unitTypeMarketRentsByMonth);
            marketResponseBuilder.unitTypeMarketRentYears(yearSummaryByUnitType);
        } else {
            marketResponseBuilder.unitTypeMarketRentMonths(unitTypeMarketRentsByMonth);
        }

        return marketResponseBuilder.build();
    }

    private Map<String, List<MarketRentMonth>> getMonthlyMarketRentsForAllUnitTypes(List<UnitTypeEscalationData> unitTypeEscalationDataList) {
        // maintain order which we received
        Map<String, List<MarketRentMonth>> unitTypeMarketRentsByMonth = new LinkedHashMap<>();

        // Loop through all unit types and calculate the market rent per month
        unitTypeEscalationDataList.forEach(unitTypeEscalationData -> {
            List<MarketRentMonth> marketRentByMonthForUnitType = calculateMonthlyMarketRentsByIndividualUnitType(unitTypeEscalationData);
            unitTypeMarketRentsByMonth.put(unitTypeEscalationData.getUnitType(), marketRentByMonthForUnitType);
        });

        return unitTypeMarketRentsByMonth;
    }

    private List<MarketRentMonth> calculateMonthlyMarketRentsByIndividualUnitType(UnitTypeEscalationData unitTypeEscalationData) {
        // maintain order which we received
        List<MarketRentMonth> marketRentsByMonth = new ArrayList<>();
        // Track the market rent for the unit type
        BigDecimal marketValue = unitTypeEscalationData.getStartingMarketValue();


        for (EscalationMonth escalationMonth : unitTypeEscalationData.getEscalationMonthData()) {
            // Calculate the market rent for the month in question
            MarketRentMonth marketRentMonth = calculateMarketRentMonth(escalationMonth, marketValue);

            // Update the tracker for current market rents for the unit type
            marketRentsByMonth.add(marketRentMonth);
            marketValue = marketRentMonth.getMarketRent();
        }

        return marketRentsByMonth;
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
