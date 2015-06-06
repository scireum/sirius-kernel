/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Helper classes to read and write XML.
 * <p>
 * One central class in here is the {@link sirius.kernel.xml.XMLReader} which is responsible for reading
 * large XML files, by splitting them into sub DOMs and processing them via xpath
 * ({@link sirius.kernel.xml.StructuredNode})
 * <p>
 * Additionally classes for reading and writing XML of "normal" size (i.e. for web service calls) are provided as
 * {@link sirius.kernel.xml.XMLStructuredInput} and {@link sirius.kernel.xml.XMLStructuredOutput} best used via
 * {@link sirius.kernel.xml.XMLCall}
 */
package sirius.kernel.xml;