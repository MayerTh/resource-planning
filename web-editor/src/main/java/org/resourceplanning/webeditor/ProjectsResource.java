package org.resourceplanning.webeditor;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.resourceplanning.domain.Assignment;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.Project;

import java.net.URI;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Path("/projects")
public class ProjectsResource {

    @Inject
    PlanningRepository repository;

    @Inject
    @Location("projects/list.html")
    Template listTemplate;

    @Inject
    @Location("projects/edit.html")
    Template editTemplate;

    @Inject
    @Location("projects/detail.html")
    Template detailTemplate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return listTemplate.data("projects", repository.load().projects());
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") String id) {
        PlanningData data = repository.load();
        Project project = findProject(data, id);
        List<Assignment> assignments = data.assignments().stream()
            .filter(a -> a.project().equals(id))
            .sorted(java.util.Comparator.comparing(Assignment::month).thenComparing(Assignment::member))
            .toList();
        return detailTemplate.data("project", project)
            .data("assignments", assignments)
            .data("members", data.teamMembers());
    }

    @GET
    @Path("/{id}/edit")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@PathParam("id") String id) {
        Project project = findProject(repository.load(), id);
        return editTemplate.data("project", project).data("skillsText", String.join(", ", project.skillsNeeded()));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(@FormParam("id") String id, @FormParam("name") String name,
                            @FormParam("startMonth") String startMonth, @FormParam("endMonth") String endMonth,
                            @FormParam("hoursTotal") double hoursTotal, @FormParam("skillsNeeded") String skillsNeeded,
                            @FormParam("responsible") String responsible) {
        PlanningData data = repository.load();
        List<Project> projects = new ArrayList<>(data.projects());
        projects.add(new Project(id, name, YearMonth.parse(startMonth), YearMonth.parse(endMonth),
            hoursTotal, parseList(skillsNeeded), responsible));
        repository.savePlanning(projects, data.assignments());
        return redirectTo("/projects");
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@PathParam("id") String id, @FormParam("name") String name,
                            @FormParam("startMonth") String startMonth, @FormParam("endMonth") String endMonth,
                            @FormParam("hoursTotal") double hoursTotal, @FormParam("skillsNeeded") String skillsNeeded,
                            @FormParam("responsible") String responsible) {
        PlanningData data = repository.load();
        List<Project> projects = new ArrayList<>();
        for (Project existing : data.projects()) {
            if (existing.id().equals(id)) {
                projects.add(new Project(id, name, YearMonth.parse(startMonth), YearMonth.parse(endMonth),
                    hoursTotal, parseList(skillsNeeded), responsible));
            } else {
                projects.add(existing);
            }
        }
        repository.savePlanning(projects, data.assignments());
        return redirectTo("/projects");
    }

    @POST
    @Path("/{id}/delete")
    public Response delete(@PathParam("id") String id) {
        PlanningData data = repository.load();
        List<Project> projects = new ArrayList<>(data.projects());
        projects.removeIf(project -> project.id().equals(id));
        List<Assignment> assignments = data.assignments().stream()
            .filter(a -> !a.project().equals(id))
            .toList();
        repository.savePlanning(projects, assignments);
        return redirectTo("/projects");
    }

    @POST
    @Path("/{id}/assignments")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addAssignment(@PathParam("id") String id, @FormParam("month") String month,
                                   @FormParam("member") String member, @FormParam("percentage") double percentage) {
        PlanningData data = repository.load();
        List<Assignment> assignments = new ArrayList<>(data.assignments());
        assignments.add(new Assignment(member, id, YearMonth.parse(month), percentage));
        repository.savePlanning(data.projects(), assignments);
        return redirectTo("/projects/" + id);
    }

    @POST
    @Path("/{id}/assignments/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteAssignment(@PathParam("id") String id, @FormParam("month") String month,
                                      @FormParam("member") String member) {
        PlanningData data = repository.load();
        YearMonth targetMonth = YearMonth.parse(month);
        List<Assignment> assignments = data.assignments().stream()
            .filter(a -> !(a.project().equals(id) && a.month().equals(targetMonth) && a.member().equals(member)))
            .toList();
        repository.savePlanning(data.projects(), assignments);
        return redirectTo("/projects/" + id);
    }

    private static Project findProject(PlanningData data, String id) {
        return data.projects().stream()
            .filter(project -> project.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("No project with id '" + id + "'"));
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static Response redirectTo(String path) {
        return Response.seeOther(URI.create(path)).build();
    }
}
