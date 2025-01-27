package sirius.kernel.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Provides utility methods for working with XML.
 */
public class XmlUtil {

    private XmlUtil() {
        // Prevent instantiation
    }

    /**
     * Creates a new {@link DocumentBuilderFactory} which is secure by default.
     *
     * @return a new {@link DocumentBuilderFactory}
     * @throws ParserConfigurationException if a configuration error occurs
     */
    public static DocumentBuilderFactory createSecurityAwareDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory result = DocumentBuilderFactory.newInstance();
        result.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        result.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        result.setFeature("http://xml.org/sax/features/external-general-entities", false);
        result.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        result.setExpandEntityReferences(false);
        result.setXIncludeAware(false);
        return result;
    }
}
