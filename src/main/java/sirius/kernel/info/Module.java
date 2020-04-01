/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.info;

import com.google.common.hash.Hashing;
import sirius.kernel.commons.Strings;
import sirius.kernel.nls.NLS;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reports build-time information about a loaded SIRIUS module.
 * <p>
 * This is used to keep track of loaded modules in the system.
 */
public class Module {

    private String name;
    private String version;
    private String build;
    private String date;
    private String vcs;

    private static final String RANDOM_REPLACEMENT = String.valueOf(ThreadLocalRandom.current().nextInt());

    protected Module(String name, String version, String build, String date, String vcs) {
        this.name = name;
        this.version = version;
        this.build = build;
        this.date = date;
        this.vcs = vcs;
    }

    /**
     * Returns the name of the module
     *
     * @return the name of the module
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a detailed description of the version of this module
     *
     * @return a string containing the version, build number and VCS tag of this module
     */
    public String getDetails() {
        return Strings.apply("Version: %s, Build: %s (%s), Revision: %s",
                             fix(version, "DEV"),
                             fix(build, "-"),
                             fix(date, NLS.toMachineString(LocalDate.now())),
                             fix(vcs, "-"));
    }

    /**
     * Creates a string which is unqiue for each released version (based on its commit hash and build number).
     * <p>
     * For development and test systems which have neigther of both, a random string is created for each
     * running instance.
     *
     * @return a unique version string per release or instance
     */
    public String getUniqueVersionString() {
        return Hashing.md5()
                      .hashString(fix(vcs, RANDOM_REPLACEMENT) + fix(build, RANDOM_REPLACEMENT), StandardCharsets.UTF_8)
                      .toString();
    }

    /**
     * As some module infos rely on a build environment (build server) their value will be the unreplaced version
     * (${property}) in development systems.
     * <p>
     * Therefore we filter those values and return the given replacement.
     *
     * @param value       the value to check
     * @param replacement the replacement to use
     * @return the given value or the replacement if the value is empty or a property key (starting with bracket -
     * as the $ is swallowed by typesafe config)
     */
    protected static String fix(String value, String replacement) {
        if (Strings.isEmpty(value) || value.startsWith("{") || value.startsWith("$")) {
            return replacement;
        }

        return value;
    }

    @Override
    public String toString() {
        return getName() + " (" + getDetails() + ")";
    }
}
