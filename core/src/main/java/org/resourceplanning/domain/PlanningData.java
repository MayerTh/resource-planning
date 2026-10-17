package org.resourceplanning.domain;

import java.util.List;

public record PlanningData(
    List<TeamMember> teamMembers,
    List<Project> projects,
    List<Assignment> assignments,
    WorkingDaysCalendar workingDays
) {
}
