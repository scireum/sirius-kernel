/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Helperclass for handling files in Java 8.
 */
public class Files {

    private Files() {
    }

    /**
     * Deletes the given directory structure <b>including</b> all sub directories.
     *
     * @param directory the directory to delete (along with all sub directories)
     * @throws IOException in case of an io error while deleting the files and directories
     */
    public static void delete(Path directory) throws IOException {
        if (!directory.toFile().exists()) {
            return;
        }
        java.nio.file.Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                java.nio.file.Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                java.nio.file.Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Removes all children (files and directories) of the given directory.
     *
     * @param directory the containing the children to delete (along with all sub directories)
     * @throws IOException in case of an io error while deleting the files and directories
     */
    public static void removeChildren(Path directory) throws IOException {
        java.nio.file.Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                java.nio.file.Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(directory)) {
                    java.nio.file.Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
