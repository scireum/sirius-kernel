/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.health.Exceptions;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static helper class which helps to parse filenames from content disposition headers
 * <p>
 * Based on DownloadUtils by mozilla-mobile/android-components
 * <a href="https://github.com/mozilla-mobile/android-components/blob/main/components/support/utils/src/main/java/mozilla/components/support/utils/DownloadUtils.kt">DownloadUtils.kt on github.com/mozilla-mobile/android-components</a>
 * <p>
 * Right for use and modification was granted under the Mozilla Public License 2.0 ,
 * <a href="http://mozilla.org/MPL/2.0/">MPL 2.0</a>
 */
public class ContentDispositionParser {

    /**
     * This is the regular expression to match the content disposition type segment.
     * <p>
     * A content disposition header can start either with inline or attachment followed by semicolon;
     * For example: attachment; filename="filename.jpg" or inline; filename="filename.jpg"
     * (inline|attachment)\\s*; -> Match either inline or attachment, followed by zero or more
     * optional whitespaces characters followed by a semicolon.
     */
    private static final String CONTENT_DISPOSITION_TYPE = "(inline|attachment)\\s*;";

    /**
     * This is the regular expression to match filename* parameter segment.
     * <p>
     * A content disposition header could have an optional filename* parameter,
     * the difference between this parameter and the filename is that this uses
     * the encoding defined in RFC 5987.
     * <p>
     * Some examples:
     * filename*=utf-8''success.html
     * filename*=iso-8859-1'en'file%27%20%27name.jpg
     * filename*=utf-8'en'filename.jpg
     * <p>
     * For matching this section we use:
     * \\s*filename\\s*=\\s*= -> Zero or more optional whitespaces characters
     * followed by filename followed by any zero or more whitespaces characters and the equal sign;
     * <p>
     * (utf-8|iso-8859-1)-> Either utf-8 or iso-8859-1 encoding types.
     * <p>
     * '[^']*'-> Zero or more characters that are inside of single quotes '' that are not single
     * quote.
     * <p>
     * (\S*) -> Zero or more characters that are not whitespaces. In this group,
     * it's where we are going to have the filename.
     */
    private static final String CONTENT_DISPOSITION_FILE_NAME_ASTERISK =
            "\\s*filename\\*\\s*=\\s*\"?(utf-8|iso-8859-1)'[^']*'([^;\"\\s]*)\"?.*";

