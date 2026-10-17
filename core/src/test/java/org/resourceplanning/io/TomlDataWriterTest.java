package org.resourceplanning.io;

import org.junit.jupiter.api.Test;
import org.resourceplanning.domain.Assignment;
import org.resourceplanning.domain.PaySchema;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TomlDataWriterTest {

    @Test
    void writtenTeamMembersAndPlanningReloadToTheSameData(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        List<TeamMember> members = List.of(
            new TeamMember("anna.mayer", "Anna", "Mayer", "Platform", "Engineer", 8.0, PaySchema.D4, List.of("java", "sql")),
            new TeamMember("jonas.schmidt", "Jonas", "Schmidt", "Data", "Engineer", 6.0, PaySchema.D3, List.of("python"))
        );
        List<Project> projects = List.of(
            new Project("revamp", "Revamp", YearMonth.of(2026, 1), YearMonth.of(2026, 2), 500.0, List.of("java"), "Someone")
        );
        List<Assignment> assignments = List.of(
            new Assignment("anna.mayer", "revamp", YearMonth.of(2026, 1), 50.0),
            new Assignment("jonas.schmidt", "revamp", YearMonth.of(2026, 1), 25.0)
        );

        Path teamMembersFile = tempDir.resolve("team_members.toml");
        Path planningFile = tempDir.resolve("planning.toml");
        Path workingDaysFile = tempDir.resolve("working_days.toml");
        Files.writeString(workingDaysFile, "[[working_days]]\nyear = 2026\nmonth = 1\ncount = 20\n");

        TomlDataWriter.writeTeamMembers(teamMembersFile, members);
        TomlDataWriter.writePlanning(planningFile, projects, assignments);

        PlanningData reloaded = TomlDataLoader.load(teamMembersFile, planningFile, workingDaysFile);

        assertEquals(members, reloaded.teamMembers());
        assertEquals(projects, reloaded.projects());
        assertEquals(2, reloaded.assignments().size());
        assertEquals(assignments.get(0), reloaded.assignments().get(0));
        assertEquals(assignments.get(1), reloaded.assignments().get(1));
    }
}
