package com.explodenot.gli.list;

import com.explodenot.gli.model.Target;
import com.explodenot.gli.model.Tier;

import java.util.regex.Pattern;

public final class TargetValidator {
    private TargetValidator() {}

    private static final Pattern REGION_ID_PATTERN = Pattern.compile("^[a-z0-9_-]{2,32}$");
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]{3,253}$");

    public record ValidationResult(boolean valid, String error) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }
        public static ValidationResult fail(String error) {
            return new ValidationResult(false, error);
        }
    }

    public static ValidationResult validate(
        String regionId,
        String continent,
        String country,
        String city,
        String provider,
        String host,
        int port,
        double lat,
        double lon,
        String tier
    ) {
        if (regionId == null || !REGION_ID_PATTERN.matcher(regionId.trim()).matches()) {
            return ValidationResult.fail("Invalid region_id: " + regionId);
        }
        if (continent == null || continent.isBlank() || continent.length() > 64) {
            return ValidationResult.fail("Invalid continent: " + continent);
        }
        if (country == null || country.isBlank() || country.length() > 64) {
            return ValidationResult.fail("Invalid country: " + country);
        }
        if (city == null || city.isBlank() || city.length() > 64) {
            return ValidationResult.fail("Invalid city: " + city);
        }
        if (provider == null || provider.isBlank() || provider.length() > 64) {
            return ValidationResult.fail("Invalid provider: " + provider);
        }
        if (host == null || !HOSTNAME_PATTERN.matcher(host.trim()).matches()) {
            return ValidationResult.fail("Invalid host: " + host);
        }
        if (port < 1 || port > 65535) {
            return ValidationResult.fail("Invalid port: " + port);
        }
        if (lat < -90.0 || lat > 90.0) {
            return ValidationResult.fail("Invalid latitude: " + lat);
        }
        if (lon < -180.0 || lon > 180.0) {
            return ValidationResult.fail("Invalid longitude: " + lon);
        }
        try {
            Tier.fromString(tier);
        } catch (IllegalArgumentException e) {
            return ValidationResult.fail(e.getMessage());
        }

        // Host cannot contain suspicious path / control chars
        if (host.contains("/") || host.contains("\\") || host.contains("\n") || host.contains("\r")) {
            return ValidationResult.fail("Host contains invalid characters: " + host);
        }

        return ValidationResult.ok();
    }
}
