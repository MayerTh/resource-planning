package org.resourceplanning.domain;

import java.util.List;

public record TeamMember(
    String id,
    String firstName,
    String lastName,
    String team,
    String role,
    double hoursDaily,
    PaySchema paySchema,
    List<String> skills
) {
}
