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
        return memberMonthSummaries(null);
    }

    /**
     * Same as {@link #memberMonthSummaries()}, but only for months not before {@code from}
     * (pass {@code null} for no lower bound).
     */
    public List<MemberMonthSummary> memberMonthSummaries(YearMonth from) {
        List<MemberMonthSummary> summaries = new ArrayList<>();
        for (TeamMember member : data.teamMembers()) {
            for (YearMonth month : assignedMonths()) {
                if (from == null || !month.isBefore(from)) {
                    summaries.add(memberMonthSummary(member, month));
                }
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
        return projectMonthSummaries(null);
    }

    /**
     * Same as {@link #projectMonthSummaries()}, but only for months not before {@code from}
     * (pass {@code null} for no lower bound).
     */
    public List<ProjectMonthSummary> projectMonthSummaries(YearMonth from) {
        List<ProjectMonthSummary> summaries = new ArrayList<>();
        for (Project project : data.projects()) {
            YearMonth start = effectiveStart(project.startMonth(), from);
            for (YearMonth month = start; !month.isAfter(project.endMonth()); month = month.plusMonths(1)) {
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

    /**
     * Per-project, per-month headcount gap: how a project's {@code hours_total} (spread evenly
     * across its own start/end months) compares against the hours actually assigned to it that
     * month, expressed both in hours and in heads (via {@link #referenceHeadcountHours}).
     * Positive {@code gapHeadcount} means short-staffed; negative means over-staffed relative to
     * the project's rough hour budget.
     */
    public List<ProjectHeadcountGap> projectHeadcountGaps() {
        return projectHeadcountGaps(null);
    }

    /** Same as {@link #projectHeadcountGaps()}, but only for months not before {@code from}. */
    public List<ProjectHeadcountGap> projectHeadcountGaps(YearMonth from) {
        List<ProjectHeadcountGap> gaps = new ArrayList<>();
        for (Project project : data.projects()) {
            double demandHours = demandHoursPerMonth(project);
            YearMonth start = effectiveStart(project.startMonth(), from);
            for (YearMonth month = start; !month.isAfter(project.endMonth()); month = month.plusMonths(1)) {
                double assignedHours = assignedHoursForProjectMonth(project, month);
                double gapHours = demandHours - assignedHours;
                gaps.add(new ProjectHeadcountGap(project, month, demandHours, assignedHours, gapHours,
                    toHeadcount(gapHours, month)));
            }
        }
        return gaps;
    }

    /**
     * Company-wide, per-month headcount gap: total project demand (sum of every active project's
     * {@code hours_total} spread evenly across its own duration) against the current team's total
     * capacity that month — independent of how assignments are actually distributed. Answers "how
     * many more people would I need to hire to cover everything," not "is the work well-allocated."
     */
    public List<MonthlyHeadcountGap> overallHeadcountGaps() {
        return overallHeadcountGaps(null);
    }

    /** Same as {@link #overallHeadcountGaps()}, but only for months not before {@code from}. */
    public List<MonthlyHeadcountGap> overallHeadcountGaps(YearMonth from) {
        Set<YearMonth> months = new TreeSet<>();
        for (Project project : data.projects()) {
            YearMonth start = effectiveStart(project.startMonth(), from);
            for (YearMonth month = start; !month.isAfter(project.endMonth()); month = month.plusMonths(1)) {
                months.add(month);
            }
        }

        List<MonthlyHeadcountGap> gaps = new ArrayList<>();
        for (YearMonth month : months) {
            double demandHours = 0.0;
            for (Project project : data.projects()) {
                if (!month.isBefore(project.startMonth()) && !month.isAfter(project.endMonth())) {
                    demandHours += demandHoursPerMonth(project);
                }
            }
            double supplyHours = teamSupplyHoursForMonth(month);
            double gapHours = demandHours - supplyHours;
            gaps.add(new MonthlyHeadcountGap(month, demandHours, supplyHours, gapHours, toHeadcount(gapHours, month)));
        }
        return gaps;
    }

    private double demandHoursPerMonth(Project project) {
        return project.hoursTotal() / projectDurationInMonths(project);
    }

    private static int projectDurationInMonths(Project project) {
        int startIndex = project.startMonth().getYear() * 12 + project.startMonth().getMonthValue();
        int endIndex = project.endMonth().getYear() * 12 + project.endMonth().getMonthValue();
        return endIndex - startIndex + 1;
    }

    private double assignedHoursForProjectMonth(Project project, YearMonth month) {
        double hours = 0.0;
        for (Assignment assignment : data.assignments()) {
            if (assignment.project().equals(project.id()) && assignment.month().equals(month)) {
                TeamMember member = membersById.get(assignment.member());
                if (member != null) {
                    hours += assignment.percentage() / 100.0 * member.hoursDaily() * data.workingDays().workingDays(month);
                }
            }
        }
        return hours;
    }

    private double teamSupplyHoursForMonth(YearMonth month) {
        double hours = 0.0;
        for (TeamMember member : data.teamMembers()) {
            hours += member.hoursDaily() * data.workingDays().workingDays(month);
        }
        return hours;
    }

    /** Falls back to a standard 8h workday if there's no team yet to average over. */
    private double averageHoursDaily() {
        if (data.teamMembers().isEmpty()) {
            return 8.0;
        }
        return data.teamMembers().stream().mapToDouble(TeamMember::hoursDaily).average().orElse(8.0);
    }

    private double referenceHeadcountHours(YearMonth month) {
        return averageHoursDaily() * data.workingDays().workingDays(month);
    }

    private double toHeadcount(double hours, YearMonth month) {
        double referenceHours = referenceHeadcountHours(month);
        return referenceHours == 0.0 ? 0.0 : hours / referenceHours;
    }

    private static YearMonth effectiveStart(YearMonth projectStart, YearMonth from) {
        return (from != null && from.isAfter(projectStart)) ? from : projectStart;
    }

    private List<YearMonth> assignedMonths() {
        Set<YearMonth> months = new TreeSet<>();
        for (Assignment assignment : data.assignments()) {
            months.add(assignment.month());
        }
        return new ArrayList<>(months);
    }
}
