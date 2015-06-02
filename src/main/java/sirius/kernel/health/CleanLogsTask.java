/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.Sirius;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.EveryDay;

import java.time.Duration;

/**
 * Automatically deletes old log files.
 */
@Register
public class CleanLogsTask implements EveryDay {

    @ConfigValue("health.logFileRetention")
    private Duration logFileRetention;

    @Override
    public String getConfigKeyName() {
        return "logCleanupHour";
    }

    @Override
    public void runTimer() throws Exception {
        Sirius.getSetup().cleanOldLogFiles(logFileRetention.toMillis());
    }
}
