// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.jetbrains.cef;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full parsed JCEF version<br>
 * Example: {@link CefVersion#major 81}.{@link CefVersion#api 2}.{@link CefVersion#patch 24}-g{@link CefVersion#commitHash c0b313d}-chromium-{@link ChromiumVersion#major 81}.{@link ChromiumVersion#minor 0}.{@link ChromiumVersion#build 4044}.{@link ChromiumVersion#patch 113}-api-{@link ApiVersion#major 1}.{@link ApiVersion#minor 1}
 * @author Nikita Gubarkov
 */
public final class JCefVersionDetails {

    public static final class VersionUnavailableException extends Exception {
        VersionUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
        VersionUnavailableException(String message) {
            super(message);
        }
    }

    /**
     * CEF version details
     * @see <a href=
     * "https://bitbucket.org/chromiumembedded/cef/wiki/BranchesAndBuilding#markdown-header-version-number-format">
     * CEF Version Number Format</a>
     */
    public static final class CefVersion {
        /**
         * Chromium major version
         */
        public final int major;
        /**
         * Native CEF API version
         */
        public final int api;
        public final int patch;
        public final String commitHash;

        CefVersion(int major, int api, int patch, String commitHash) {
            this.major = major;
            this.api = api;
            this.patch = patch;
            this.commitHash = commitHash;
        }
    }

    /**
     * Chromium version details
     * @see <a href="https://www.chromium.org/developers/version-numbers">Chromium Version Number Format</a>
     */
    public static final class ChromiumVersion {
        public final int major, minor, build, patch;

        ChromiumVersion(int major, int minor, int build, int patch) {
            this.major = major;
            this.minor = minor;
            this.build = build;
            this.patch = patch;
        }
    }

    /**
     * Public JCEF API version in form {@link #major [major]}.{@link #minor [minor]}
     */
    public static final class ApiVersion {
        /**
         * Major JCEF API version, incremented when non-backward compatible API changes are made
         */
        public final int major;
        /**
         * Minor JCEF API version, incremented when backward compatible API changes are made (API is extended)
         */
        public final int minor;

        ApiVersion(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        /**
         * Checks if current JCEF API version is compatible with requested (target) API version
         */
        public boolean isCompatible(int targetMajor, int targetMinor) {
            return major == targetMajor && minor >= targetMinor;
        }
    }


    private static final Pattern PATTERN = Pattern.compile(
            "#.#.#-g([0-9a-f]{7})-chromium-#.#.#.#-api-#.#"
                    .replaceAll("\\.", "\\\\.").replaceAll("#", "(\\\\d+)"));

    public final CefVersion cefVersion;
    public final ChromiumVersion chromiumVersion;
    public final ApiVersion apiVersion;
    private final String stringValue;

    JCefVersionDetails(String stringValue) throws VersionUnavailableException {
        this.stringValue = stringValue;
        Matcher matcher = PATTERN.matcher(stringValue);
        if(!matcher.matches()) throw new VersionUnavailableException("JCEF version has wrong format: " + stringValue);
        try {
            cefVersion = new CefVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    matcher.group(4)
            );
            chromiumVersion = new ChromiumVersion(
                    Integer.parseInt(matcher.group(5)),
                    Integer.parseInt(matcher.group(6)),
                    Integer.parseInt(matcher.group(7)),
                    Integer.parseInt(matcher.group(8))
            );
            apiVersion = new ApiVersion(
                    Integer.parseInt(matcher.group(9)),
                    Integer.parseInt(matcher.group(10))
            );
        } catch(Exception e) {
            throw new VersionUnavailableException("Unable to parse JCEF version: " + stringValue, e);
        }
    }

    @Override
    public String toString() {
        return stringValue;
    }

}
