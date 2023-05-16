/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings;

import sirius.kernel.commons.Tuple;
import sirius.kernel.health.Exceptions;

/**
 * Maps a given destination port to the effective port assigned.
 * <p>
 * This is mainly used in test environments where we provision external
 * dependencies via docker composer. As docker assigns a more or less
 * random port per test run, we need to determine and assign this.
 * <p>
 * As we do not want a production dependency against docker, we
 * use this bridge class and provide an appropriate implementation
 * as <tt>DockerHelper</tt> in the test scope.
 */
public abstract class PortMapper {

    private static PortMapper mapper;

    /**
     * Determines which port mapper to use.
     *
     * @param mapper the mapper to use.
     */
    public static void setMapper(PortMapper mapper) {
        PortMapper.mapper = mapper;
    }

    /**
     * Maps the given port for the given service.
     *
     * @param service the service used to identify to container which provides the service
     * @param host    the host to map
     * @param port    the port to map
     * @return the mapped port number or <tt>port</tt> if no mapper is present.
     */
    public static Tuple<String, Integer> mapPort(String service, String host, int port) {
        if (mapper == null) {
            return Tuple.create(host, port);
        }

        try {
            return mapper.map(service, host, port);
        } catch (Exception exception) {
            Exceptions.ignore(exception);
            return Tuple.create(host, port);
        }
    }

    /**
     * Maps the given port for the given service.
     *
     * @param service the service used to identify to container which provides the service
     * @param host    the host to map
     * @param port    the port to map
     * @return the mapped port number or <tt>port</tt> if no mapping is present.
     */
    protected abstract Tuple<String, Integer> map(String service, String host, int port);
}
