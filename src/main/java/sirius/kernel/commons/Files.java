/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Helperclass for handling files in Java 8.
 */
public class Files {

    private static final Pattern NON_PATH_CHARACTERS = Pattern.compile("[^a-zA-Z0-9\\-.]");

    /**
     * Contains a list of file names and endings which are considered to be metadata.
     */
    private static final List<String> METADATA_FILES = List.of(
            "__MACOSX",
            ".DS_Store",
            "Thumbs.db");

    private Files() {
    }

    /**
     * Converts the given string to a valid and sane filename.
     * <p>
     * The result will only consist of letters, digits '-' and '_'. Everything else will be
     * replaced by a '_'. German umlauts like 'ä', 'ö', 'ü' are replaced the appropriate
     * ASCII sequences.
     *
     * @param input the filename to fix
     * @return the transformed filename wrapped as Optional or an empty Optional if the filename is not filled
     */
    @Nonnull
    public static Optional<String> toSaneFileName(@Nullable String input) {
        String sanitizedInput = Strings.cleanup(input,
                                                StringCleanup::reduceCharacters,
                                                StringCleanup::trim,
                                                path -> NON_PATH_CHARACTERS.matcher(path).replaceAll("_"));
        if (Strings.isEmpty(sanitizedInput)) {
            return Optional.empty();
        }

        Tuple<String, String> nameAndSuffix = Strings.splitAtLast(sanitizedInput, ".");
        if (nameAndSuffix.getSecond() == null) {
            return Optional.of(sanitizedInput);
        }

        return Optional.of(nameAndSuffix.getFirst().replace(".", "_") + "." + nameAndSuffix.getSecond());
    }

    /**
     * Returns the file extension of the given path or filename.
     * <p>
     * A path could be <tt>/dir/foo/test.txt</tt>, a filename would be <tt>test.txt</tt>.
     *
     * @param path the path to parse
     * @return the file extension (<tt>txt</tt> for <tt>test.txt</tt>) without the leading dot. Returns <tt>null</tt> if
     * the input is empty or does not contain a file extension. If a file contains several extensions, like
     * <tt>.txt.zip</tt>, only the last is returned.
     */
    @Nullable
    @SuppressWarnings("java:S2259")
    @Explain("path is checked for null by the Strings.isEmpty call in getFilenameAndExtension")
    public static String getFileExtension(@Nullable String path) {
        return Strings.splitAtLast(getFilenameAndExtension(path), ".").getSecond();
    }

    /**
     * Returns the basepath of the given path to a file.
     * <p>
     * The basepath is everything but the filename of the given path. So <tt>/foo/bar/test.txt</tt>
     * will yield <tt>/foo/bar</tt> as as path. <tt>text.txt</tt> will yield <tt>null</tt>.
     * <p>
     * Note that both <tt>/</tt> and <tt>\</tt> are accepted as path separators and are preserved in the output.
     *
     * @param path the path to a file to parse
     * @return the path part of the given path, which is everything but the last separator and the filename. Returns
     * <tt>null</tt> if the given path is empty or does not contain a path.
     */
    @Nullable
    @SuppressWarnings("java:S2259")
    @Explain("path is already checked for null by the Strings.isEmpty call")
    public static String getBasepath(@Nullable String path) {
        if (Strings.isEmpty(path)) {
            return null;
        }
        int lastPathSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastPathSeparator <= 0) {
            return null;
        }

        return path.substring(0, lastPathSeparator);
    }

    /**
     * Returns the filename with the file extension of the given path.
     * <p>
     * The filename is everything after the last separator (<tt>/</tt> or <tt>\</tt>). If the given path
     * does not contain a separator, the whole path will be returned.
     *
     * @param path the path to a file to parse
     * @return the filename without the path to it. Returns <tt>null</tt> if the input is empty or <tt>null</tt>.
     */
    @Nullable
    @SuppressWarnings("java:S2259")
    @Explain("path is already checked for null by the Strings.isEmpty call")
    public static String getFilenameAndExtension(@Nullable String path) {
        if (Strings.isEmpty(path)) {
            return null;
        }
        int lastPathSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastPathSeparator < 0) {
            return path;
        }
        if (lastPathSeparator == path.length() - 1) {
            return null;
        }

        return path.substring(lastPathSeparator + 1);
    }

    /**
     * Returns the filename without its file extension.
     * <p>
     * The file extension is everything after the last <tt>.</tt> in the filename.
     *
     * @param path the path to a file to parse
     * @return the filename without extension or <tt>null</tt> if the given path is empty, <tt>null</tt> or does not
     * have a filename (ends with a sepratator).
     */
    @Nullable
    @SuppressWarnings("java:S2259")
    @Explain("path is checked for null by the Strings.isEmpty call in getFilenameAndExtension")
    public static String getFilenameWithoutExtension(@Nullable String path) {
        return Strings.splitAtLast(getFilenameAndExtension(path), ".").getFirst();
    }

    /**
     * Determines if the given path is hidden.
     * <p>
     * A path is considered hidden if it starts with a dot.
     *
     * @param path the path to check
     * @return <tt>true</tt> if the path is hidden, <tt>false</tt> otherwise
     */
    public static boolean isConsideredHidden(@Nullable String path) {
        try {
            return getFilenameAndExtension(path).startsWith(".");
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Determines if the given path is a metadata file.
     * <p>
     * A metadata file is a file which is not part of the actual content but rather contains metadata or is used by
     * the operating system or other tools.
     *
     * @param path the path to check
     * @return <tt>true</tt> if the path is a metadata file that is listed in the METADATA_FILES list, <tt>false</tt> otherwise
     */
    public static boolean isConsideredMetadata(@Nullable String path) {
        if (Strings.isEmpty(path)) {
            return false;
        }
        return METADATA_FILES.stream().anyMatch(path::startsWith);
    }

    /**
     * If the given file is not null and exists, tries to delete that file and logs when a file cannot be deleted. T
     * his is useful for error reporting and to diagnose why a file cannot be deleted.
     *
     * @param file the file to delete
     */
    public static void delete(@Nullable File file) {
        if (file != null) {
            try {
                java.nio.file.Files.deleteIfExists(file.toPath());
            } catch (IOException exception) {
                Exceptions.handle(exception);
            }
        }
    }

    /**
     * Deletes the given directory structure <b>including</b> all sub directories.
     *
     * @param directory the directory to delete (along with all sub directories)
     * @throws IOException in case of an io error while deleting the files and directories
     */
    public static void delete(@Nullable Path directory) throws IOException {
        if (directory == null) {
            return;
        }
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
