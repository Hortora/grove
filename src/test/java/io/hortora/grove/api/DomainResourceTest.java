package io.hortora.grove.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DomainResourceTest {

    @Test
    void domainsEndpointReturnsNonEmptyList() {
        given()
                .when().get("/api/domains")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0))
                .body("[0].domain", notNullValue())
                .body("[0].entryCount", greaterThan(0))
                .body("[0].typeBreakdown", notNullValue())
                .body("[0].averageScore", greaterThan(0.0f));
    }

    @Test
    void overviewEndpointReturnsTotals() {
        given()
                .when().get("/api/overview")
                .then()
                .statusCode(200)
                .body("totalEntries", greaterThan(0))
                .body("totalDomains", greaterThan(0));
    }

    @Test
    void healthEndpointStillWorks() {
        given()
                .when().get("/api/health")
                .then()
                .statusCode(200)
                .body("status", is("ok"));
    }
}
