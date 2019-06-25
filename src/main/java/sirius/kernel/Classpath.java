/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.google.common.base.Objects;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Log;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
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
    private ClassLoader loader;
    private String componentName;
    private List<String> customizations;

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
                componentRoots.sort((url1, url2) -> {
                    String asString1 = Value.of(Strings.toString(url1)).asString();
                    String asString2 = Value.of(Strings.toString(url2)).asString();

                    // put the files from jar-files in front of the files of the project, so library-files will be read
                    // first, and therefore can be overwritten by an actual applications
                    if (asString1.startsWith("jar") && asString2.startsWith("file")) {
                        return -1;
                    }
                    if (asString1.startsWith("file") && asString2.startsWith("jar")) {
                        return 1;
                    }

                    return asString1.compareTo(asString2);
                });
            } catch (IOException e) {
                LOG.SEVERE(e);
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
            } catch (URISyntaxException e) {
                LOG.SEVERE(e);
            }
        } else if ("jar".equals(url.getProtocol())) {
            try {
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> e = jar.entries();
                while (e.hasMoreElements()) {
                    JarEntry entry = e.nextElement();
                    result.add(entry.getName());
                }
            } catch (IOException e) {
                LOG.SEVERE(e);
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
        while (iter != null && !Objects.equal(iter, reference)) {
            path.add(0, iter.getName());
            iter = iter.getParentFile();
        }

        return Strings.join(path, "/");
    }
}
