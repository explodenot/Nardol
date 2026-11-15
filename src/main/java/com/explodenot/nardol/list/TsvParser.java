package com.explodenot.nardol.list;

import com.explodenot.nardol.model.Region;
import com.explodenot.nardol.model.RegionGroup;
import com.explodenot.nardol.model.Target;
import com.explodenot.nardol.model.Tier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TsvParser {
    private TsvParser() {}

    public static final int MAX_FILE_SIZE_BYTES = 512 * 1024; // 512 KB
    public static final int MAX_LINES = 5000;

    public record ParseResult(
        List<RegionGroup> groups,
        int totalLines,
        int validTargets,
        int invalidLines,
        List<String> errors
    ) {}

    public static ParseResult parse(String content) throws IOException {
        if (content.getBytes().length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Target list exceeds maximum size limit of 512 KB");
        }

        Map<String, List<Target>> regionMap = new LinkedHashMap<>();
        Map<String, Region> metadataMap = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        int lineCount = 0;
        int validCount = 0;
        int invalidCount = 0;

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (lineCount > MAX_LINES) {
                    throw new IllegalArgumentException("Target list exceeds maximum line limit of " + MAX_LINES);
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] cols = line.split("\t", -1);
                if (cols.length < 10) {
                    invalidCount++;
                    errors.add("Line " + lineCount + ": expected 10 tab-separated columns, found " + cols.length);
                    continue;
                }

                String regionId = cols[0].trim();
                String continent = cols[1].trim();
                String country = cols[2].trim();
                String city = cols[3].trim();
                String provider = cols[4].trim();
                String host = cols[5].trim();
                int port;
                double lat;
                double lon;
                try {
                    port = Integer.parseInt(cols[6].trim());
                    lat = Double.parseDouble(cols[7].trim());
                    lon = Double.parseDouble(cols[8].trim());
                } catch (NumberFormatException e) {
                    invalidCount++;
                    errors.add("Line " + lineCount + ": invalid numeric port or lat/lon: " + e.getMessage());
                    continue;
                }
                String tierStr = cols[9].trim();

                TargetValidator.ValidationResult v = TargetValidator.validate(
                    regionId, continent, country, city, provider, host, port, lat, lon, tierStr
                );

                if (!v.valid()) {
                    invalidCount++;
                    errors.add("Line " + lineCount + ": " + v.error());
                    continue;
                }

                Tier tier = Tier.fromString(tierStr);
                Target target = new Target(regionId, continent, country, city, provider, host, port, lat, lon, tier);

                regionMap.computeIfAbsent(regionId, k -> new ArrayList<>()).add(target);
                metadataMap.putIfAbsent(regionId, new Region(regionId, continent, country, city, lat, lon));
                validCount++;
            }
        }

        List<RegionGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Target>> entry : regionMap.entrySet()) {
            Region reg = metadataMap.get(entry.getKey());
            groups.add(new RegionGroup(reg, entry.getValue()));
        }

        return new ParseResult(groups, lineCount, validCount, invalidCount, errors);
    }
}
