package org.resourceplanning.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.resourceplanning.io.TomlDataLoaderTest.fixture;

class TomlDataLoaderValidationTest {

    @Test
    void collectsAllValidationErrorsAcrossAllThreeFiles() throws Exception {
        DataValidationException exception = assertThrows(DataValidationException.class, () ->
            TomlDataLoader.load(
                fixture("fixtures/invalid/team_members.toml"),
                fixture("fixtures/invalid/planning.toml"),
                fixture("fixtures/invalid/working_days.toml")
            ));

        // team_members.toml: missing role, missing hours_daily, invalid pay_schema, duplicate id (4)
        // planning.toml: start_month after end_month, missing responsible, unknown member reference (3)
        // working_days.toml: month out of range, duplicate (year, month) entry (2)
        assertEquals(9, exception.errors().size());

        String combined = String.join("\n", exception.errors());
        assertTrue(combined.contains("missing required field 'role'"));
        assertTrue(combined.contains("invalid pay_schema 'd9'"));
        assertTrue(combined.contains("duplicate id 'anna.mayer'"));
        assertTrue(combined.contains("must not be after end_month"));
        assertTrue(combined.contains("missing required field 'responsible'"));
        assertTrue(combined.contains("member 'ghost.person' does not match any team_members.toml id"));
        assertTrue(combined.contains("month must be between 1 and 12"));
        assertTrue(combined.contains("duplicate entry for 2026-01"));
    }
}
