package com.explodenot.nardol.list;

import com.explodenot.nardol.model.Region;
import com.explodenot.nardol.model.RegionGroup;
import com.explodenot.nardol.model.Target;
import com.explodenot.nardol.model.Tier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonListParser {
    private JsonListParser() {}

    private static final Gson GSON = new Gson();

    private static class JsonTarget {
        String region_id;
        String continent;
        String country;
        String city;
        String provider;
        String host;
        int port;
        double lat;
        double lon;
        String tier;
    }

    public static TsvParser.ParseResult parse(String content) throws IOException {
        if (content.getBytes().length > TsvParser.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Target list exceeds maximum size limit of 512 KB");
        }

        Type listType = new TypeToken<List<JsonTarget>>() {}.getType();
        List<JsonTarget> items = GSON.fromJson(content, listType);

        if (items == null) {
            return new TsvParser.ParseResult(List.of(), 0, 0, 0, List.of("Empty JSON list"));
        }

        Map<String, List<Target>> regionMap = new LinkedHashMap<>();
        Map<String, Region> metadataMap = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        int validCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < items.size(); i++) {
            JsonTarget item = items.get(i);
            TargetValidator.ValidationResult v = TargetValidator.validate(
                item.region_id, item.continent, item.country, item.city, item.provider,
                item.host, item.port, item.lat, item.lon, item.tier
            );
            if (!v.valid()) {
                invalidCount++;
                errors.add("Item " + (i + 1) + ": " + v.error());
                continue;
            }

            Tier tier = Tier.fromString(item.tier);
            Target target = new Target(
                item.region_id, item.continent, item.country, item.city, item.provider,
                item.host, item.port, item.lat, item.lon, tier
            );
            regionMap.computeIfAbsent(item.region_id, k -> new ArrayList<>()).add(target);
            metadataMap.putIfAbsent(item.region_id, new Region(item.region_id, item.continent, item.country, item.city, item.lat, item.lon));
            validCount++;
        }

        List<RegionGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Target>> entry : regionMap.entrySet()) {
            Region reg = metadataMap.get(entry.getKey());
            groups.add(new RegionGroup(reg, entry.getValue()));
        }

        return new TsvParser.ParseResult(groups, items.size(), validCount, invalidCount, errors);
    }
}
