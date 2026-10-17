package org.resourceplanning.domain;

import java.time.YearMonth;
import java.util.List;

public record Project(
    String id,
    String name,
    YearMonth startMonth,
    YearMonth endMonth,
    double hoursTotal,
    List<String> skillsNeeded,
    String responsible
) {
}
