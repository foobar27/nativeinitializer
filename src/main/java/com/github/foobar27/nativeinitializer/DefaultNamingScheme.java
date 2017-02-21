/*
 * Copyright (c) 2017 Sebastien Wagener.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sebastien Wagener - initial release
 */
package com.github.foobar27.nativeinitializer;

import java.util.Objects;

public class DefaultNamingScheme implements NamingScheme {

    private final String libraryName;
    private final String version;
    private final String relativePath;

    public DefaultNamingScheme(String libraryName, String version) {
        this(libraryName, version, "lib/");
    }

    public DefaultNamingScheme(String libraryName, String version, String relativePath) {
        if (libraryName == null) {
            throw new NullPointerException("libraryName is null");
        }
        if (libraryName.isEmpty()) {
            throw new IllegalArgumentException("libraryName is empty");
        }
        this.libraryName = libraryName;
        if (relativePath != null) {
            if (!relativePath.endsWith("/")) {
                relativePath = relativePath + "/";
            }
        }
        if (version == null) {
            throw new NullPointerException("version is null");
        }
        if (version.isEmpty()) {
            throw new IllegalArgumentException("version is empty");
        }
        this.version = version;
        this.relativePath = relativePath;
    }

    public String getLibraryName() {
        return libraryName;
    }

    @Override
    public String determineName(boolean includePath, boolean includeVersion) {
        String osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name").toLowerCase();
        Platform platform = new Platform(osArch, osName);
        String result = libraryName + "-" + platform.osName + "-" + platform.architecture;
        if (includeVersion) {
            result = result + "-" + version;
        }
        if (includePath && relativePath != null) {
            return relativePath + result;
        } else {
            return result;
        }
    }

    private static final class Platform {
        private final String architecture;
        private final String osName;

        Platform(String architecture, String osName) {
            if (architecture == null) {
                throw new NullPointerException("architecture is null");
            }
            if (osName == null) {
                throw new NullPointerException("osName is null");
            }
            this.architecture = architecture;
            this.osName = osName;
        }

        @Override
        public String toString() {
            return osName + ":" + architecture;
        }

        @Override
        public boolean equals(Object t) {
            if (!(t instanceof Platform)) {
                return false;
            }
            Platform that = (Platform) t;
            return this.architecture.equals(that.architecture)
                    && this.osName.equals(that.osName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(architecture, osName);
        }
    }

    public static void main(String[] args) {
        DefaultNamingScheme scheme = new DefaultNamingScheme("thelib", "0.4.2");
        System.out.println(scheme.determineName(false, false));
        System.out.println(scheme.determineName(false, true));
    }

}
