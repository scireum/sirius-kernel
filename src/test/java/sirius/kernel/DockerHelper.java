/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import org.joda.time.Duration;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.Initializable;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Log;
import sirius.kernel.settings.PortMapper;

import java.util.stream.Collectors;

/**
 * Initializes <b>Docker Composer</b> during test runs.
 * <p>
 * This basically uses <tt>docker.file</tt> from the system config to determine which
 * ccompser file to use and start / stops docker for each test run.
 * <p>
 * Also it provides a {@link PortMapper} to map the desired production ports to the
 * ones provided by the docker containers.
 */
@Register(classes = {Initializable.class, Lifecycle.class})
public class DockerHelper extends PortMapper implements Initializable, Lifecycle {

    @ConfigValue("docker.file")
    private String dockerfile;
    private DockerComposeRule docker;

    private static final Log LOG = Log.get("docker");

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    protected int map(String service, int port) {
        if (docker == null) {
            return port;
        }

        return docker.containers().container(service).port(port).getExternalPort();
    }

    @Override
    public void initialize() throws Exception {
        if (Strings.isFilled(dockerfile)) {
            LOG.INFO("Starting docker compose using: %s", dockerfile);
            this.docker = DockerComposeRule.builder().file(dockerfile).build();
            this.docker.before();
            awaitClusterHealth();

            PortMapper.setMapper(this);
        } else {
            LOG.INFO("No docker file is present - skipping....");
        }
    }

    private void awaitClusterHealth() {
        try {
            docker.containers().allContainers().forEach(c -> {
                LOG.INFO("Waiting for '%s' to become ready...", c.getContainerName());
                try {
                    new ClusterWait(ignored -> c.areAllPortsOpen(),
                                    Duration.standardSeconds(10)).waitUntilReady(docker.containers());
                    LOG.INFO("Container '%s' is ONLINE - Ports: %s",
                             c.getContainerName(),
                             c.ports().stream().map(Object::toString).collect(Collectors.joining(", ")));
                } catch (Exception e) {
                    LOG.SEVERE(e);
                }
            });
        } catch (Exception e) {
            LOG.SEVERE(e);
        }
    }

    @Override
    public void started() {
        // We start in "initialize to be ready when other components initialize...
    }

    @Override
    public void stopped() {
        // We stop in awaitTermination to keep all containers running until our
        // frameworks halted...
    }

    @Override
    public void awaitTermination() {
        if (docker != null) {
            docker.after();
        }
    }

    @Override
    public String getName() {
        return "docker";
    }
}
