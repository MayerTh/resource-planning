package org.resourceplanning.report;

import org.resourceplanning.compute.MemberMonthSummary;
import org.resourceplanning.compute.ProjectMonthSummary;

import java.util.Comparator;
import java.util.List;

public final class HtmlReportRenderer {

    private HtmlReportRenderer() {
    }

    public static String render(List<MemberMonthSummary> memberSummaries, List<ProjectMonthSummary> projectSummaries) {
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
              tr.over-allocated { background: #fde2e2; }
            </style>
            </head>
            <body>
            """);

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

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
