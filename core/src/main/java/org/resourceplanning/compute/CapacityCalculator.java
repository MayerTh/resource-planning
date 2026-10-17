package org.resourceplanning.compute;

import org.resourceplanning.domain.Assignment;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class CapacityCalculator {

    private final PlanningData data;
    private final Map<String, TeamMember> membersById;

    public CapacityCalculator(PlanningData data) {
        this.data = data;
        this.membersById = data.teamMembers().stream()
            .collect(Collectors.toMap(TeamMember::id, member -> member));
    }

    public List<MemberMonthSummary> memberMonthSummaries() {
        List<MemberMonthSummary> summaries = new ArrayList<>();
        for (TeamMember member : data.teamMembers()) {
            for (YearMonth month : assignedMonths()) {
                summaries.add(memberMonthSummary(member, month));
            }
        }
        return summaries;
    }

    public MemberMonthSummary memberMonthSummary(TeamMember member, YearMonth month) {
        List<MemberAllocation> allocations = new ArrayList<>();
        double total = 0.0;
        for (Assignment assignment : data.assignments()) {
            if (assignment.member().equals(member.id()) && assignment.month().equals(month)) {
                allocations.add(new MemberAllocation(assignment.project(), assignment.percentage()));
                total += assignment.percentage();
            }
        }
        double spare = Math.max(0.0, 100.0 - total);
        boolean overAllocated = total > 100.0;
        return new MemberMonthSummary(member, month, total, spare, overAllocated, allocations);
    }

    public List<ProjectMonthSummary> projectMonthSummaries() {
        List<ProjectMonthSummary> summaries = new ArrayList<>();
        for (Project project : data.projects()) {
            for (YearMonth month = project.startMonth(); !month.isAfter(project.endMonth()); month = month.plusMonths(1)) {
                summaries.add(projectMonthSummary(project, month));
            }
        }
        return summaries;
    }

    public ProjectMonthSummary projectMonthSummary(Project project, YearMonth month) {
        Set<String> assignedSkills = new LinkedHashSet<>();
        for (Assignment assignment : data.assignments()) {
            if (assignment.project().equals(project.id()) && assignment.month().equals(month) && assignment.percentage() > 0) {
                TeamMember member = membersById.get(assignment.member());
                if (member != null) {
                    assignedSkills.addAll(member.skills());
                }
            }
        }

        List<String> covered = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();
        for (String skill : project.skillsNeeded()) {
            if (assignedSkills.contains(skill)) {
                covered.add(skill);
            } else {
                uncovered.add(skill);
            }
        }
        return new ProjectMonthSummary(project, month, covered, uncovered);
    }

    private List<YearMonth> assignedMonths() {
        Set<YearMonth> months = new TreeSet<>();
        for (Assignment assignment : data.assignments()) {
            months.add(assignment.month());
        }
        return new ArrayList<>(months);
    }
}
