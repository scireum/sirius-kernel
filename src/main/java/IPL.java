/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Initial Program Load - This is the main program entry point.
 * <p>
 * This will load all provided jar files from the "libs" sub folder as well as all classes from the "classes"
 * folder. When debugging from an IDE, set the system property <tt>ide</tt> to <tt>true</tt> - this will
 * bypass class loading, as all classes are typically provided via the system classpath.
 * <p>
 * This class only generates a <tt>ClassLoader</tt> which is then used to load
 * {@link sirius.kernel.Sirius#initializeEnvironment(ClassLoader)} as stage2.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class IPL {

    private static ClassLoader loader = ClassLoader.getSystemClassLoader();

    /**
     * Main Program entry point
     *
     * @param args currently the command line arguments are ignored.
     */
    public static void main(String[] args) {
        boolean kill = Boolean.parseBoolean(System.getProperty("kill"));
        int port = 0;
        if (kill && System.getProperty("port") != null) {
            port = Integer.parseInt(System.getProperty("port"));
        }
        // When we're started as windows service, the start/stop command and port are passed in
        // as arguments
        if (args.length == 2) {
            if ("stop".equals(args[0])) {
                kill = true;
            }
            port = Integer.parseInt(args[1]);
            // In case of "start", set port as system property so that Sirius will pick it up...
            System.setProperty("port", args[1]);
        }
        if (kill && port > 0) {
            kill(port);
        } else {
            kickstart();
        }
    }

    /**
     * Kills a sirius app by opening a connection to the lethal port.
     */
    private static void kill(int port) {
        try {
            System.out.println("Killing localhost:" + port);
            long now = System.currentTimeMillis();
            Socket socket = new Socket("localhost", port);
            socket.getInputStream().read();
            System.out.println("Kill succeeded after: " + (System.currentTimeMillis() - now) + " ms");
        } catch (Exception e) {
            System.out.println("Kill failed: ");
            e.printStackTrace();
        }
    }

    /*
     * Sets up a classloader and loads <tt>Sirius</tt> to initialize the framework.
     */
    private static void kickstart() {
        boolean debug = Boolean.parseBoolean(System.getProperty("debug"));
        boolean ide = Boolean.parseBoolean(System.getProperty("ide"));
        File home = new File(System.getProperty("user.dir"));
        System.out.println();
        System.out.println("I N I T I A L   P R O G R A M   L O A D");
        System.out.println("---------------------------------------");
        System.out.println("IPL from: " + home.getAbsolutePath());

        if (!ide) {
            List<URL> urls = new ArrayList<URL>();
            try {
                File jars = new File(home, "lib");
                if (jars.exists()) {
                    for (URL url : allJars(jars)) {
                        if (debug) {
                            System.out.println(" - Classpath: " + url);
                        }
                        urls.add(url);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            try {
                File classes = new File(home, "app");
                if (classes.exists()) {
                    if (debug) {
                        System.out.println(" - Classpath: " + classes.toURI().toURL());
                    }
                    urls.add(classes.toURI().toURL());
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), loader);
            Thread.currentThread().setContextClassLoader(loader);
        } else {
            System.out.println("IPL from IDE: not loading any classes or jars!");
        }

        try {
            System.out.println("IPL completed - Loading Sirius as stage2...");
            System.out.println();
            Class.forName("sirius.kernel.Sirius", true, loader)
                    .getMethod("initializeEnvironment", ClassLoader.class)
                    .invoke(null, loader);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * Enumerates all jars in the given directory
     */
    private static List<URL> allJars(File libs) throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        for (File file : libs.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                urls.add(file.toURI().toURL());
            }
        }
        return urls;
    }
}
