/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.info;

import com.google.common.collect.Lists;
import sirius.kernel.Sirius;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;

import java.util.List;

/**
 * Provides the name and modules of the currently active product.
 * <p>
 * Returns the current product as well as all modules known to the system.
 * <p>
 * The product information is fetched from the "product" section in the system configuration. The modules
 * are fetched from product.modules.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/12
 */
public class Product {

    private static Module product;
    private static List<Module> modules;

    /**
     * Returns the name and build infos for the current product
     *
     * @return the product infos wrapped as {@link Module}
     */
    public static Module getProduct() {
        if (product == null) {
            product = new Module(Module.fix(Sirius.getConfig().getString("product.name"), "SIRIUS"),
                                 Sirius.getConfig().getString("product.version"),
                                 Sirius.getConfig().getString("product.build"),
                                 Sirius.getConfig().getString("product.date"),
                                 Sirius.getConfig().getString("product.vcs"));
        }

        return product;
    }

    /**
     * Returns a list of all modules known to the system
     *
     * @return a list containing the name and build environment of all modules
     */
    public static List<Module> getModules() {
        if (modules == null) {
            List<Module> result = Lists.newArrayList();
            for (Extension ext : Extensions.getExtensions("product.modules")) {
                result.add(new Module(ext.getId(),
                                      ext.get("version").asString(),
                                      ext.get("build").asString(),
                                      ext.get("date").asString(),
                                      ext.get("vcs").asString()));
            }
            modules = result;
        }
        return modules;
    }

}
