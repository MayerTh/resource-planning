package org.resourceplanning.webeditor;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class WebEditorPagesTest {

    @Test
    void homePageLinksToMembersAndProjects() {
        given().when().get("/")
            .then().statusCode(200)
            .body(containsString("Team members"), containsString("Projects"));
    }

    @Test
    void membersListShowsFixtureMemberAndAddForm() {
        given().when().get("/members")
            .then().statusCode(200)
            .body(containsString("anna.mayer"), containsString("Add team member"));
    }

    @Test
    void memberEditFormIsPrefilled() {
        given().when().get("/members/anna.mayer/edit")
            .then().statusCode(200)
            .body(containsString("Anna"), containsString("java, kotlin, sql"));
    }

    @Test
    void projectsListShowsFixtureProject() {
        given().when().get("/projects")
            .then().statusCode(200)
            .body(containsString("internal-tools-revamp"), containsString("Add project"));
    }

    @Test
    void projectDetailShowsAssignmentsAndMemberOptions() {
        given().when().get("/projects/internal-tools-revamp")
            .then().statusCode(200)
            .body(containsString("2026-01"), containsString("anna.mayer"));
    }
}
