package com.methodicalplatforms.marketvalue;

import com.methodicalplatforms.marketvalue.request.EscalationMonth;
import com.methodicalplatforms.marketvalue.request.RentOptions;
import com.methodicalplatforms.marketvalue.request.RentRequest;
import com.methodicalplatforms.marketvalue.request.UnitTypeEscalationData;
import com.methodicalplatforms.marketvalue.response.RentMonth;
import com.methodicalplatforms.marketvalue.response.RentResponse;
import com.methodicalplatforms.marketvalue.response.RentYear;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RentValueService {

    private static final String ALL_UNITS = "ALL UNITS";

    public RentResponse calculateMarketRent(RentRequest rentRequest) {
        Map<String, List<RentMonth>> rentByMonths = getMonthlyMarketRentsForAllUnitTypes(rentRequest.getUnitTypeEscalationDataList());

        RentResponse.RentResponseBuilder marketResponseBuilder = RentResponse.builder();
        RentOptions options = rentRequest.getOptions();
        if (options != null && rentRequest.getOptions().getSummarizeByYear()) {
            // Summarize by Year
            Map<String, List<RentYear>> rentByYears = summarizeYearsForUnitTypes(rentByMonths);
            if (options.getSummarizeByUnitType()) {
                // Summarize by unit type
                rentByYears = yearlySummaryByUnitType(rentByYears);
            }
            marketResponseBuilder.unitTypeMarketRentYears(rentByYears);
        } else {
            // Monthly summary
            if (options != null && options.getSummarizeByUnitType()) {
                // Summarize by unit type
                rentByMonths = monthlySummaryByUnitType(rentByMonths);
            }
            marketResponseBuilder.unitTypeMarketRentMonths(rentByMonths);
        }

        return marketResponseBuilder.build();
    }

    private Map<String, List<RentMonth>> monthlySummaryByUnitType(Map<String, List<RentMonth>> rentMonthsByUnitType) {
        Map<String, RentMonth> totalRentMonths = new HashMap<>();
        rentMonthsByUnitType.values().stream()
                .flatMap(List::stream)
                .forEach(unitRentMonth -> totalRentMonths.merge(
                        unitRentMonth.getMonth() + "-" + unitRentMonth.getYear(),
                        unitRentMonth,
                        (oldRentMonth, newRentMonth) -> {
                            oldRentMonth.setMarketRent(oldRentMonth.getMarketRent().add(newRentMonth.getMarketRent()));
                            return oldRentMonth;
                        }));

        return Map.of(ALL_UNITS, new ArrayList<>(totalRentMonths.values()));
    }

    private Map<String, List<RentYear>> yearlySummaryByUnitType(Map<String, List<RentYear>> rentYearsByUnitType) {
        Map<Integer, RentYear> totalsForRentYears = new HashMap<>();
        rentYearsByUnitType.values().stream()
                .flatMap(List::stream)
                .forEach(unitRentYear -> totalsForRentYears.merge(
                        unitRentYear.getYear(),
                        unitRentYear,
                        (oldRentYear, newRentYear) -> {
                            oldRentYear.setMarketRent(oldRentYear.getMarketRent().add(newRentYear.getMarketRent()));
                            return oldRentYear;
                        }));

        return Map.of(ALL_UNITS, new ArrayList<>(totalsForRentYears.values()));
    }


    private Map<String, List<RentMonth>> getMonthlyMarketRentsForAllUnitTypes(List<UnitTypeEscalationData> unitTypeEscalationDataList) {
        // Use a stream to iterate through the unitTypeEscalationDataList and create a map with unit type as key and monthly market rents as value
        return unitTypeEscalationDataList.stream()
                // The first argument to Collectors.toMap specifies the key to be used in the map
                .collect(Collectors.toMap(
                        UnitTypeEscalationData::getUnitType, // key is unit type
                        this::calculateMonthlyMarketRentsByIndividualUnitType, // value is monthly market rents
                        // This merge function resolves any key collisions by keeping the last value encountered (i.e. using the new value instead of the old value)
                        (a, b) -> b, HashMap::new));
    }


    private List<RentMonth> calculateMonthlyMarketRentsByIndividualUnitType(UnitTypeEscalationData unitTypeEscalationData) {
        List<RentMonth> marketRentsByMonth = new ArrayList<>();
        // Track the market rent for the unit type
        BigDecimal marketValue = unitTypeEscalationData.getStartingMarketValue();

        // Sort the escalation months
        List<EscalationMonth> sortedEscalationMonths = unitTypeEscalationData.getEscalationMonthData().stream()
                .sorted(Comparator.comparingInt(EscalationMonth::getYear)
                        .thenComparingInt(EscalationMonth::getMonth))
                .toList();

        for (EscalationMonth escalationMonth : sortedEscalationMonths) {
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
        Map<String, List<RentYear>> yearSummaryByUnitType = new HashMap<>();

        // Summarize the year for each unit type
        marketRentMonthsByUnitType.forEach((unitType, marketRentMonths) -> {
            List<RentYear> rentYearSummaryForUnitType = summarizeYearsForUnitType(marketRentMonths);
            yearSummaryByUnitType.put(unitType, rentYearSummaryForUnitType);
        });

        return yearSummaryByUnitType;
    }

    private List<RentYear> summarizeYearsForUnitType(List<RentMonth> rentMonths) {
        // Create a map that groups the RentMonth objects by year and sums up the market rent values for each year
        Map<Integer, BigDecimal> yearMarketValue = rentMonths.stream()
                .collect(Collectors.groupingBy(RentMonth::getYear,
                        Collectors.mapping(RentMonth::getMarketRent,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        // Convert the map to a list of RentYear objects
        return yearMarketValue.entrySet().stream()
                .map(entry -> RentYear.builder().year(entry.getKey()).marketRent(entry.getValue()).build())
                .toList();
    }

}
