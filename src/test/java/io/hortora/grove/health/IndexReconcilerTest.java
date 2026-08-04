package io.hortora.grove.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class IndexReconcilerTest {

    @Test
    void reconcileEndpointReturnsCounts() {
        given()
                .when().get("/api/health/reconcile")
                .then()
                .statusCode(200)
                .body("qdrantCount", greaterThan(0))
                .body("fileCount", greaterThan(0))
                .body("gardenDbCount", greaterThan(0));
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
