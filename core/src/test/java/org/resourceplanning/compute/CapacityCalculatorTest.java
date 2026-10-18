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

    @Test
    void memberMonthSummariesFilteredByFromExcludesEarlierMonths() {
        // Fixture assignments only exist in Jan/Feb/Mar/May 2026; filtering from March
        // should drop the Jan/Feb rows for every member, keeping Mar and May.
        List<MemberMonthSummary> summaries = calculator.memberMonthSummaries(YearMonth.of(2026, 3));

        assertEquals(6, summaries.size());
        assertTrue(summaries.stream().noneMatch(s -> s.month().isBefore(YearMonth.of(2026, 3))));
    }

    @Test
    void projectMonthSummariesFilteredByFromClipsToProjectsRemainingMonths() {
        // internal-tools-revamp (Jan-Mar) is clipped to just March; data-platform-migration
        // (Mar-May) is unaffected since it already starts at/after the filter.
        List<ProjectMonthSummary> summaries = calculator.projectMonthSummaries(YearMonth.of(2026, 3));

        assertEquals(4, summaries.size());
        assertTrue(summaries.stream().noneMatch(s -> s.month().isBefore(YearMonth.of(2026, 3))));
        assertEquals(1, summaries.stream().filter(s -> s.project().id().equals("internal-tools-revamp")).count());
    }

    @Test
    void projectHeadcountGapSpreadsHoursTotalEvenlyAcrossProjectDuration() {
        // internal-tools-revamp: 800h over Jan-Mar (3 months) -> 266.667h/month demand.
        // January: only anna.mayer at 25% * 8h/day * 20 working days = 40h assigned.
        ProjectHeadcountGap january = calculator.projectHeadcountGaps().stream()
            .filter(g -> g.project().id().equals("internal-tools-revamp") && g.month().equals(YearMonth.of(2026, 1)))
            .findFirst().orElseThrow();

        assertEquals(800.0 / 3, january.demandHours(), 1e-6);
        assertEquals(40.0, january.assignedHours(), 1e-6);
        // Team average is (8+8+6)/3 = 7.333h/day; headcount gap = gapHours / (avg * workingDays).
        assertEquals(17.0 / 11, january.gapHeadcount(), 1e-6);
    }

    @Test
    void projectHeadcountGapsFilteredByFromKeepsFullDurationDemandButClipsRows() {
        List<ProjectHeadcountGap> filtered = calculator.projectHeadcountGaps(YearMonth.of(2026, 3)).stream()
            .filter(g -> g.project().id().equals("internal-tools-revamp"))
            .toList();

        assertEquals(1, filtered.size());
        assertEquals(YearMonth.of(2026, 3), filtered.get(0).month());
        // Demand per month stays based on the project's full Jan-Mar duration, not recomputed
        // as if it only ran for the one remaining (filtered) month.
        assertEquals(800.0 / 3, filtered.get(0).demandHours(), 1e-6);
    }

    @Test
    void overallHeadcountGapComparesTotalProjectDemandAgainstTotalTeamSupply() {
        // January: only internal-tools-revamp is active (266.667h demand). Team supply is
        // anna(8h) + jonas(8h) + lea(6h) daily * 20 working days = 440h -> a surplus that month.
        MonthlyHeadcountGap january = calculator.overallHeadcountGaps().stream()
            .filter(g -> g.month().equals(YearMonth.of(2026, 1)))
            .findFirst().orElseThrow();

        assertEquals(800.0 / 3, january.demandHours(), 1e-6);
        assertEquals(440.0, january.supplyHours(), 1e-6);
        assertEquals(-13.0 / 11, january.gapHeadcount(), 1e-6);
    }

    private static TeamMember memberById(PlanningData data, String id) {
        return data.teamMembers().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
    }

    private static Project projectById(PlanningData data, String id) {
        return data.projects().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }
}
