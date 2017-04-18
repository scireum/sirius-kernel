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
import sirius.kernel.settings.Extension;

import java.util.List;

/**
 * Provides the name and modules of the currently active product.
 * <p>
 * Returns the current product as well as all modules known to the system.
 * <p>
 * The product information is fetched from the "product" section in the system configuration. The modules
 * are fetched from product.modules.
 */
public class Product {

    private static Module product;
    private static List<Module> modules;

    private Product() {
    }

    /**
     * Returns the name and build infos for the current product
     *
     * @return the product infos wrapped as {@link Module}
     */
    public static Module getProduct() {
        if (product == null) {
            product = new Module(Module.fix(Sirius.getSettings().getString("product.name"), "SIRIUS"),
                                 Sirius.getSettings().getString("product.version"),
                                 Sirius.getSettings().getString("product.build"),
                                 Sirius.getSettings().getString("product.date"),
                                 Sirius.getSettings().getString("product.vcs"));
        }

        return product;
    }

    /**
     * Returns a list of all modules known to the system
     *
     * @return a list containing the name and build environment of all modules
     */
    @SuppressWarnings("Convert2streamapi")
    public static List<Module> getModules() {
        if (modules == null) {
            List<Module> result = Lists.newArrayList();
            for (Extension ext : Sirius.getSettings().getExtensions("product.modules")) {
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
