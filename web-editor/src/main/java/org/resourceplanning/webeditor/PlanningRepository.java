package org.resourceplanning.webeditor;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.resourceplanning.domain.Assignment;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;
import org.resourceplanning.io.TomlDataLoader;
import org.resourceplanning.io.TomlDataWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class PlanningRepository {

    @ConfigProperty(name = "resourceplanning.data-dir", defaultValue = "../data")
    String dataDir;

    public PlanningData load() {
        try {
            return TomlDataLoader.load(teamMembersFile(), planningFile(), workingDaysFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void saveTeamMembers(List<TeamMember> members) {
        try {
            TomlDataWriter.writeTeamMembers(teamMembersFile(), members);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void savePlanning(List<Project> projects, List<Assignment> assignments) {
        try {
            TomlDataWriter.writePlanning(planningFile(), projects, assignments);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path teamMembersFile() {
        return Path.of(dataDir, "team_members.toml");
    }

    private Path planningFile() {
        return Path.of(dataDir, "planning.toml");
    }

    private Path workingDaysFile() {
        return Path.of(dataDir, "working_days.toml");
    }
}
