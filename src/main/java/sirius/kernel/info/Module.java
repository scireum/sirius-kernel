/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.info;

import sirius.kernel.commons.Strings;
import sirius.kernel.nls.NLS;

import java.time.LocalDate;

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
