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

    /**
     * Holds key value pairs of the namespace prefix and the namespace uri.
     */
    private Map<String, String> prefixNamespaceMap;

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefixNamespaceMap == null) {
            initializeMap();
        }
        return prefixNamespaceMap.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    private void initializeMap() {
        Map<String, String> map = new HashMap<>();

        for (Extension extension : Sirius.getSettings().getExtensions("xpath.namespaces")) {
            map.put(extension.getId(), extension.getString("namespace"));
        }

        this.prefixNamespaceMap = map;
    }
}
