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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class NativeLoaderFactory {

    private static final Logger logger = Logger.getLogger(NativeLoaderFactory.class.getName());

    private final NamingScheme namingScheme;

    public NativeLoaderFactory(NamingScheme namingScheme) {
        if (namingScheme == null) {
            throw new NullPointerException("namingScheme null");
        }
        this.namingScheme = namingScheme;
    }

    public NamingScheme getNamingScheme() {
        return namingScheme;
    }

    /**
     * Loads the library TODO via the system path.
     * You might need to adjust LD_LIBRARY_PATH or java.library.path.
     */
    public NativeLoader systemLoader() {
        return systemLoader(namingScheme.getLibraryName());
    }

    /**
     * Loads the library TODO via the system path.
     * You might need to adjust LD_LIBRARY_PATH or java.library.path.
     */
    public NativeLoader systemLoader(String libraryName) {
        return new NativeLoader() {

            @Override
            public String toString() {
                return String.format("systemLoader(%s)", libraryName);
            }

            @Override
            public void load() throws IOException {
                logger.info(String.format("Loading system library '%s'", libraryName));
                System.loadLibrary(libraryName);
            }
        };
    }

    public NativeLoader fileLoader(Path path) {
        return new NativeLoader() {

            @Override
            public String toString() {
                return String.format("fileLoader(%s)", path);
            }

            @Override
            public void load() throws IOException {
                logger.info(String.format("Loading library from file '%s'", path));
                System.load(path.toString());
            }
        };
    }

    private static Path createTempDirectory(String libraryName) throws IOException {
        Path result = Files.createTempDirectory(libraryName);
        result.toFile().deleteOnExit();
        return result;
    }

    public NativeLoader resourceLoaderTempDirectory() {
        return resourceLoaderTempDirectory(namingScheme.getLibraryName());
    }

    public NativeLoader resourceLoaderTempDirectory(String libraryName) {
        // Create the temporary directory only when NativeLoader called.
        return new NativeLoader() {
            @Override
            public String toString() {
                return String.format("resourceLoaderTempDirectory(%s)", libraryName);
            }

            @Override
            public void load() throws IOException {
                // No log line (else we have two subsequent log lines).
                resourceLoader(libraryName, createTempDirectory(libraryName), true).load();
            }
        };
    }

    public NativeLoader resourceLoaderFixedDirectory(Path tempDirectory) throws IOException {
        return resourceLoader(tempDirectory, false);
    }

    public NativeLoader resourceLoader(Path tempDirectory, boolean deleteOnExit) throws IOException {
        return resourceLoader(namingScheme.getLibraryName(), tempDirectory, deleteOnExit);
    }

    private NativeLoader resourceLoader(String libraryName, Path tempDirectory, boolean deleteOnExit) {
        return new NativeLoader() {
            @Override
            public String toString() {
                return String.format("resourceLoader(%s, %s, %b)",
                        libraryName,
                        tempDirectory,
                        deleteOnExit);
            }

            @Override
            public void load() throws IOException {
                logger.info(
                        String.format("Loading library '%s' file from resource via directory %s (deleteOnExit: %b)",
                                libraryName,
                                tempDirectory,
                                deleteOnExit));
                System.load(saveLibrary(tempDirectory, deleteOnExit).toString());
            }
        };
    }

    static final class FallbackLoader implements NativeLoader {

        private final NativeLoader firstLoader;
        private final NativeLoader secondLoader;

        FallbackLoader(NativeLoader firstLoader, NativeLoader secondLoader) {
            this.firstLoader = firstLoader;
            this.secondLoader = secondLoader;
        }

        @Override
        public void load() throws IOException {
            try {
                firstLoader.load();
            } catch (Exception e) {
                logger.log(
                        Level.INFO,
                        String.format("%s failed, trying %s",
                                firstLoader,
                                secondLoader),
                        e);
                secondLoader.load();
            }
        }
    }

    private static String toFile(InputStream in, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            int cnt;
            byte buf[] = new byte[16 * 1024];
            while ((cnt = in.read(buf)) >= 1) {
                out.write(buf, 0, cnt);
            }
            return file.getAbsolutePath();
        }
    }

    private Path saveLibrary(Path tmpDir, boolean deleteOnExit) throws IOException {
        String resourceName = namingScheme.determineName(true, false);
        logger.info(
                String.format("Extracting resource '%s' to '%s'%s",
                        resourceName,
                        tmpDir,
                        deleteOnExit ? " (will be deleted on exit)" : ""));
        if (!Files.exists(tmpDir)) {
            Files.createDirectory(tmpDir);
        }
        Path file = tmpDir.resolve(namingScheme.determineName(false, true));
        if (Files.exists(file)) {
            if (deleteOnExit) {
                // There might be a race condition that the file will be deleted in the short period
                // between this check and the subsequent loading
                // (especially when the a small program is executed again and again).
                // Let's extract it again. Since we extract it to a random file and atomically move it afterwards
                // there should not be a race condition.
                logger.info(
                        String.format(
                                "File '%s' already exists, overwriting to avoid race conditions",
                                file));
            } else {
                // In normal operations the file will not be deleted in the short period
                // between this check and the subsequent loading, no need to extract it again.
                // The speed improvement outweighs this risk.
                logger.info(
                        String.format("File '%s' already exists, no need to extract",
                                file));
                return file;
            }
        }

        // Avoid race conditions by making an atomic move from a random file.
        Path tmpFile = tmpDir.resolve(UUID.randomUUID().toString());
        logger.info(
                String.format(
                        "Extracting to temporary file '%s'",
                        tmpFile));
        try (InputStream in = NativeLoaderFactory
                .class
                .getClassLoader()
                .getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException(
                        String.format("Resource '%s' does not exist",
                                resourceName));
            }
            toFile(in, tmpFile.toFile());
        }
        logger.info(
                String.format(
                        "Moving temporary file '%s' -> '%s'",
                        tmpFile,
                        file));
        Files.move(tmpFile, file, ATOMIC_MOVE, REPLACE_EXISTING);
        if (deleteOnExit) {
            file.toFile().deleteOnExit();
        }
        return file;
    }

}
