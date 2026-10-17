package org.resourceplanning.compute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;
import org.resourceplanning.io.TomlDataLoader;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.resourceplanning.io.TomlDataLoaderTest.fixture;

class CapacityCalculatorTest {

    private CapacityCalculator calculator;
    private TeamMember anna;
    private TeamMember jonas;
    private TeamMember lea;
    private Project internalToolsRevamp;
    private Project dataPlatformMigration;

    @BeforeEach
    void loadFixture() throws Exception {
        PlanningData data = TomlDataLoader.load(
            fixture("fixtures/valid/team_members.toml"),
            fixture("fixtures/valid/planning.toml"),
            fixture("fixtures/valid/working_days.toml")
        );
        calculator = new CapacityCalculator(data);
        anna = memberById(data, "anna.mayer");
        jonas = memberById(data, "jonas.schmidt");
        lea = memberById(data, "lea.fischer");
        internalToolsRevamp = projectById(data, "internal-tools-revamp");
        dataPlatformMigration = projectById(data, "data-platform-migration");
    }

    @Test
    void memberWithPartialAssignmentHasSpareCapacityAndIsNotOverAllocated() {
        MemberMonthSummary summary = calculator.memberMonthSummary(anna, YearMonth.of(2026, 1));

        assertEquals(25.0, summary.assignedPercentage());
        assertEquals(75.0, summary.sparePercentage());
        assertFalse(summary.overAllocated());
        assertEquals(1, summary.allocations().size());
        assertEquals("internal-tools-revamp", summary.allocations().get(0).projectId());
    }

    @Test
    void memberAssignedOverFullCapacityIsFlaggedOverAllocatedWithNoSpare() {
        MemberMonthSummary summary = calculator.memberMonthSummary(jonas, YearMonth.of(2026, 5));

        assertEquals(120.0, summary.assignedPercentage());
        assertEquals(0.0, summary.sparePercentage());
        assertTrue(summary.overAllocated());
    }

    @Test
    void memberWithNoAssignmentThatMonthIsFullySpare() {
        MemberMonthSummary summary = calculator.memberMonthSummary(lea, YearMonth.of(2026, 1));

        assertEquals(0.0, summary.assignedPercentage());
        assertEquals(100.0, summary.sparePercentage());
        assertFalse(summary.overAllocated());
        assertTrue(summary.allocations().isEmpty());
    }

    @Test
    void projectMissingAssignedSkillShowsUpAsUncovered() {
        ProjectMonthSummary summary = calculator.projectMonthSummary(internalToolsRevamp, YearMonth.of(2026, 1));

        // anna.mayer (java, kotlin, sql) is the only one assigned -> java is covered, typescript is not.
        assertEquals(List.of("java"), summary.coveredSkills());
        assertEquals(List.of("typescript"), summary.uncoveredSkills());
    }

    @Test
    void projectWithBothAssigneesCoveringDifferentSkillsStillMissesUncoveredOnes() {
        ProjectMonthSummary summary = calculator.projectMonthSummary(dataPlatformMigration, YearMonth.of(2026, 3));

        // lea.fischer (java, typescript, python) + jonas.schmidt (python, go) together cover python, not sql.
        assertEquals(List.of("python"), summary.coveredSkills());
        assertEquals(List.of("sql"), summary.uncoveredSkills());
    }

    @Test
    void projectMonthSummariesSpanTheProjectsFullDuration() {
        List<ProjectMonthSummary> summaries = calculator.projectMonthSummaries().stream()
            .filter(s -> s.project().id().equals("data-platform-migration"))
            .toList();

        assertEquals(3, summaries.size());
    }

    private static TeamMember memberById(PlanningData data, String id) {
        return data.teamMembers().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
    }

    private static Project projectById(PlanningData data, String id) {
        return data.projects().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }
}
