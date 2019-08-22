package sirius.kernel.xml;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;
import sirius.kernel.settings.Extension;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

/**
 * Provides all the namespaces for each prefix.
 */
@Register(classes = NamespaceContext.class)
public class ConfigBasedNamespaceContext implements NamespaceContext {

    @Override
    public String getNamespaceURI(String prefix) {
        for (Extension extension : Sirius.getSettings().getExtensions("xpath.namespaces")) {
            if (Strings.areEqual(prefix, extension.getId())) {
                return extension.getString("namespace");
            }
        }

        return XMLConstants.NULL_NS_URI;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }
}
