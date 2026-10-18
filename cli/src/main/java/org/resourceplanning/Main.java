package org.resourceplanning;

import org.resourceplanning.compute.CapacityCalculator;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.io.DataValidationException;
import org.resourceplanning.io.TomlDataLoader;
import org.resourceplanning.report.HtmlReportRenderer;
import org.resourceplanning.report.TerminalReportRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

public class Main {

    public static void main(String[] args) {
        Path dataDir = Path.of(System.getenv().getOrDefault("RESOURCEPLANNING_DATA_DIR", "data"));
        Path htmlOutput = Path.of(args.length > 0 ? args[0] : "report.html");

        PlanningData data;
        try {
            data = TomlDataLoader.load(
                dataDir.resolve("team_members.toml"),
                dataDir.resolve("planning.toml"),
                dataDir.resolve("working_days.toml"));
        } catch (DataValidationException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("Could not read data files: " + e.getMessage());
            System.exit(1);
            return;
        }

        YearMonth currentMonth = YearMonth.now();
        CapacityCalculator calculator = new CapacityCalculator(data);
        var memberSummaries = calculator.memberMonthSummaries(currentMonth);
        var projectSummaries = calculator.projectMonthSummaries(currentMonth);
        var overallHeadcountGaps = calculator.overallHeadcountGaps(currentMonth);
        var projectHeadcountGaps = calculator.projectHeadcountGaps(currentMonth);

        System.out.print(TerminalReportRenderer.render(memberSummaries, projectSummaries));

        try {
            Files.writeString(htmlOutput,
                HtmlReportRenderer.render(memberSummaries, projectSummaries, overallHeadcountGaps, projectHeadcountGaps));
            System.out.println("\nHTML report written to " + htmlOutput.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not write HTML report: " + e.getMessage());
            System.exit(1);
        }
    }
}
