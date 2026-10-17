package org.resourceplanning.compute;

import org.resourceplanning.domain.TeamMember;

import java.time.YearMonth;
import java.util.List;

public record MemberMonthSummary(
    TeamMember member,
    YearMonth month,
    double assignedPercentage,
    double sparePercentage,
    boolean overAllocated,
    List<MemberAllocation> allocations
) {
}
