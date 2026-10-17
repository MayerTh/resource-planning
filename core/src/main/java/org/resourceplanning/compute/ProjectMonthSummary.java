package org.resourceplanning.compute;

import org.resourceplanning.domain.Project;

import java.time.YearMonth;
import java.util.List;

public record ProjectMonthSummary(
    Project project,
    YearMonth month,
    List<String> coveredSkills,
    List<String> uncoveredSkills
) {
}
