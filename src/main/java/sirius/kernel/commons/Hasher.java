/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Provides a convenient way of computing common hash functions.
 */
public class Hasher {

    private static final MessageDigest MD5 = obtainDigest("MD5");
    private static final MessageDigest SHA1 = obtainDigest("SHA1");
    private static final MessageDigest SHA256 = obtainDigest("SHA-256");
    private static final MessageDigest SHA512 = obtainDigest("SHA-512");

    private MessageDigest digest;

    private Hasher(MessageDigest digest) {
        this.digest = digest;
    }

    private static MessageDigest obtainDigest(String name) {
        try {
            return MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            Log.SYSTEM.SEVERE(e);
            return null;
        }
    }

    /**
     * Creates a hasher based on the <tt>MD5</tt> hash function.
     *
     * @return a new instance to perform the hash function
     */
    public static Hasher md5() {
        try {
            return new Hasher((MessageDigest) MD5.clone());
        } catch (Exception e) {
            throw Exceptions.handle(Log.SYSTEM, e);
        }
    }

    /**
     * Creates a hasher based on the <tt>SHA 1</tt> hash function.
     *
     * @return a new instance to perform the hash function
     */
    public static Hasher sha1() {
        try {
            return new Hasher((MessageDigest) SHA1.clone());
        } catch (Exception e) {
            throw Exceptions.handle(Log.SYSTEM, e);
        }
    }

    /**
     * Creates a hasher based on the <tt>SHA 256</tt> hash function.
     *
     * @return a new instance to perform the hash function
     */
    public static Hasher sha256() {
        try {
            return new Hasher((MessageDigest) SHA256.clone());
        } catch (Exception e) {
            throw Exceptions.handle(Log.SYSTEM, e);
        }
    }

    /**
     * Creates a hasher based on the <tt>SHA 512</tt> hash function.
     *
     * @return a new instance to perform the hash function
     */
    public static Hasher sha512() {
        try {
            return new Hasher((MessageDigest) SHA512.clone());
        } catch (Exception e) {
            throw Exceptions.handle(Log.SYSTEM, e);
        }
    }

    /**
     * Appends the given value to the data to be hashed.
     *
     * @param value the value to hash. If <tt>null</tt> is given, the call is ignored. Otherwise, we use the
     *              <b>UTF-8</b> bytes of the string representation of the given value.
     * @return the hasher itself for fluent method calls
     */
    public Hasher hash(Object value) {
        if (value == null) {
            return this;
        }

        checkState();

        this.digest.update(value.toString().getBytes(StandardCharsets.UTF_8));
        return this;
    }

    private void checkState() {
        if (digest == null) {
            throw new IllegalStateException("Hash has already been computed");
        }
    }

    /**
     * Appends the given bytes to the data to be hashed.
     *
     * @param data the bytes to put into the hash function
     * @return the hasher itself for fluent method calls
     */
    public Hasher hashBytes(byte[] data) {
        checkState();

        this.digest.update(data);
        return this;
    }

    /**
     * Appends the bytes of the given long value to the data to be hashed.
     *
     * @param data the long value to be hashed
     * @return the hasher itself for fluent method calls
     */
    public Hasher hashLong(long data) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(data);
        return hashBytes(buffer.array());
    }

    /**
     * Appends the given bytes to the data to be hashed.
     *
     * @param data   the bytes to put into the hash function
     * @param offset the offset to start from in the array of bytes
     * @param length the number of bytes to use, starting at <tt>offset</tt>
     * @return the hasher itself for fluent method calls
     */
    public Hasher hashBytes(byte[] data, int offset, int length) {
        checkState();

        this.digest.update(data, offset, length);
        return this;
    }

    /**
     * Reads the given inputStream and appends its bytes to the data to be hashed.
     * <p>
     * The given stream is read until the end but not closed.
     *
     * @param inputStream the stream to be read and put into the hash function
     * @return the hasher itself for fluent method calls
     * @throws IOException in case of an IO error while reading the stream
     */
    public Hasher hashStream(InputStream inputStream) throws IOException {
        checkState();
        byte[] buffer = new byte[8192];

        int readBytes;
        do {
            readBytes = inputStream.read(buffer);
            if (readBytes > 0) {
                this.digest.update(buffer, 0, readBytes);
            }
        } while (readBytes != -1);
        return this;
    }

    /**
     * Reads the given file and appends its bytes to the data to be hashed.
     *
     * @param file the file to be read and put into the hash function
     * @return the hasher itself for fluent method calls
     * @throws sirius.kernel.health.HandledException in case of an IO error while reading the file
     */
    public Hasher hashFile(File file) {
        try (FileInputStream input = new FileInputStream(file)) {
            hashStream(input);
        } catch (IOException e) {
            throw Exceptions.handle(Log.SYSTEM, e);
        }
        return this;
    }

    /**
     * Computes the hash function and returns the resulting bytes.
     *
     * @return the computes hash as byte array
     */
    public byte[] toHash() {
        checkState();

        byte[] result = digest.digest();
        digest = null;
        return result;
    }

    @Override
    public String toString() {
        return toHexString();
    }

    /**
     * Computes the hash function and returns the resulting bytes as hex string.
     *
     * @return the computes hash as a string in hexadecimal encoded values
     */
    public String toHexString() {
        byte[] hash = toHash();
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    /**
     * Computes the hash function and returns the resulting bytes as BASE64 string.
     *
     * @return the computes hash as a string using BASE64 as encoding
     */
    public String toBase64String() {
        return Base64.getEncoder().encodeToString(toHash());
    }
}
