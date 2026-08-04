package io.hortora.grove.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class EntryResourceTest {

    @Test
    void getEntryByGeIdReturnsEnrichedDetail() {
        given()
                .when().get("/api/entries/GE-20260516-3a27dc")
                .then()
                .statusCode(200)
                .body("title", notNullValue())
                .body("domain", is("jvm"))
                .body("type", is("gotcha"))
                .body("sourceDocumentId", containsString("GE-20260516-3a27dc"))
                .body("tags", not(empty()))
                .body("stalenessThreshold", notNullValue())
                .body("author", is("mdp"))
                .body("verified", is(true))
                .body("stalenessStatus", anyOf(is("current"), is("aging"), is("stale")));
    }

    @Test
    void getEntryNotFoundReturns404() {
        given()
                .when().get("/api/entries/GE-NONEXISTENT-000000")
                .then()
                .statusCode(404);
    }

    @Test
    void getDomainEntriesReturnsFilteredList() {
        given()
                .when().get("/api/domains/jvm/entries")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0))
                .body("[0].domain", is("jvm"));
    }

    @Test
    void getDomainEntriesFiltersByType() {
        given()
                .queryParam("type", "technique")
                .when().get("/api/domains/jvm/entries")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0))
                .body("type", everyItem(is("technique")));
    }

    @Test
    void getDomainEntriesSortsByScore() {
        var response = given()
                .queryParam("sort", "score")
                .when().get("/api/domains/tools/entries")
                .then()
                .statusCode(200)
                .extract().jsonPath();

        var scores = response.getList("score", Double.class);
        for (int i = 1; i < scores.size(); i++) {
            assertTrue(scores.get(i - 1) >= scores.get(i),
                    "Expected descending score order but got " + scores.get(i - 1) + " before " + scores.get(i));
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
