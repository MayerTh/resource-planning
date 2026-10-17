package org.resourceplanning.io;

import org.resourceplanning.domain.Assignment;
import org.resourceplanning.domain.PaySchema;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.Project;
import org.resourceplanning.domain.TeamMember;
import org.resourceplanning.domain.WorkingDaysCalendar;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TomlDataLoader {

    private TomlDataLoader() {
    }

    public static PlanningData load(Path teamMembersFile, Path planningFile, Path workingDaysFile) throws IOException {
        List<String> errors = new ArrayList<>();

        TomlParseResult teamMembersToml = parse(teamMembersFile, errors);
        TomlParseResult planningToml = parse(planningFile, errors);
        TomlParseResult workingDaysToml = parse(workingDaysFile, errors);

        if (!errors.isEmpty()) {
            throw new DataValidationException(errors);
        }

        List<TeamMember> teamMembers = readTeamMembers(teamMembersToml, errors);

        Set<String> memberIds = new HashSet<>();
        for (TeamMember member : teamMembers) {
            memberIds.add(member.id());
        }

        WorkingDaysCalendar workingDays = readWorkingDays(workingDaysToml, errors);
        ProjectsAndAssignments projectsAndAssignments = readProjectsAndAssignments(planningToml, memberIds, errors);

        if (!errors.isEmpty()) {
            throw new DataValidationException(errors);
        }

        return new PlanningData(teamMembers, projectsAndAssignments.projects(), projectsAndAssignments.assignments(), workingDays);
    }

    private static TomlParseResult parse(Path file, List<String> errors) throws IOException {
        TomlParseResult result = Toml.parse(file);
        for (TomlParseError error : result.errors()) {
            errors.add(file + ": " + error);
        }
        return result;
    }

    private static List<TeamMember> readTeamMembers(TomlTable root, List<String> errors) {
        List<TeamMember> members = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        TomlArray array = root.getArray("member");
        if (array == null) {
            return members;
        }

        for (int i = 0; i < array.size(); i++) {
            TomlTable table = array.getTable(i);
            String context = "team_members.toml [[member]] #" + (i + 1);

            String id = requireString(table, "id", errors, context);
            if (id != null && !seenIds.add(id)) {
                errors.add(context + ": duplicate id '" + id + "'");
            }
            String firstName = requireString(table, "first_name", errors, context);
            String lastName = requireString(table, "last_name", errors, context);
            String team = requireString(table, "team", errors, context);
            String role = requireString(table, "role", errors, context);
            Double hoursDaily = requireNumber(table, "hours_daily", errors, context);
            String paySchemaCode = requireString(table, "pay_schema", errors, context);
            PaySchema paySchema = null;
            if (paySchemaCode != null) {
                paySchema = PaySchema.fromCode(paySchemaCode).orElse(null);
                if (paySchema == null) {
                    errors.add(context + ": invalid pay_schema '" + paySchemaCode + "' (expected one of d3, d4, d5)");
                }
            }
            List<String> skills = requireStringArray(table, "skills", errors, context);

            if (id != null && firstName != null && lastName != null && team != null && role != null
                && hoursDaily != null && paySchema != null && skills != null) {
                members.add(new TeamMember(id, firstName, lastName, team, role, hoursDaily, paySchema, skills));
            }
        }
        return members;
    }

    private static WorkingDaysCalendar readWorkingDays(TomlTable root, List<String> errors) {
        Map<YearMonth, Integer> counts = new HashMap<>();
        TomlArray array = root.getArray("working_days");
        if (array == null) {
            return new WorkingDaysCalendar(counts);
        }

        for (int i = 0; i < array.size(); i++) {
            TomlTable table = array.getTable(i);
            String context = "working_days.toml [[working_days]] #" + (i + 1);

            Double year = requireNumber(table, "year", errors, context);
            Double month = requireNumber(table, "month", errors, context);
            Double count = requireNumber(table, "count", errors, context);
            if (year == null || month == null || count == null) {
                continue;
            }
            int monthValue = month.intValue();
            if (monthValue < 1 || monthValue > 12) {
                errors.add(context + ": month must be between 1 and 12, got " + monthValue);
                continue;
            }
            if (count < 0) {
                errors.add(context + ": count must not be negative, got " + count);
                continue;
            }
            YearMonth yearMonth = YearMonth.of(year.intValue(), monthValue);
            if (counts.containsKey(yearMonth)) {
                errors.add(context + ": duplicate entry for " + yearMonth);
                continue;
            }
            counts.put(yearMonth, count.intValue());
        }
        return new WorkingDaysCalendar(counts);
    }

    private static ProjectsAndAssignments readProjectsAndAssignments(TomlTable root, Set<String> memberIds, List<String> errors) {
        List<Project> projects = new ArrayList<>();
        List<Assignment> assignments = new ArrayList<>();
        Set<String> seenProjectIds = new HashSet<>();
        TomlArray array = root.getArray("project");
        if (array == null) {
            return new ProjectsAndAssignments(projects, assignments);
        }

        for (int i = 0; i < array.size(); i++) {
            TomlTable table = array.getTable(i);
            String context = "planning.toml [[project]] #" + (i + 1);

            String id = requireString(table, "id", errors, context);
            if (id != null && !seenProjectIds.add(id)) {
                errors.add(context + ": duplicate id '" + id + "'");
            }
            String name = requireString(table, "name", errors, context);
            YearMonth startMonth = requireYearMonth(table, "start_month", errors, context);
            YearMonth endMonth = requireYearMonth(table, "end_month", errors, context);
            if (startMonth != null && endMonth != null && startMonth.isAfter(endMonth)) {
                errors.add(context + ": start_month " + startMonth + " must not be after end_month " + endMonth);
            }
            Double hoursTotal = requireNumber(table, "hours_total", errors, context);
            List<String> skillsNeeded = requireStringArray(table, "skills_needed", errors, context);
            String responsible = requireString(table, "responsible", errors, context);

            if (id != null && name != null && startMonth != null && endMonth != null
                && hoursTotal != null && skillsNeeded != null && responsible != null) {
                projects.add(new Project(id, name, startMonth, endMonth, hoursTotal, skillsNeeded, responsible));
            }

            if (id != null) {
                readAssignments(table, id, memberIds, context, assignments, errors);
            }
        }
        return new ProjectsAndAssignments(projects, assignments);
    }

    private static void readAssignments(TomlTable projectTable, String projectId, Set<String> memberIds,
                                          String projectContext, List<Assignment> assignments, List<String> errors) {
        TomlTable assignmentsTable = projectTable.getTable("assignments");
        if (assignmentsTable == null) {
            return;
        }

        for (String monthKey : assignmentsTable.keySet()) {
            String monthContext = projectContext + " assignments['" + monthKey + "']";
            YearMonth month;
            try {
                month = YearMonth.parse(monthKey);
            } catch (DateTimeParseException e) {
                errors.add(monthContext + ": key must be in YYYY-MM format");
                continue;
            }

            Object rawEntries = assignmentsTable.get(monthKey);
            if (!(rawEntries instanceof TomlArray entries)) {
                errors.add(monthContext + ": must be an array of { member, percentage } tables");
                continue;
            }

            for (int j = 0; j < entries.size(); j++) {
                String entryContext = monthContext + " entry #" + (j + 1);
                Object element = entries.get(j);
                if (!(element instanceof TomlTable entryTable)) {
                    errors.add(entryContext + ": must be a table with 'member' and 'percentage'");
                    continue;
                }

                String member = requireString(entryTable, "member", errors, entryContext);
                Double percentage = requireNumber(entryTable, "percentage", errors, entryContext);
                if (member != null && !memberIds.contains(member)) {
                    errors.add(entryContext + ": member '" + member + "' does not match any team_members.toml id");
                }
                if (percentage != null && percentage < 0) {
                    errors.add(entryContext + ": percentage must not be negative, got " + percentage);
                }

                if (member != null && memberIds.contains(member) && percentage != null && percentage >= 0) {
                    assignments.add(new Assignment(member, projectId, month, percentage));
                }
            }
        }
    }

    private static String requireString(TomlTable table, String key, List<String> errors, String context) {
        Object value = table.get(key);
        if (value == null) {
            errors.add(context + ": missing required field '" + key + "'");
            return null;
        }
        if (!(value instanceof String s)) {
            errors.add(context + ": field '" + key + "' must be a string");
            return null;
        }
        if (s.isBlank()) {
            errors.add(context + ": field '" + key + "' must not be blank");
            return null;
        }
        return s;
    }

    private static Double requireNumber(TomlTable table, String key, List<String> errors, String context) {
        Object value = table.get(key);
        if (value == null) {
            errors.add(context + ": missing required field '" + key + "'");
            return null;
        }
        if (!(value instanceof Number n)) {
            errors.add(context + ": field '" + key + "' must be a number");
            return null;
        }
        return n.doubleValue();
    }

    private static List<String> requireStringArray(TomlTable table, String key, List<String> errors, String context) {
        Object value = table.get(key);
        if (value == null) {
            errors.add(context + ": missing required field '" + key + "'");
            return null;
        }
        if (!(value instanceof TomlArray array)) {
            errors.add(context + ": field '" + key + "' must be an array");
            return null;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Object element = array.get(i);
            if (!(element instanceof String s)) {
                errors.add(context + ": field '" + key + "' element #" + (i + 1) + " must be a string");
                return null;
            }
            result.add(s);
        }
        return result;
    }

    private static YearMonth requireYearMonth(TomlTable table, String key, List<String> errors, String context) {
        String raw = requireString(table, key, errors, context);
        if (raw == null) {
            return null;
        }
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            errors.add(context + ": field '" + key + "' must be in YYYY-MM format, got '" + raw + "'");
            return null;
        }
    }

    private record ProjectsAndAssignments(List<Project> projects, List<Assignment> assignments) {
    }
}
