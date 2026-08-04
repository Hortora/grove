package io.hortora.grove.health;

public record ReindexResult(String status, String message) {

    public static ReindexResult success(String message) {
        return new ReindexResult("ok", message);
    }

    public static ReindexResult error(String message) {
        return new ReindexResult("error", message);
    }
}