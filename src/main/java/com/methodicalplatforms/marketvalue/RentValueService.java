package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.EscalationMonth;
import com.methodicalplatforms.marketvalue.request.RentRequest;
import com.methodicalplatforms.marketvalue.request.UnitTypeEscalationData;
import com.methodicalplatforms.marketvalue.response.RentMonth;
import com.methodicalplatforms.marketvalue.response.RentResponse;
import com.methodicalplatforms.marketvalue.response.RentYear;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RentValueService {

    public RentResponse calculateMarketRent(RentRequest rentRequest) {
        Map<String, List<RentMonth>> unitTypeMarketRentsByMonth = getMonthlyMarketRentsForAllUnitTypes(rentRequest.getUnitTypeEscalationDataList());

        RentResponse.RentResponseBuilder marketResponseBuilder = RentResponse.builder();
        if (rentRequest.getOptions() != null && rentRequest.getOptions().getSummarizeByYear()) {
            Map<String, List<RentYear>> yearSummaryByUnitType = summarizeYearsForUnitTypes(unitTypeMarketRentsByMonth);
            marketResponseBuilder.unitTypeMarketRentYears(yearSummaryByUnitType);
        } else {
            marketResponseBuilder.unitTypeMarketRentMonths(unitTypeMarketRentsByMonth);
        }

        return marketResponseBuilder.build();
    }

    private Map<String, List<RentMonth>> getMonthlyMarketRentsForAllUnitTypes(List<UnitTypeEscalationData> unitTypeEscalationDataList) {
        // maintain order which we received
        Map<String, List<RentMonth>> unitTypeMarketRentsByMonth = new LinkedHashMap<>();

        // Loop through all unit types and calculate the market rent per month
        unitTypeEscalationDataList.forEach(unitTypeEscalationData -> {
            List<RentMonth> marketRentByMonthForUnitType = calculateMonthlyMarketRentsByIndividualUnitType(unitTypeEscalationData);
            unitTypeMarketRentsByMonth.put(unitTypeEscalationData.getUnitType(), marketRentByMonthForUnitType);
        });

        return unitTypeMarketRentsByMonth;
    }

    private List<RentMonth> calculateMonthlyMarketRentsByIndividualUnitType(UnitTypeEscalationData unitTypeEscalationData) {
        // maintain order which we received
        List<RentMonth> marketRentsByMonth = new ArrayList<>();
        // Track the market rent for the unit type
        BigDecimal marketValue = unitTypeEscalationData.getStartingMarketValue();


        for (EscalationMonth escalationMonth : unitTypeEscalationData.getEscalationMonthData()) {
            // Calculate the market rent for the month in question
            RentMonth rentMonth = calculateMarketRentMonth(escalationMonth, marketValue);

            // Update the tracker for current market rents for the unit type
            marketRentsByMonth.add(rentMonth);
            marketValue = rentMonth.getMarketRent();
        }

        return marketRentsByMonth;
    }

    private RentMonth calculateMarketRentMonth(EscalationMonth escalationMonth, BigDecimal currentMarketRent) {
        BigDecimal escalationFactor = BigDecimal.ONE.add(escalationMonth.getEscalationRate());
        BigDecimal marketRent = currentMarketRent.multiply(escalationFactor);

        return RentMonth.builder()
                .month(escalationMonth.getMonth())
                .year(escalationMonth.getYear())
                .marketRent(marketRent)
                .build();
    }

    private Map<String, List<RentYear>> summarizeYearsForUnitTypes(Map<String, List<RentMonth>> marketRentMonthsByUnitType) {
        // maintaining order which we received
        Map<String, List<RentYear>> yearSummaryByUnitType = new LinkedHashMap<>();

        // Summarize the year for each unit type
        marketRentMonthsByUnitType.forEach((unitType, marketRentMonths) -> {
            List<RentYear> rentYearSummaryForUnitType = summarizeYearsForUnitType(marketRentMonths);
            yearSummaryByUnitType.put(unitType, rentYearSummaryForUnitType);
        });

        return yearSummaryByUnitType;
    }

    private List<RentYear> summarizeYearsForUnitType(List<RentMonth> rentMonths) {
        // maintaining order which we received
        LinkedHashMap<Integer, RentYear> yearMarketValue = new LinkedHashMap<>();

        for (RentMonth rentMonth : rentMonths) {
            int year = rentMonth.getYear();
            BigDecimal marketRentForMonth = rentMonth.getMarketRent();

            // If year doesn't exist in linked map then initialize it
            if (!yearMarketValue.containsKey(rentMonth.getYear())) {
                yearMarketValue.put(year, RentYear.builder().year(year).marketValue(BigDecimal.ZERO).build());
            }

            // Update market value for year
            BigDecimal newMarketRentValue = yearMarketValue.get(year).getMarketValue().add(marketRentForMonth);
            yearMarketValue.get(year).setMarketValue(newMarketRentValue);
        }
        return new ArrayList<>(yearMarketValue.values());
    }
}
