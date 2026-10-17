package org.resourceplanning.report;

import org.resourceplanning.compute.MemberMonthSummary;
import org.resourceplanning.compute.ProjectMonthSummary;

import java.util.Comparator;
import java.util.List;

public final class TerminalReportRenderer {

    private TerminalReportRenderer() {
    }

    public static String render(List<MemberMonthSummary> memberSummaries, List<ProjectMonthSummary> projectSummaries) {
        StringBuilder out = new StringBuilder();
        out.append("=== Member capacity ===\n");
        appendMemberTable(out, memberSummaries);
        out.append("\n=== Project skill coverage ===\n");
        appendProjectTable(out, projectSummaries);
        return out.toString();
    }

    private static void appendMemberTable(StringBuilder out, List<MemberMonthSummary> summaries) {
        List<MemberMonthSummary> sorted = summaries.stream()
            .sorted(Comparator.comparing((MemberMonthSummary s) -> s.member().id()).thenComparing(MemberMonthSummary::month))
            .toList();
        out.append(String.format("%-16s %-8s %9s %9s %-6s %s%n", "Member", "Month", "Assigned", "Spare", "Over?", "Skills"));
        for (MemberMonthSummary s : sorted) {
            out.append(String.format("%-16s %-8s %8.0f%% %8.0f%% %-6s %s%n",
                s.member().id(), s.month(), s.assignedPercentage(), s.sparePercentage(),
                s.overAllocated() ? "yes" : "no", String.join(", ", s.member().skills())));
        }
    }

    private static void appendProjectTable(StringBuilder out, List<ProjectMonthSummary> summaries) {
        List<ProjectMonthSummary> sorted = summaries.stream()
            .sorted(Comparator.comparing((ProjectMonthSummary s) -> s.project().id()).thenComparing(ProjectMonthSummary::month))
            .toList();
        out.append(String.format("%-24s %-8s %-20s %-20s%n", "Project", "Month", "Covered skills", "Missing skills"));
        for (ProjectMonthSummary s : sorted) {
            out.append(String.format("%-24s %-8s %-20s %-20s%n",
                s.project().id(), s.month(),
                String.join(", ", s.coveredSkills()),
                String.join(", ", s.uncoveredSkills())));
        }
    }
}
