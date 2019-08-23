package sirius.kernel.xml;

import sirius.kernel.Sirius;
import sirius.kernel.settings.Extension;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Resolves namespace prefixes into URLs based in the system configuration (<tt>xpath.namespaces</tt>).
 */
public class ConfigBasedNamespaceContext implements NamespaceContext {

    private Map<String, String> prefixNamespaceMap;

    public ConfigBasedNamespaceContext() {
        prefixNamespaceMap = new HashMap<>();

        for (Extension extension : Sirius.getSettings().getExtensions("xpath.namespaces")) {
            prefixNamespaceMap.put(extension.getId(), extension.getString("namespace"));
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefixNamespaceMap.containsKey(prefix)) {
            return prefixNamespaceMap.get(prefix);
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
