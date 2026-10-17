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
import org.resourceplanning.domain.PaySchema;
import org.resourceplanning.domain.PlanningData;
import org.resourceplanning.domain.TeamMember;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Path("/members")
public class MembersResource {

    @Inject
    PlanningRepository repository;

    @Inject
    @Location("members/list.html")
    Template listTemplate;

    @Inject
    @Location("members/edit.html")
    Template editTemplate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return listTemplate.data("members", repository.load().teamMembers());
    }

    @GET
    @Path("/{id}/edit")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@PathParam("id") String id) {
        TeamMember member = findMember(id);
        return editTemplate.data("member", member).data("skillsText", String.join(", ", member.skills()));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(@FormParam("id") String id, @FormParam("firstName") String firstName,
                            @FormParam("lastName") String lastName, @FormParam("team") String team,
                            @FormParam("role") String role, @FormParam("hoursDaily") double hoursDaily,
                            @FormParam("paySchema") String paySchemaCode, @FormParam("skills") String skills) {
        PlanningData data = repository.load();
        List<TeamMember> members = new ArrayList<>(data.teamMembers());
        members.add(new TeamMember(id, firstName, lastName, team, role, hoursDaily,
            PaySchema.fromCode(paySchemaCode).orElseThrow(), parseSkills(skills)));
        repository.saveTeamMembers(members);
        return redirectTo("/members");
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@PathParam("id") String id, @FormParam("firstName") String firstName,
                            @FormParam("lastName") String lastName, @FormParam("team") String team,
                            @FormParam("role") String role, @FormParam("hoursDaily") double hoursDaily,
                            @FormParam("paySchema") String paySchemaCode, @FormParam("skills") String skills) {
        PlanningData data = repository.load();
        List<TeamMember> members = new ArrayList<>();
        for (TeamMember existing : data.teamMembers()) {
            if (existing.id().equals(id)) {
                members.add(new TeamMember(id, firstName, lastName, team, role, hoursDaily,
                    PaySchema.fromCode(paySchemaCode).orElseThrow(), parseSkills(skills)));
            } else {
                members.add(existing);
            }
        }
        repository.saveTeamMembers(members);
        return redirectTo("/members");
    }

    @POST
    @Path("/{id}/delete")
    public Response delete(@PathParam("id") String id) {
        PlanningData data = repository.load();
        List<TeamMember> members = new ArrayList<>(data.teamMembers());
        members.removeIf(member -> member.id().equals(id));
        repository.saveTeamMembers(members);
        return redirectTo("/members");
    }

    private TeamMember findMember(String id) {
        return repository.load().teamMembers().stream()
            .filter(member -> member.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("No team member with id '" + id + "'"));
    }

    private static List<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skills.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static Response redirectTo(String path) {
        return Response.seeOther(URI.create(path)).build();
    }
}
