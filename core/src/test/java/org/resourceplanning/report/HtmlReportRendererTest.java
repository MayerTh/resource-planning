package org.resourceplanning.report;

import org.junit.jupiter.api.Test;
import org.resourceplanning.compute.MemberAllocation;
import org.resourceplanning.compute.MemberMonthSummary;
import org.resourceplanning.compute.MonthlyHeadcountGap;
import org.resourceplanning.compute.ProjectHeadcountGap;
import org.resourceplanning.compute.ProjectMonthSummary;
import org.resourceplanning.domain.PaySchema;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlReportRendererTest {

    @Test
    void rendersSortableTablesAndFlagsOverAllocationWithoutUnescapedInput() {
        TeamMember anna = new TeamMember("anna.mayer", "Anna", "Mayer", "Platform", "Engineer",
            8.0, PaySchema.D4, List.of("java", "<script>"));
        MemberMonthSummary memberSummary = new MemberMonthSummary(anna, YearMonth.of(2026, 5), 120.0, 0.0, true,
            List.of(new MemberAllocation("data-platform-migration", 120.0)));

        Project project = new Project("data-platform-migration", "Data Platform Migration",
            YearMonth.of(2026, 3), YearMonth.of(2026, 5), 1400.0, List.of("python", "sql"), "Lea Fischer");
        ProjectMonthSummary projectSummary = new ProjectMonthSummary(project, YearMonth.of(2026, 3),
            List.of("python"), List.of("sql"));

        MonthlyHeadcountGap overallGap = new MonthlyHeadcountGap(YearMonth.of(2026, 5), 200.0, 160.0, 40.0, 0.25);
        ProjectHeadcountGap projectGap = new ProjectHeadcountGap(project, YearMonth.of(2026, 5), 200.0, 120.0, 80.0, 0.5);

        String html = HtmlReportRenderer.render(List.of(memberSummary), List.of(projectSummary),
            List.of(overallGap), List.of(projectGap));

        assertTrue(html.contains("class=\"sortable\""));
        assertTrue(html.contains("over-allocated"));
        assertTrue(html.contains("short-staffed"));
        assertTrue(html.contains("<svg"));
        assertTrue(html.contains("class=\"heatmap\""));
        assertTrue(html.contains("function sortTable"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(!html.contains("<script>java"));
    }
}
