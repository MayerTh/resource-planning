package org.resourceplanning.io;

import org.junit.jupiter.api.Test;
import org.resourceplanning.domain.Assignment;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TomlDataLoaderTest {

    @Test
    void loadsTeamMembersProjectsAndFlattenedAssignments() throws Exception {
        PlanningData data = TomlDataLoader.load(
            fixture("fixtures/valid/team_members.toml"),
            fixture("fixtures/valid/planning.toml"),
            fixture("fixtures/valid/working_days.toml")
        );

        assertEquals(3, data.teamMembers().size());
        TeamMember anna = data.teamMembers().stream()
            .filter(m -> m.id().equals("anna.mayer"))
            .findFirst().orElseThrow();
        assertEquals("Mayer", anna.lastName());
        assertEquals(8.0, anna.hoursDaily());
        assertTrue(anna.skills().contains("kotlin"));

        assertEquals(2, data.projects().size());
        Project migration = data.projects().stream()
            .filter(p -> p.id().equals("data-platform-migration"))
            .findFirst().orElseThrow();
        assertEquals(YearMonth.of(2026, 3), migration.startMonth());
        assertEquals(YearMonth.of(2026, 5), migration.endMonth());

        // The nested per-project, per-month TOML structure (ADR 0007) is flattened
        // into standalone Assignment facts (ADR 0015) - one per {project, month, member}.
        assertEquals(6, data.assignments().size());
        assertTrue(data.assignments().contains(
            new Assignment("jonas.schmidt", "data-platform-migration", YearMonth.of(2026, 5), 120.0)));

        assertEquals(22, data.workingDays().workingDays(YearMonth.of(2026, 3)));
    }

    public static Path fixture(String classpathResource) throws URISyntaxException {
        return Path.of(TomlDataLoaderTest.class.getClassLoader().getResource(classpathResource).toURI());
    }
}
