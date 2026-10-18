package org.resourceplanning.report;

import org.resourceplanning.compute.MemberMonthSummary;
import org.resourceplanning.compute.MonthlyHeadcountGap;
import org.resourceplanning.compute.ProjectHeadcountGap;
import org.resourceplanning.compute.ProjectMonthSummary;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HtmlReportRenderer {

    private HtmlReportRenderer() {
    }

    public static String render(List<MemberMonthSummary> memberSummaries, List<ProjectMonthSummary> projectSummaries,
                                  List<MonthlyHeadcountGap> overallHeadcountGaps, List<ProjectHeadcountGap> projectHeadcountGaps) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <!doctype html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <title>Resource planning report</title>
            <style>
              body { font-family: sans-serif; margin: 2rem; }
              table { border-collapse: collapse; margin-bottom: 2rem; }
              th, td { border: 1px solid #ccc; padding: 0.3rem 0.6rem; text-align: left; }
              th { cursor: pointer; background: #f0f0f0; }
              tr.over-allocated, tr.short-staffed { background: #fde2e2; }
              .chart { margin-bottom: 2rem; }
              .chart svg { font-family: sans-serif; }
              .heatmap td { text-align: center; color: #fff; font-weight: 600; }
            </style>
            </head>
            <body>
            """);

        html.append("<h2>Headcount gap (this month onward)</h2>\n");
        html.append("<p>Estimated heads needed to cover all active projects' <code>hours_total</code> budget, "
            + "spread evenly across each project's own duration, versus the current team's total capacity. "
            + "A rough estimate, not an exact figure — see ADR 0021.</p>\n");
        appendHeadcountGapChart(html, overallHeadcountGaps);

        html.append("<h2>Project headcount gap</h2>\n");
        appendProjectHeadcountGapTable(html, projectHeadcountGaps);

        html.append("<h2>Member utilization</h2>\n");
        appendUtilizationHeatmap(html, memberSummaries);

        html.append("<h2>Member capacity</h2>\n");
        appendMemberTable(html, memberSummaries);

        html.append("<h2>Project skill coverage</h2>\n");
        appendProjectTable(html, projectSummaries);

        html.append("""
            <script>
              function sortTable(table, column) {
                const rows = Array.from(table.tBodies[0].rows);
                const ascending = table.dataset.sortCol == column && table.dataset.sortDir != 'asc';
                rows.sort((a, b) => a.cells[column].innerText.localeCompare(b.cells[column].innerText, undefined, {numeric: true}));
                if (!ascending) rows.reverse();
                rows.forEach(row => table.tBodies[0].appendChild(row));
                table.dataset.sortCol = column;
                table.dataset.sortDir = ascending ? 'asc' : 'desc';
              }
              document.querySelectorAll('table.sortable th').forEach((th, index) => {
                th.addEventListener('click', () => sortTable(th.closest('table'), index));
              });
            </script>
            </body>
            </html>
            """);
        return html.toString();
    }

    private static void appendMemberTable(StringBuilder html, List<MemberMonthSummary> summaries) {
        List<MemberMonthSummary> sorted = summaries.stream()
            .sorted(Comparator.comparing((MemberMonthSummary s) -> s.member().id()).thenComparing(MemberMonthSummary::month))
            .toList();
        html.append("<table class=\"sortable\">\n<thead><tr><th>Member</th><th>Month</th><th>Assigned %</th>"
            + "<th>Spare %</th><th>Over-allocated</th><th>Skills</th></tr></thead>\n<tbody>\n");
        for (MemberMonthSummary s : sorted) {
            html.append("<tr class=\"").append(s.overAllocated() ? "over-allocated" : "").append("\">")
                .append("<td>").append(escape(s.member().id())).append("</td>")
                .append("<td>").append(s.month()).append("</td>")
                .append("<td>").append(String.format("%.0f", s.assignedPercentage())).append("</td>")
                .append("<td>").append(String.format("%.0f", s.sparePercentage())).append("</td>")
                .append("<td>").append(s.overAllocated() ? "yes" : "no").append("</td>")
                .append("<td>").append(escape(String.join(", ", s.member().skills()))).append("</td>")
                .append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");
    }

    private static void appendProjectTable(StringBuilder html, List<ProjectMonthSummary> summaries) {
        List<ProjectMonthSummary> sorted = summaries.stream()
            .sorted(Comparator.comparing((ProjectMonthSummary s) -> s.project().id()).thenComparing(ProjectMonthSummary::month))
            .toList();
        html.append("<table class=\"sortable\">\n<thead><tr><th>Project</th><th>Month</th>"
            + "<th>Covered skills</th><th>Missing skills</th></tr></thead>\n<tbody>\n");
        for (ProjectMonthSummary s : sorted) {
            html.append("<tr>")
                .append("<td>").append(escape(s.project().id())).append("</td>")
                .append("<td>").append(s.month()).append("</td>")
                .append("<td>").append(escape(String.join(", ", s.coveredSkills()))).append("</td>")
                .append("<td>").append(escape(String.join(", ", s.uncoveredSkills()))).append("</td>")
                .append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");
    }

    private static void appendHeadcountGapChart(StringBuilder html, List<MonthlyHeadcountGap> gaps) {
        if (gaps.isEmpty()) {
            html.append("<p>No upcoming project months to compute a headcount gap for.</p>\n");
            return;
        }
        List<MonthlyHeadcountGap> sorted = gaps.stream().sorted(Comparator.comparing(MonthlyHeadcountGap::month)).toList();
        double maxAbs = sorted.stream().mapToDouble(g -> Math.abs(g.gapHeadcount())).max().orElse(0.0);
        if (maxAbs == 0.0) {
            maxAbs = 1.0;
        }

        int barWidth = 70;
        int barGap = 10;
        int halfHeight = 100;
        int width = sorted.size() * (barWidth + barGap) + barGap;
        int height = halfHeight * 2 + 40;
        int baselineY = halfHeight + 10;

        html.append("<div class=\"chart\"><svg width=\"").append(width).append("\" height=\"").append(height).append("\">\n")
            .append("<line x1=\"0\" y1=\"").append(baselineY).append("\" x2=\"").append(width).append("\" y2=\"")
            .append(baselineY).append("\" stroke=\"#999\" />\n");

        int x = barGap;
        for (MonthlyHeadcountGap g : sorted) {
            double ratio = g.gapHeadcount() / maxAbs;
            int barHeight = Math.max(1, (int) Math.round(Math.abs(ratio) * (halfHeight - 10)));
            boolean shortage = g.gapHeadcount() > 0;
            String color = g.gapHeadcount() == 0 ? "#999" : (shortage ? "#d9534f" : "#5cb85c");
            int barY = shortage || g.gapHeadcount() == 0 ? baselineY - barHeight : baselineY;

            html.append("<rect x=\"").append(x).append("\" y=\"").append(barY).append("\" width=\"").append(barWidth)
                .append("\" height=\"").append(barHeight).append("\" fill=\"").append(color).append("\" />\n")
                .append("<text x=\"").append(x + barWidth / 2.0).append("\" y=\"")
                .append(shortage || g.gapHeadcount() == 0 ? barY - 4 : barY + barHeight + 14)
                .append("\" text-anchor=\"middle\" font-size=\"12\">")
                .append(String.format("%+.1f", g.gapHeadcount())).append("</text>\n")
                .append("<text x=\"").append(x + barWidth / 2.0).append("\" y=\"").append(height - 6)
                .append("\" text-anchor=\"middle\" font-size=\"12\">").append(g.month()).append("</text>\n");
            x += barWidth + barGap;
        }
        html.append("</svg></div>\n");
    }

    private static void appendProjectHeadcountGapTable(StringBuilder html, List<ProjectHeadcountGap> gaps) {
        List<ProjectHeadcountGap> sorted = gaps.stream()
            .sorted(Comparator.comparing((ProjectHeadcountGap g) -> g.project().id()).thenComparing(ProjectHeadcountGap::month))
            .toList();
        html.append("<table class=\"sortable\">\n<thead><tr><th>Project</th><th>Month</th><th>Demand (h)</th>"
            + "<th>Assigned (h)</th><th>Gap (heads)</th></tr></thead>\n<tbody>\n");
        for (ProjectHeadcountGap g : sorted) {
            boolean shortStaffed = g.gapHeadcount() > 0.05;
            html.append("<tr class=\"").append(shortStaffed ? "short-staffed" : "").append("\">")
                .append("<td>").append(escape(g.project().id())).append("</td>")
                .append("<td>").append(g.month()).append("</td>")
                .append("<td>").append(String.format("%.0f", g.demandHours())).append("</td>")
                .append("<td>").append(String.format("%.0f", g.assignedHours())).append("</td>")
                .append("<td>").append(String.format("%+.1f", g.gapHeadcount())).append("</td>")
                .append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");
    }

    private static void appendUtilizationHeatmap(StringBuilder html, List<MemberMonthSummary> summaries) {
        if (summaries.isEmpty()) {
            html.append("<p>No upcoming assignment data to show utilization for.</p>\n");
            return;
        }
        List<String> memberIds = summaries.stream().map(s -> s.member().id()).distinct().sorted().toList();
        List<YearMonth> months = summaries.stream().map(MemberMonthSummary::month).distinct().sorted().toList();
        Map<String, MemberMonthSummary> byKey = new HashMap<>();
        for (MemberMonthSummary s : summaries) {
            byKey.put(s.member().id() + "|" + s.month(), s);
        }

        html.append("<table class=\"heatmap\">\n<thead><tr><th>Member</th>");
        for (YearMonth month : months) {
            html.append("<th>").append(month).append("</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");
        for (String memberId : memberIds) {
            html.append("<tr><td>").append(escape(memberId)).append("</td>");
            for (YearMonth month : months) {
                MemberMonthSummary s = byKey.get(memberId + "|" + month);
                double assigned = s == null ? 0.0 : s.assignedPercentage();
                html.append("<td style=\"background-color:").append(heatColor(assigned)).append(";\">")
                    .append(String.format("%.0f", assigned)).append("%</td>");
            }
            html.append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");
    }

    private static String heatColor(double assignedPercentage) {
        if (assignedPercentage > 100.0) {
            return "#7b0000";
        }
        double ratio = Math.max(0.0, Math.min(1.0, assignedPercentage / 100.0));
        double hue = ratio * 120.0;
        return String.format("hsl(%.0f, 70%%, 50%%)", hue);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
