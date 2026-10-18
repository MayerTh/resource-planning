package org.resourceplanning.compute;

import java.time.YearMonth;

public record MonthlyHeadcountGap(
    YearMonth month,
    double demandHours,
    double supplyHours,
    double gapHours,
    double gapHeadcount
) {
}
