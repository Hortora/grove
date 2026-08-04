package io.hortora.grove.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

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

    @Test
    void bulkConfirmEndpointRejectsEmptyList() {
        given()
                .contentType("application/json")
                .body("{\"entries\": []}")
                .when().post("/api/curation/bulk/confirm")
                .then()
                .statusCode(500);
    }

    @Test
    void bulkRetireEndpointRejectsEmptyList() {
        given()
                .contentType("application/json")
                .body("{\"entries\": [], \"reason\": \"test\"}")
                .when().post("/api/curation/bulk/retire")
                .then()
                .statusCode(500);
    }

    @Test
    void bulkRetagEndpointRejectsEmptyList() {
        given()
                .contentType("application/json")
                .body("{\"entries\": [], \"addTags\": [\"test\"]}")
                .when().post("/api/curation/bulk/retag")
                .then()
                .statusCode(500);
    }
}
