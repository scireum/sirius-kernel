/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.PriorityParts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.List;

/**
 * Responsible for executing {@link AutoSetupRule auto setup rules} during th system startup.
 * <p>
 * As most probably we only want to do this in dev / test / staging systems or once for production systems,
 * this facility has to be enabled via the system config in <tt>sirius.autoSetup</tt>.
 */
@Register(classes = {AutoSetup.class, Startable.class})
public class AutoSetup implements Startable {

    @ConfigValue("sirius.autoSetup")
    private boolean enabled;

    @PriorityParts(AutoSetupRule.class)
    private List<AutoSetupRule> rules;

    /**
     * Represents the logger used for everything concerning the system setup
     */
    public static final Log LOG = Log.get("autosetup");

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public void started() {
        if (!isEnabled()) {
            return;
        }

        for (AutoSetupRule rule : rules) {
            LOG.INFO("Executing auto setup rule: %s", rule.getClass().getName());
            try {
                rule.setup();
            } catch (Exception exception) {
                Exceptions.handle()
                          .to(LOG)
                          .error(exception)
                          .withSystemErrorMessage("Auto seatup rule %s failed: %s (%s)", rule.getClass().getName())
                          .handle();
            }
        }
    }

    /**
     * Determines if the auto setup facility is active.
     *
     * @return <tt>true</tt> if auto setup is enabled, <tt>false</tt> otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
}
