package io.hortora.grove.health;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ReindexServiceTest {

    @Inject
    ReindexService reindexService;

    @Test
    void reindexEndpointReturnsResult() {
        given()
                .when().post("/api/reindex")
                .then()
                .statusCode(anyOf(is(200), is(502)))
                .body("status", anyOf(is("ok"), is("error")));
    }

    @Test
    void reindexServiceHandlesUnreachableEngine() {
        var result = reindexService.triggerReindex();
        assertNotNull(result);
        assertNotNull(result.status());
    }
}
