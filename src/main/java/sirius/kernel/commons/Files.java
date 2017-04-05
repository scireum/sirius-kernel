/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * Helperclass for handling files in Java 8.
 */
public class Files {

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
        String effectiveInput = Strings.trim(input);
        if (Strings.isEmpty(effectiveInput)) {
            return Optional.empty();
        }

        effectiveInput = effectiveInput.replace("ä", "ae")
                     .replace("ö", "oe")
                     .replace("ü", "ue")
                     .replace("Ä", "Ae")
                     .replace("Ö", "Oe")
                     .replace("Ü", "Ue")
                     .replace("ß", "ss")
                     .replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
        Tuple<String, String> nameAndSuffix = Strings.splitAtLast(effectiveInput, ".");
        if (nameAndSuffix.getSecond() == null) {
            return Optional.of(effectiveInput);
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
     * the input is empty or does not contain a file extension. The a file contains several extensions, like
     * <tt>.txt.zip</tt>, only the last is returned.
     */
    @Nullable
    public static String getFileExtension(@Nullable String path) {
        return Strings.splitAtLast(path, ".").getSecond();
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
    public static String getFilenameWithoutExtension(@Nullable String path) {
        return Strings.splitAtLast(getFilenameAndExtension(path), ".").getFirst();
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
