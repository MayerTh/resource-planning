package org.resourceplanning.io;

import org.resourceplanning.domain.Assignment;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class TomlDataWriter {

    private TomlDataWriter() {
    }

    public static void writeTeamMembers(Path file, List<TeamMember> members) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            TeamMember member = members.get(i);
            out.append("[[member]]\n");
            out.append("id = ").append(quote(member.id())).append('\n');
            out.append("first_name = ").append(quote(member.firstName())).append('\n');
            out.append("last_name = ").append(quote(member.lastName())).append('\n');
            out.append("team = ").append(quote(member.team())).append('\n');
            out.append("role = ").append(quote(member.role())).append('\n');
            out.append("hours_daily = ").append(formatNumber(member.hoursDaily())).append('\n');
            out.append("pay_schema = ").append(quote(member.paySchema().name().toLowerCase())).append('\n');
            out.append("skills = ").append(stringArray(member.skills())).append('\n');
            if (i < members.size() - 1) {
                out.append('\n');
            }
        }
        Files.writeString(file, out.toString());
    }

    public static void writePlanning(Path file, List<Project> projects, List<Assignment> assignments) throws IOException {
        Map<String, Map<YearMonth, List<Assignment>>> byProject = groupAssignments(assignments);

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            out.append("[[project]]\n");
            out.append("id = ").append(quote(project.id())).append('\n');
            out.append("name = ").append(quote(project.name())).append('\n');
            out.append("start_month = ").append(quote(project.startMonth().toString())).append('\n');
            out.append("end_month = ").append(quote(project.endMonth().toString())).append('\n');
            out.append("hours_total = ").append(formatNumber(project.hoursTotal())).append('\n');
            out.append("skills_needed = ").append(stringArray(project.skillsNeeded())).append('\n');
            out.append("responsible = ").append(quote(project.responsible())).append('\n');

            Map<YearMonth, List<Assignment>> monthly = byProject.get(project.id());
            if (monthly != null && !monthly.isEmpty()) {
                out.append("\n  [project.assignments]\n");
                for (Map.Entry<YearMonth, List<Assignment>> entry : monthly.entrySet()) {
                    String entriesText = entry.getValue().stream()
                        .map(a -> "{ member = " + quote(a.member()) + ", percentage = " + formatNumber(a.percentage()) + " }")
                        .collect(Collectors.joining(", "));
                    out.append("  ").append(quote(entry.getKey().toString())).append(" = [ ").append(entriesText).append(" ]\n");
                }
            }
            if (i < projects.size() - 1) {
                out.append('\n');
            }
        }
        Files.writeString(file, out.toString());
    }

    private static Map<String, Map<YearMonth, List<Assignment>>> groupAssignments(List<Assignment> assignments) {
        Map<String, Map<YearMonth, List<Assignment>>> byProject = new TreeMap<>();
        for (Assignment assignment : assignments) {
            byProject
                .computeIfAbsent(assignment.project(), p -> new TreeMap<>())
                .computeIfAbsent(assignment.month(), m -> new ArrayList<>())
                .add(assignment);
        }
        return byProject;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String stringArray(List<String> values) {
        return "[" + values.stream().map(TomlDataWriter::quote).collect(Collectors.joining(", ")) + "]";
    }

}
