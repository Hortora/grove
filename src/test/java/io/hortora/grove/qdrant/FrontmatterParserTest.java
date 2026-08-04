package io.hortora.grove.qdrant;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrontmatterParserTest {

    static final String REAL_ENTRY = """
            ---
            id: GE-20260516-3a27dc
            garden: discovery
            title: "Maven surefire profile without combine.self"
            type: gotcha
            domain: jvm
            stack: "Maven, maven-surefire-plugin 3.x"
            tags: [maven, surefire, profiles, junit5, tags]
            score: 13
            verified: true
            staleness_threshold: 730
            verified_on: "maven-surefire-plugin: 3.5.4"
            submitted: 2026-05-16
            author: mdp
            constraints: "Only occurs when the profile does NOT use combine.self"
            invalidation_triggers: "Revisit if maven-surefire-plugin changes semantics"
            ---

            ## Symptom

            Adding a Maven profile that selects tests produces unexpected results.
            """;

    @Test
    void parsesAllFrontmatterFields() {
        Map<String, Object> fm = FrontmatterParser.parse(REAL_ENTRY);

        assertEquals("GE-20260516-3a27dc", fm.get("id"));
        assertEquals("gotcha", fm.get("type"));
        assertEquals("jvm", fm.get("domain"));
        assertEquals(13, fm.get("score"));
        assertEquals(true, fm.get("verified"));
        assertEquals(730, fm.get("staleness_threshold"));
        assertEquals("mdp", fm.get("author"));
        assertEquals("2026-05-16", fm.get("submitted"));
    }

    @Test
    void parsesQuotedValues() {
        Map<String, Object> fm = FrontmatterParser.parse(REAL_ENTRY);

        assertEquals("Maven surefire profile without combine.self", fm.get("title"));
        assertEquals("Maven, maven-surefire-plugin 3.x", fm.get("stack"));
        assertEquals("maven-surefire-plugin: 3.5.4", fm.get("verified_on"));
    }

    @Test
    void parsesTagsAsList() {
        Map<String, Object> fm = FrontmatterParser.parse(REAL_ENTRY);

        Object tags = fm.get("tags");
        assertInstanceOf(List.class, tags);
        @SuppressWarnings("unchecked")
        List<String> tagList = (List<String>) tags;
        assertEquals(List.of("maven", "surefire", "profiles", "junit5", "tags"), tagList);
    }

    @Test
    void parsesBooleanValues() {
        Map<String, Object> fm = FrontmatterParser.parse(REAL_ENTRY);
        assertEquals(true, fm.get("verified"));
    }

    @Test
    void returnsEmptyMapForNoFrontmatter() {
        Map<String, Object> fm = FrontmatterParser.parse("Just some content without frontmatter.");
        assertTrue(fm.isEmpty());
    }

    @Test
    void returnsEmptyMapForNull() {
        Map<String, Object> fm = FrontmatterParser.parse(null);
        assertTrue(fm.isEmpty());
    }

    @Test
    void returnsEmptyMapForIncompleteFrontmatter() {
        Map<String, Object> fm = FrontmatterParser.parse("---\nid: test\nno closing marker");
        assertTrue(fm.isEmpty());
    }

    @Test
    void bodyWithoutFrontmatterStripsYaml() {
        String body = FrontmatterParser.bodyWithoutFrontmatter(REAL_ENTRY);
        assertTrue(body.startsWith("## Symptom"));
        assertFalse(body.contains("---"));
    }

    @Test
    void bodyWithoutFrontmatterReturnsOriginalWhenNoFrontmatter() {
        String content = "Just plain content";
        assertEquals(content, FrontmatterParser.bodyWithoutFrontmatter(content));
    }

    @Test
    void parsesEmptyTagsList() {
        String entry = """
                ---
                id: test
                tags: []
                ---
                body
                """;
        Map<String, Object> fm = FrontmatterParser.parse(entry);
        assertInstanceOf(List.class, fm.get("tags"));
        assertTrue(((List<?>) fm.get("tags")).isEmpty());
    }
}
