/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.info;

import sirius.kernel.commons.Hasher;
import sirius.kernel.commons.Strings;
import sirius.kernel.nls.NLS;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reports build-time information about a loaded SIRIUS module.
 * <p>
 * This is used to keep track of loaded modules in the system.
 */
public class Module {

    private final String name;
    private final String version;
    private final String build;
    private final String date;
    private final String vcs;
    private String details;

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
        if (details == null) {
            details = Strings.apply("Version: %s, Build: %s (%s), Revision: %s",
                                    getVersion(),
                                    getBuild(),
                                    fix(date, NLS.toMachineString(LocalDate.now())),
                                    getVCS());
        }

        return details;
    }

    /**
     * Returns the raw version string.
     *
     * @return the software version of this module
     */
    public String getVersion() {
        return fix(version, "DEV");
    }

    /**
     * Returns the raw build number.
     *
     * @return the build number of this module version
     */
    public String getBuild() {
        return fix(build, "-");
    }

    /**
     * Returns the raw commit id.
     *
     * @return the commit id of this module version
     */
    public String getVCS() {
        return fix(vcs, "-");
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
        return Hasher.md5().hash(fix(vcs, RANDOM_REPLACEMENT) + fix(build, RANDOM_REPLACEMENT)).toHexString();
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
