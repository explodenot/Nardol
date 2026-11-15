package com.explodenot.nardol.cli;

import java.io.InputStream;
import java.util.Properties;

public final class VersionProvider implements picocli.CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String ver = "2.1.0";
        try (InputStream in = getClass().getResourceAsStream("/version.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                ver = props.getProperty("version", ver);
            }
        } catch (Exception ignored) {}
        return new String[]{ "Nardol v" + ver };
    }
}
