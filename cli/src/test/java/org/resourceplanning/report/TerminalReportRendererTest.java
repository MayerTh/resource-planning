package org.resourceplanning.report;

import org.junit.jupiter.api.Test;
import org.resourceplanning.compute.MemberAllocation;
import org.resourceplanning.compute.MemberMonthSummary;
import org.resourceplanning.compute.ProjectMonthSummary;
import org.resourceplanning.domain.PaySchema;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalReportRendererTest {

    @Test
    void rendersMemberAndProjectTablesAsPlainText() {
        TeamMember anna = new TeamMember("anna.mayer", "Anna", "Mayer", "Platform", "Engineer",
            8.0, PaySchema.D4, List.of("java"));
        MemberMonthSummary memberSummary = new MemberMonthSummary(anna, YearMonth.of(2026, 5), 120.0, 0.0, true,
            List.of(new MemberAllocation("data-platform-migration", 120.0)));

        Project project = new Project("data-platform-migration", "Data Platform Migration",
            YearMonth.of(2026, 3), YearMonth.of(2026, 5), 1400.0, List.of("python", "sql"), "Lea Fischer");
        ProjectMonthSummary projectSummary = new ProjectMonthSummary(project, YearMonth.of(2026, 3),
            List.of("python"), List.of("sql"));

        String rendered = TerminalReportRenderer.render(List.of(memberSummary), List.of(projectSummary));

        assertTrue(rendered.contains("anna.mayer"));
        assertTrue(rendered.contains("120%"));
        assertTrue(rendered.contains("yes"));
        assertTrue(rendered.contains("data-platform-migration"));
        assertTrue(rendered.contains("python"));
        assertTrue(rendered.contains("sql"));
    }
}
