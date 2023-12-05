/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.commons.Strings;
import sirius.kernel.health.Log;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Retrieves a filtered list of resources in the classpath.
 * <p>
 * This is used by the {@link sirius.kernel.di.Injector} to discover and register all classes in the
 * component model. Additionally {@link sirius.kernel.nls.Babelfish} uses this to load all relevant .properties files.
 * <p>
 * The method used, is to provide a name of a resource which is placed in every component root (jar file etc.) which
 * then can be discovered using <tt>Class.getResources</tt>.
 * <p>
 * Once a file pattern is given, all files in the classpath are scanned, starting from the detected roots.
 */
public class Classpath {

    /**
     * Logger used to log problems when scanning the classpath
     */
    protected static final Log LOG = Log.get("classpath");
    private List<URL> componentRoots;
    private final ClassLoader loader;
    private final String componentName;
    private final List<String> customizations;

    /**
     * Creates a new Classpath, scanning for component roots with the given name
     *
     * @param loader         the class loader used to load the components
     * @param componentName  the file name to identify component roots
     * @param customizations the list of active customizations to filter visible resources
     */
    public Classpath(ClassLoader loader, String componentName, List<String> customizations) {
        this.loader = loader;
        this.componentName = componentName;
        this.customizations = customizations;
    }

    /**
     * Returns the class loader used to load the classpath
     *
     * @return the class loader used by the framework
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * Returns all detected component roots
     *
     * @return a list of URLs pointing to the component roots
     */
    public List<URL> getComponentRoots() {
        if (componentRoots == null) {
            try {
                componentRoots = Collections.list(loader.getResources(componentName));
                componentRoots.sort(Comparator.comparing(URL::toString));
            } catch (IOException exception) {
                LOG.SEVERE(exception);
            }
        }
        return componentRoots;
    }

    /**
     * Scans the classpath for files which relative path match the given patter
     *
     * @param pattern the pattern for the relative path used to filter files
     * @return a stream of matching elements
     */
    public Stream<Matcher> find(final Pattern pattern) {
        return getComponentRoots().stream().flatMap(this::scan).filter(path -> {
            if (customizations != null && path.startsWith("customizations")) {
                String config = Sirius.getCustomizationName(path);
                return customizations.contains(config);
            }
            return true;
        }).map(pattern::matcher).filter(Matcher::matches);
    }

    /*
     * Scans all files below the given root URL. This can handle file:// and jar:// URLs
     */
    private Stream<String> scan(URL url) {
        List<String> result = new ArrayList<>();
        if ("file".equals(url.getProtocol())) {
            try {
                File file = new File(url.toURI().getPath());
                if (!file.isDirectory()) {
                    file = file.getParentFile();
                }
                addFiles(file, result, file);
            } catch (URISyntaxException exception) {
                LOG.SEVERE(exception);
            }
        } else if ("jar".equals(url.getProtocol())) {
            try {
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> enumeration = jar.entries();
                while (enumeration.hasMoreElements()) {
                    JarEntry entry = enumeration.nextElement();
                    result.add(entry.getName());
                }
            } catch (IOException exception) {
                LOG.SEVERE(exception);
            }
        }
        return result.stream();
    }

    /*
     * DFS searcher for file / directory based classpath elements
     */
    private void addFiles(File file, List<String> collector, File reference) {
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        for (File child : file.listFiles()) {
            if (child.isDirectory()) {
                addFiles(child, collector, reference);
            } else {
                String path = buildRelativePath(reference, child);
                collector.add(path);
            }
        }
    }

    private String buildRelativePath(File reference, File child) {
        List<String> path = new ArrayList<>();
        File iter = child;
        while (iter != null && !Objects.equals(iter, reference)) {
            path.addFirst(iter.getName());
            iter = iter.getParentFile();
        }

        return Strings.join(path, "/");
    }
}
