/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.ImmutableCluster;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.DockerExecutable;
import com.palantir.docker.compose.execution.RetryingDockerCompose;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Wait;
import sirius.kernel.di.Initializable;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Log;
import sirius.kernel.settings.PortMapper;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * Initializes <b>Docker Composer</b> if requested by the framework.
 * <p>
 * This basically uses <tt>docker.file</tt> from the system config to determine which
 * composer file to use and start / stops docker for each test run or staging environment.
 * <p>
 * Also it provides a {@link PortMapper} to map the desired production ports to the
 * ones provided by the docker containers.
 */
@Register
public class DockerHelper extends PortMapper implements Initializable, Killable {

    private static final int MAX_WAIT_SECONDS = 10;

    @ConfigValue("docker.project")
    private String project;

    @ConfigValue("docker.hostIp")
    private String hostIp;

    @ConfigValue("docker.file")
    private String dockerfile;

    @ConfigValue("docker.retryAttempts")
    private int retryAttempts;

    @ConfigValue("docker.pull")
    private boolean pull;

    @ConfigValue("docker.keepRunning")
    private boolean keepRunning;

    private static final Log LOG = Log.get("docker");

    private DockerCompose dockerCompose;
    private Cluster cluster;
    private DockerMachine machine;
    private DockerExecutable executable;

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    protected Tuple<String, Integer> map(String service, String host, int port) {
        if (dockerCompose == null) {
            return Tuple.create(host, port);
        }

        return Tuple.create(machine.getIp(), containers().container(service).port(port).getExternalPort());
    }

    private DockerMachine machine() {
        if (machine == null) {
            if (Strings.isEmpty(hostIp)) {
                machine = DockerMachine.localMachine().build();
            } else {
                LOG.INFO("Using hostIp: %s", hostIp);
                machine = new DockerMachine(hostIp, System.getenv());
            }
        }
        return machine;
    }

    private DockerExecutable dockerExecutable() {
        if (executable == null) {
            executable = DockerExecutable.builder().dockerConfiguration(this.machine()).build();
        }
        return executable;
    }

    private Cluster containers() {
        if (cluster == null) {
            cluster = ImmutableCluster.builder()
                                      .ip(this.machine().getIp())
                                      .containerCache(new ContainerCache(this.docker(), this.dockerCompose))
                                      .build();
        }
        return cluster;
    }

    private Docker docker() {
        return new Docker(this.dockerExecutable());
    }

    private DockerComposeExecutable dockerComposeExecutable() {
        return DockerComposeExecutable.builder()
                                      .dockerComposeFiles(DockerComposeFiles.from(dockerfile))
                                      .dockerConfiguration(this.machine())
                                      .projectName(projectName())
                                      .build();
    }

    private ProjectName projectName() {
        return Strings.isFilled(project) && !Sirius.isStartedAsTest() ?
               ProjectName.fromString(project) :
               ProjectName.random();
    }

    @Override
    public void initialize() throws Exception {
        determineEffectiveDockerFile();
        if (Strings.isFilled(dockerfile)) {
            LOG.INFO("Starting docker compose using: %s", dockerfile);
            this.dockerCompose = new RetryingDockerCompose(retryAttempts,
                                                           new DefaultDockerCompose(dockerComposeExecutable(),
                                                                                    machine()));
            if (pull) {
                try {
                    LOG.INFO("Executing docker-compose pull...");
                    dockerCompose.pull();
                } catch (Exception e) {
                    LOG.WARN("docker-compose pull failed: %s (%s)", e.getMessage(), e.getClass().getName());
                }
            }
            try {
                LOG.INFO("Executing docker-compose up...");
                dockerCompose.up();
            } catch (Exception e) {
                LOG.WARN("docker-compose up failed: %s (%s)", e.getMessage(), e.getClass().getName());
            }

            awaitClusterHealth();

            PortMapper.setMapper(this);
        } else {
            LOG.INFO("No docker file is present - skipping....");
        }
    }

    private void determineEffectiveDockerFile() throws URISyntaxException {
        if (Strings.isEmpty(dockerfile)) {
            return;
        }
        if (new File(dockerfile).exists()) {
            return;
        }
        URL dockerResource = getClass().getResource(dockerfile.startsWith("/") ? dockerfile : "/" + dockerfile);
        if (dockerResource != null && dockerResource.toURI() != null && new File(dockerResource.toURI()).exists()) {
            dockerfile = new File(dockerResource.toURI()).getAbsolutePath();
        } else {
            dockerfile = null;
        }
    }

    private void awaitClusterHealth() {
        try {
            containers().allContainers().forEach(this::awaitContainerStart);
        } catch (Exception e) {
            LOG.SEVERE(e);
        }
    }

    private void awaitContainerStart(Container container) {
        LOG.INFO("Waiting for '%s' to become ready...", container.getContainerName());
        try {
            int retries = MAX_WAIT_SECONDS;
            while (container.areAllPortsOpen().failed()) {
                Wait.seconds(1);
                if (retries-- <= 0) {
                    LOG.WARN("Failed to start '%s' - Ports: %s",
                             container.getContainerName(),
                             container.ports().stream().map(Object::toString).collect(Collectors.joining(", ")));
                    return;
                }
            }

            LOG.INFO("Container '%s' is ONLINE - Ports: %s",
                     container.getContainerName(),
                     container.ports().stream().map(Object::toString).collect(Collectors.joining(", ")));
        } catch (Exception e) {
            LOG.SEVERE(e);
        }
    }

    @Override
    public void awaitTermination() {
        if (Strings.isEmpty(dockerfile)) {
            return;
        }

        if (keepRunning) {
            return;
        }

        if (Sirius.isStartedAsTest()) {
            try {
                LOG.INFO("Executing docker-compose kill...");
                dockerCompose.kill();
            } catch (Exception e) {
                LOG.WARN("docker-compose kill failed: %s (%s)", e.getMessage(), e.getClass().getName());
            }
            try {
                LOG.INFO("Executing docker-compose down...");
                dockerCompose.down();
            } catch (Exception e) {
                LOG.WARN("docker-compose down failed: %s (%s)", e.getMessage(), e.getClass().getName());
            }
            try {
                LOG.INFO("Executing docker-compose rm...");
                dockerCompose.rm();
            } catch (Exception e) {
                LOG.WARN("docker-compose rm failed: %s (%s)", e.getMessage(), e.getClass().getName());
            }
        } else {
            try {
                LOG.INFO("Executing docker-compose stop...");
                containers().allContainers().forEach(c -> {
                    try {
                        LOG.INFO("Executing docker-compose stop for '%s'...", c.getContainerName());
                        dockerCompose.stop(c);
                    } catch (Exception e) {
                        LOG.WARN("docker-compose stop for '%s' failed: %s (%s)",
                                 c.getContainerName(),
                                 e.getMessage(),
                                 e.getClass().getName());
                    }
                });
            } catch (Exception e) {
                LOG.WARN("docker-compose stop failed: %s (%s)", e.getMessage(), e.getClass().getName());
            }
        }
    }
}
