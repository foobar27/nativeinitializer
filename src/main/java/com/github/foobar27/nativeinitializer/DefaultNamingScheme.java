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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DefaultNamingScheme implements NamingScheme {

    private final String libraryName;
    private final String relativePath;
    private final Set<Platform> supportedPlatforms = new HashSet<>();

    public DefaultNamingScheme(String libraryName) {
        this(libraryName, "lib/");
    }

    public DefaultNamingScheme(String libraryName, String relativePath) {
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
        this.relativePath = relativePath;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void addSupportedPlatform(String architecture, String osName) {
        supportedPlatforms.add(new Platform(architecture, osName));
    }

    public void removeSupportedPlatform(String architecture, String osName) {
        supportedPlatforms.remove(new Platform(architecture, osName));
    }

    @Override
    public String determineName() {
        String osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name").toLowerCase();
        Platform platform = new Platform(osArch, osName);
        if (!supportedPlatforms.contains(platform)) {
            throw new UnsupportedOperationException("Platform " + platform+ " not supported");
        }
        String result = platform.osName + "-" + platform.architecture;
        if (relativePath != null) {
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

}
