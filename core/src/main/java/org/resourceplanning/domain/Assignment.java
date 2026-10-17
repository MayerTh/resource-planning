package org.resourceplanning.domain;

import java.time.YearMonth;

public record Assignment(
    String member,
    String project,
    YearMonth month,
    double percentage
) {
}
