package com.explodenot.nardol.render;

import com.explodenot.nardol.model.RegionResult;
import com.explodenot.nardol.model.RunReport;
import com.google.gson.*;

import java.io.PrintStream;
import java.lang.reflect.Type;
import java.time.Instant;

public final class JsonOut {
    private JsonOut() {}

    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) -> Instant.parse(json.getAsString()))
        .setPrettyPrinting()
        .create();

    public static final Gson COMPACT_GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) -> Instant.parse(json.getAsString()))
        .create();

    public static void render(RunReport report, PrintStream out, boolean ndjson) {
        if (ndjson) {
            for (RegionResult r : report.results()) {
                out.println(COMPACT_GSON.toJson(r));
            }
        } else {
            out.println(GSON.toJson(report));
        }
    }
}
