package org.resourceplanning.webeditor;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.resourceplanning.compute.CapacityCalculator;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.report.HtmlReportRenderer;

import java.time.YearMonth;

@Path("/reports")
public class ReportsResource {

    @Inject
    PlanningRepository repository;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String report() {
        PlanningData data = repository.load();
        CapacityCalculator calculator = new CapacityCalculator(data);
        YearMonth currentMonth = YearMonth.now();
        return HtmlReportRenderer.render(
            calculator.memberMonthSummaries(currentMonth),
            calculator.projectMonthSummaries(currentMonth),
            calculator.overallHeadcountGaps(currentMonth),
            calculator.projectHeadcountGaps(currentMonth));
    }
}
