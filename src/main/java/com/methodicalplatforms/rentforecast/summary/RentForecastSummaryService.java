package com.methodicalplatforms.rentforecast.summary;

import com.methodicalplatforms.rentforecast.request.UnitTypeForecast;
import com.methodicalplatforms.rentforecast.response.RentForecastMonth;
import com.methodicalplatforms.rentforecast.response.RentForecastYear;
import com.methodicalplatforms.rentforecast.response.UnitTypeForecastMonthly;
import com.methodicalplatforms.rentforecast.response.UnitTypeForecastYearly;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RentForecastSummaryService {

    private static final String ALL_UNITS = "ALL UNITS";

    // TODO: ALL YEARLY VALUES NEED A FISCAL VALUE!!!!


    /**
     * Sum up individual units for a given Unit Type to provide a summary
     *
     * @param unitForecastData - the forecast details for a unit type
     * @return - the summed up monthly values for a unit type
     */
    public List<RentForecastMonth> summarizeUnitType(Map<String, List<RentForecastMonth>> unitForecastData) {
        List<RentForecastMonth> unitTypeSummary = new ArrayList<>();

        unitForecastData.forEach((unitName, rentForecastMonths) -> {
            for (int i = 0; i < rentForecastMonths.size(); i++) {
                RentForecastMonth rentForecastMonth = rentForecastMonths.get(i);
                if (unitTypeSummary.size() <= i) {
                    unitTypeSummary.add(RentForecastMonth.builder().month(rentForecastMonth.getMonth()).year(rentForecastMonth.getYear()).build());
                }
                RentForecastMonth unitTypeRentforecastMonth = unitTypeSummary.get(i);
                unitTypeRentforecastMonth.setActualRent(unitTypeRentforecastMonth.getActualRent().add(rentForecastMonth.getActualRent()));
                unitTypeRentforecastMonth.setMarketRent(unitTypeRentforecastMonth.getMarketRent().add(rentForecastMonth.getMarketRent()));
            }

        });
        return unitTypeSummary;
    }

    /**
     * Provide a summary for each unit type at a yearly level
     *
     * @param forecastedRentMonthsByUnitType - the monthly forecast data for each unit type
     * @return - summary for each unit type, yearly
     */
    public Map<String, UnitTypeForecastYearly> summarizeYearsForAllUnitTypes(Map<String, UnitTypeForecastMonthly> forecastedRentMonthsByUnitType) {
        Map<String, UnitTypeForecastYearly> unitTypeForecastYearlyMap = new HashMap<>();
        // Summarize the year for each unit type
        forecastedRentMonthsByUnitType.forEach((unitType, unitTypeForecastMonthly) -> {
            UnitTypeForecastYearly unitTypeForecastYearly = new UnitTypeForecastYearly();
            Map<String, List<RentForecastYear>> unitForecastsYearly = new HashMap<>();
            // Summarize the years for each unit
            unitTypeForecastMonthly.getUnitForecasts().forEach((unitId, rentForecastMonths) -> {
                List<RentForecastYear> rentYearSummaryForUnitType = summarizeYearsForAUnitType(rentForecastMonths);
                unitForecastsYearly.put(unitId, rentYearSummaryForUnitType);
            });
            List<RentForecastYear> rentYearSummaryForUnitType = summarizeYearsForAUnitType(unitTypeForecastMonthly.getUnitTypeForecast());

            // Save results
            unitTypeForecastYearly.setUnitForecasts(unitForecastsYearly);
            unitTypeForecastYearly.setUnitTypeForecast(rentYearSummaryForUnitType);
            unitTypeForecastYearlyMap.put(unitType, unitTypeForecastYearly);
        });

        return unitTypeForecastYearlyMap;
    }

    public void calculateFiscalYearSummaries(Map<String, UnitTypeForecastMonthly> monthlyForecastDataForAllUnitTypes, Map<String, UnitTypeForecastYearly> unitTypeForecastYearlyMap, LocalDate closingDate) {
        monthlyForecastDataForAllUnitTypes.forEach((unitType, unitTypeForecastMonth) -> {
            UnitTypeForecastYearly yearlyDataForUnitType = unitTypeForecastYearlyMap.get(unitType);
            calculateFiscalYearSummary(unitTypeForecastMonth.getUnitTypeForecast(), closingDate, yearlyDataForUnitType.getUnitTypeForecast());
            unitTypeForecastMonth.getUnitForecasts().forEach((unitId, unitForecastMonth) -> {
                calculateFiscalYearSummary(unitForecastMonth,closingDate, unitTypeForecastYearlyMap.get(unitType).getUnitForecasts().get(unitId));
            });
        });
    }

    public void calculateFiscalYearSummary(List<RentForecastMonth> monthlyForecastData, LocalDate closingDate, List<RentForecastYear> rentForecastYears) {
        if (closingDate == null) {
            return;
        }
        int closingMonth = closingDate.getMonthValue();
        int year = 1;
        int month = 1;
        for (RentForecastMonth forecastMonth : monthlyForecastData) {
            if (forecastMonth.getYear() == 1 && forecastMonth.getMonth() <= closingMonth) {
                continue;
            }
            if (month > 12) {
                year++;
                month = 1;
            }
            RentForecastYear yearForecast = rentForecastYears.get(year - 1);
            yearForecast.setFiscalActualRent(yearForecast.getFiscalActualRent().add(forecastMonth.getActualRent()));
            yearForecast.setFiscalMarketRent(yearForecast.getFiscalMarketRent().add(forecastMonth.getMarketRent()));
            month++;
        }
    }

    /**
     * yearly summary for an individual unit type
     *
     * @param rentMonths - monthly forecast data for a unit type
     * @return - yearly forecast data for a unit type
     */
    private List<RentForecastYear> summarizeYearsForAUnitType(List<RentForecastMonth> rentMonths) {
        Map<Integer, RentForecastYear> yearSummaries = new LinkedHashMap<>();
        rentMonths.forEach(rentForecastMonth -> {
            int year = rentForecastMonth.getYear();
            if (!yearSummaries.containsKey(year)) {
                yearSummaries.put(year, RentForecastYear.builder()
                        .year(year)
                        .build());
            }
            var yearSummary = yearSummaries.get(year);
            yearSummary.setMarketRent(yearSummary.getMarketRent().add(rentForecastMonth.getMarketRent()));
            yearSummary.setActualRent(yearSummary.getActualRent().add(rentForecastMonth.getActualRent()));
        });

        // Convert the map to a list of RentYear objects
        return new ArrayList<>(yearSummaries.values());
    }

    /**
     * summarize yearly data for units by status
     *
     * @param yearlySummaries   - yearly summary data for all units
     * @param unitTypeForecasts - unit type forecast data
     * @return - yearly forecast data for all units grouped by unit statuses
     */
    public Map<String, UnitTypeForecastYearly> summarizeByUnitStatus(Map<String, UnitTypeForecastYearly> yearlySummaries, List<UnitTypeForecast> unitTypeForecasts) {
        Map<String, UnitTypeForecastYearly> unitTypeForecastYearlyMap = new HashMap<>();
        unitTypeForecasts.forEach(unitTypeForecast -> {
            String unitType = unitTypeForecast.getUnitType();
            UnitTypeForecastYearly unitTypeForecastYearly = new UnitTypeForecastYearly();
            Map<String, List<RentForecastYear>> unitStatusForecasts = new HashMap<>();
            unitTypeForecast.getUnitDetails().forEach((unitId, unitDetails) -> {
                String unitStatus = unitDetails.getUnitStatus();
                if (!unitStatusForecasts.containsKey(unitStatus)) {
                    unitStatusForecasts.put(unitStatus, new ArrayList<>());
                }
                List<RentForecastYear> rentForecastYearsForUnitId = yearlySummaries.get(unitType).getUnitForecasts().get(unitId);
                sumTwoForecastYears(unitStatusForecasts.get(unitStatus), rentForecastYearsForUnitId);
                unitTypeForecastYearly.setUnitForecasts(unitStatusForecasts);
            });
            unitTypeForecastYearlyMap.put(unitTypeForecast.getUnitType(), unitTypeForecastYearly);
        });
        return unitTypeForecastYearlyMap;
    }

    /**
     * Summarize all unit types at a yearly level
     *
     * @param rentYearsByUnitType - the yearly summaries for individual units
     */
    public void yearlySummaryByUnitType(Map<String, UnitTypeForecastYearly> rentYearsByUnitType) {
        UnitTypeForecastYearly allUnitTypeForecastYearly = new UnitTypeForecastYearly();
        List<RentForecastYear> unitTypeSummary = new ArrayList<>();

        rentYearsByUnitType.forEach((unitName, rentForecastYearly) -> {
            for (int i = 0; i < rentForecastYearly.getUnitTypeForecast().size(); i++) {
                RentForecastYear rentForecastYear = rentForecastYearly.getUnitTypeForecast().get(i);
                if (unitTypeSummary.size() <= i) {
                    unitTypeSummary.add(RentForecastYear.builder()
                            .year(rentForecastYear.getYear())
                            .actualRent(rentForecastYear.getActualRent())
                            .marketRent(rentForecastYear.getMarketRent()).build());
                } else {
                    RentForecastYear unitTypeForecastYear = unitTypeSummary.get(i);
                    unitTypeForecastYear.setActualRent(unitTypeForecastYear.getActualRent().add(rentForecastYear.getActualRent()));
                    unitTypeForecastYear.setMarketRent(unitTypeForecastYear.getMarketRent().add(rentForecastYear.getMarketRent()));
                }
            }
        });
        allUnitTypeForecastYearly.setUnitTypeForecast(unitTypeSummary);
        rentYearsByUnitType.put(ALL_UNITS, allUnitTypeForecastYearly);
    }

    /**
     * Summarizes the data for all unit types on a monthly basis
     *
     * @param rentMonthsByUnitType - map of forecasts by unit type
     */
    public void summarizeAllUnitTypes(Map<String, UnitTypeForecastMonthly> rentMonthsByUnitType) {
        UnitTypeForecastMonthly allUnitsForecastSummary = UnitTypeForecastMonthly.builder()
                .unitTypeForecast(new ArrayList<>()).build();

        rentMonthsByUnitType.values().forEach(unitTypeForecastMonthly -> {
            List<RentForecastMonth> allUnitsForecasts = allUnitsForecastSummary.getUnitTypeForecast();
            List<RentForecastMonth> unitTypeForecast = unitTypeForecastMonthly.getUnitTypeForecast();
            for (int i = 0; i < unitTypeForecast.size(); i++) {
                RentForecastMonth unitTypeRentForecastMonth = unitTypeForecast.get(i);
                if (allUnitsForecasts.size() <= i) {
                    allUnitsForecasts.add(RentForecastMonth.builder().month(unitTypeRentForecastMonth.getMonth())
                            .year(unitTypeRentForecastMonth.getYear()).build());
                }
                RentForecastMonth allUnitsForecastMonth = allUnitsForecasts.get(i);
                BigDecimal unitTypeForecastMonthMarketRent = unitTypeRentForecastMonth.getMarketRent();
                BigDecimal unitTypeForecastMonthActualRent = unitTypeRentForecastMonth.getActualRent();
                allUnitsForecastMonth.setActualRent(allUnitsForecastMonth.getActualRent().add(unitTypeForecastMonthActualRent));
                allUnitsForecastMonth.setMarketRent(allUnitsForecastMonth.getMarketRent().add(unitTypeForecastMonthMarketRent));

            }
        });

        rentMonthsByUnitType.put(ALL_UNITS, allUnitsForecastSummary);
    }


    /**
     * Sum 2 yearly forecast summaries
     *
     * @param forecastYearsToAddTo   - the list of years to add to
     * @param forecastYearsToAddFrom - the list of years to add from
     */
    private void sumTwoForecastYears(List<RentForecastYear> forecastYearsToAddTo, List<RentForecastYear> forecastYearsToAddFrom) {
        for (int i = 0; i < forecastYearsToAddFrom.size(); i++) {
            RentForecastYear forecastYearFrom = forecastYearsToAddFrom.get(i);
            if (forecastYearsToAddTo.size() <= i) {
                forecastYearsToAddTo.add(RentForecastYear.builder().year(forecastYearFrom.getYear()).build());
            }
            RentForecastYear forecastYearTo = forecastYearsToAddTo.get(i);

            forecastYearTo.setActualRent(forecastYearTo.getActualRent().add(forecastYearFrom.getActualRent()));
            forecastYearTo.setMarketRent(forecastYearTo.getMarketRent().add(forecastYearFrom.getMarketRent()));
            forecastYearTo.setFiscalActualRent(forecastYearTo.getFiscalActualRent().add(forecastYearFrom.getFiscalActualRent()));
            forecastYearTo.setFiscalMarketRent(forecastYearTo.getFiscalMarketRent().add(forecastYearFrom.getFiscalMarketRent()));

        }
    }
}