    /**
     * Format as defined in RFC 2616 and RFC 5987
     * Both inline and attachment types are supported.
     * More details can be found <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition">
     * developer.mozilla.org: Content-Disposition</a>
     * <p>
     * The first segment is the [contentDispositionType], there you can find the documentation,
     * Next, it's the filename segment, where we have a filename="filename.ext"
     * For example, all of these could be possible in this section:
     * filename="filename.jpg"
     * filename="file\"name.jpg"
     * filename="file\\name.jpg"
     * filename="file\\\"name.jpg"
     * filename=filename.jpg
     * <p>
     * For matching this section we use:
     * \\s*filename\\s*=\\s*= -> Zero or more whitespaces followed by filename followed
     * by zero or more whitespaces and the equal sign.
     * <p>
     * As we want to extract the content of filename="THIS", we use:
     * <p>
     * \\s* -> Zero or more whitespaces
     * <p>
     * (\"((?:\\\\.|[^|"\\\\])*)\" -> A quotation mark, optional : or \\ or any character,
     * and any non quotation mark or \\\\ zero or more times.
     * <p>
     * For example: filename="file\\name.jpg", filename="file\"name.jpg" and filename="file\\\"name.jpg"
     * <p>
     * We don't want to match after ; appears, For example filename="filename.jpg"; foo
     * we only want to match before the semicolon, so we use. |[^;]*)
     * <p>
     * \\s* ->  Zero or more whitespaces.
     * <p>
     * For supporting cases, where we have both filename and filename*, we use:
     * "(?:;$contentDispositionFileNameAsterisk)?"
     * <p>
     * Some examples:
     * <p>
     * attachment; filename="_.jpg"; filename*=iso-8859-1'en'file%27%20%27name.jpg
     * attachment; filename="_.jpg"; filename*=iso-8859-1'en'file%27%20%27name.jpg
     */
    @SuppressWarnings("java:S5998")
    @Explain("Catastrophic backtracking is prevented by limiting the input length for this regex.")
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_TYPE
                                                                               + ".*\\s*filename\\s*=\\s*(\"((?:\\\\.|[^\"\\\\])*)\"|[^;]*)\\s*"
                                                                               + "(?:;"
                                                                               + CONTENT_DISPOSITION_FILE_NAME_ASTERISK
                                                                               + ")?", Pattern.CASE_INSENSITIVE);

    /**
     * This is an alternative content disposition pattern where only filename* is available
     */
    private static final Pattern FILE_NAME_ASTERISK_CONTENT_DISPOSITION_PATTERN = Pattern.compile(
            CONTENT_DISPOSITION_TYPE + CONTENT_DISPOSITION_FILE_NAME_ASTERISK,
            Pattern.CASE_INSENSITIVE);

    /**
     * Keys for the capture groups inside contentDispositionPattern
     */
    private static final int ENCODED_FILE_NAME_GROUP = 5;
    private static final int ENCODING_GROUP = 4;
    private static final int QUOTED_FILE_NAME_GROUP = 3;
    private static final int UNQUOTED_FILE_NAME = 2;

    /**
     * Belongs to the [fileNameAsteriskContentDispositionPattern]
     */
    private static final int ALTERNATIVE_FILE_NAME_GROUP = 3;
    private static final int ALTERNATIVE_ENCODING_GROUP = 2;

    /**
     * Definition as per RFC 5987, section 3.2.1. (value-chars)
     */
    private static final Pattern encodedSymbolPattern =
            Pattern.compile("%[0-9a-f]{2}|[0-9a-z!#$&+-.^_`|~]", Pattern.CASE_INSENSITIVE);

    /**
     * Limits the maximal header length to parse.
     * <p>
     * Note that any longer header will lead to an exception so that we detect that the limit was actually hit.
     */
    private static final int MAX_CONTENT_DISPOSITION_LENGTH = 512;

    private ContentDispositionParser() {
        // static helper class with no public constructor
    }

    /**
     * Tries to parse a file name from the given string which is from a content disposition header.
     * <p>
     * The format of the header is defined here: <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html">rfc2616-sec19</a>
     * This header provides a filename for content that is going to be downloaded to the file system.
     *
     * @param contentDisposition the content-disposition header as String
     * @return an Optional containing the file name given by the header, or Optional.empty if no file name is given
     */
    public static Optional<String> parseFileName(String contentDisposition) {
        if (Strings.isEmpty(contentDisposition)) {
            return Optional.empty();
        }
        if (contentDisposition.length() > MAX_CONTENT_DISPOSITION_LENGTH) {
            // We use this circuit breaker, as the regex used to parse the header is subject to catastrophic backtracking
            // and might otherwise crash the JVM for overly long inputs...
            throw new IllegalArgumentException("Cannot parse an overly long content-disposition header: "
                                               + contentDisposition);
        }
        try {
            return parseContentDispositionWithFileName(contentDisposition).or(() -> parseContentDispositionWithFileNameAsterisk(
                    contentDisposition)).map(String::trim);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            Exceptions.ignore(exception);
        }
        return Optional.empty();
    }

    private static Optional<String> parseContentDispositionWithFileName(String contentDisposition) {
        Matcher matcher = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            String encodedFileName = matcher.group(ENCODED_FILE_NAME_GROUP);
            String encoding = matcher.group(ENCODING_GROUP);
            if (Strings.isFilled(encodedFileName) && Strings.isFilled(encoding != null)) {
                try {
                    return Optional.of(decodeHeaderField(encodedFileName, encoding));
                } catch (UnsupportedEncodingException exception) {
                    Exceptions.ignore(exception);
                    return Optional.empty();
                }
            } else {
                // Return quoted string if available and replace escaped characters.
                String quotedFileName = matcher.group(QUOTED_FILE_NAME_GROUP);
                if (Strings.isFilled(quotedFileName)) {
                    return Optional.of(Pattern.compile("\\\\(.)").matcher(quotedFileName).replaceAll("$1"));
                }
                return Optional.of(matcher.group(UNQUOTED_FILE_NAME));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> parseContentDispositionWithFileNameAsterisk(String contentDisposition) {
        Matcher matcher = FILE_NAME_ASTERISK_CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);

        if (matcher.find()) {
            String encoding = matcher.group(ALTERNATIVE_ENCODING_GROUP);
            String fileName = matcher.group(ALTERNATIVE_FILE_NAME_GROUP);
            if (Strings.isFilled(encoding) && Strings.isFilled(fileName)) {
                try {
                    return Optional.of(decodeHeaderField(fileName, encoding));
                } catch (UnsupportedEncodingException exception) {
                    Exceptions.ignore(exception);
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private static String decodeHeaderField(String field, String encoding) throws UnsupportedEncodingException {
        Matcher matcher = encodedSymbolPattern.matcher(field);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        while (matcher.find()) {
            String symbol = matcher.group();

            if (symbol.startsWith("%")) {
                stream.write(Integer.parseInt(symbol.substring(1), 16));
            } else {
                stream.write(symbol.charAt(0));
            }
        }
        return stream.toString(encoding);
    }
}
