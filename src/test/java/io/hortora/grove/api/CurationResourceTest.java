package io.hortora.grove.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CurationResourceTest {

    @Test
    void moveDomainEndpointRejectsMissingDomain() {
        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/curation/move/nonexistent/GE-test.md")
                .then()
                .statusCode(500);
    }

    @Test
    void moveDomainEndpointRejectsPathTraversal() {
        given()
                .contentType("application/json")
                .body("{\"targetDomain\": \"../../etc\"}")
                .when().post("/api/curation/move/jvm/GE-test.md")
                .then()
                .statusCode(500);
    }
}
