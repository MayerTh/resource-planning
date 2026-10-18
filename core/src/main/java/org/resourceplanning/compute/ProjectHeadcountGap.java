package org.resourceplanning.compute;

import org.resourceplanning.domain.Project;

import java.time.YearMonth;

public record ProjectHeadcountGap(
    Project project,
    YearMonth month,
    double demandHours,
    double assignedHours,
    double gapHours,
    double gapHeadcount
) {
}
