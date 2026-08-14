package org.eurocarbdb.application.glycanbuilder.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * The XML readers this application uses, configured to read only the document it was given.
 *
 * <p>A default JAXP parser resolves external entities, so a document can name a file and have its
 * contents substituted into the parse. Measured on this project before this class existed: an entity
 * pointing at a temporary file was expanded and the file's contents came back through both
 * {@link XMLUtils#read(String)} and {@link SAXUtils#read}. The same configuration will issue network
 * requests from a document, so the reach is not limited to the local disk.
 *
 * <p>Everything this application reads as XML - workspaces, configuration, fragment definitions,
 * sequences pasted in by a user - goes through those two helpers, and none of it uses a
 * {@code DOCTYPE}. So the strongest setting is also the compatible one: a document type declaration is
 * refused outright, which removes entity substitution rather than restricting it.
 *
 * <p><b>It fails closed.</b> A parser that cannot be told to be safe is not returned - an unsupported
 * feature is thrown rather than logged and stepped over, because a reader that quietly falls back to
 * the default configuration is the fault this class exists to remove.
 *
 * <p>Java 8 and the Java 21 runtime the installers carry both support all of these.
 */
public final class SecureXml {

	/** Rejects a document type declaration, and with it every entity that could be declared in one. */
	private static final String DISALLOW_DOCTYPE =
			"http://apache.org/xml/features/disallow-doctype-decl";

	/** Belt and braces behind the DOCTYPE refusal, for a parser that honours these instead. */
	private static final String EXTERNAL_GENERAL_ENTITIES =
			"http://xml.org/sax/features/external-general-entities";
	private static final String EXTERNAL_PARAMETER_ENTITIES =
			"http://xml.org/sax/features/external-parameter-entities";
	private static final String LOAD_EXTERNAL_DTD =
			"http://apache.org/xml/features/nonvalidating/load-external-dtd";

	private SecureXml() {
	}

	/**
	 * A DOM builder that reads only what it is handed.
	 *
	 * @throws Exception
	 *             when the parser cannot be configured safely. Deliberately not caught here: the
	 *             caller gets no parser rather than an unsafe one.
	 */
	public static DocumentBuilder newDocumentBuilder() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		factory.setFeature(DISALLOW_DOCTYPE, true);
		factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
		factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
		factory.setFeature(LOAD_EXTERNAL_DTD, false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);

		// Belongs to the factory rather than the builder, and is only honoured by some
		// implementations - which is why it sits behind the DOCTYPE refusal rather than in front of it.
		trySetAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
		trySetAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);

		return factory.newDocumentBuilder();
	}

	/**
	 * A SAX parser that reads only what it is handed.
	 *
	 * @throws Exception
	 *             when the parser cannot be configured safely.
	 */
	public static SAXParser newSAXParser() throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();

		factory.setFeature(DISALLOW_DOCTYPE, true);
		factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
		factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
		factory.setFeature(LOAD_EXTERNAL_DTD, false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setXIncludeAware(false);

		SAXParser parser = factory.newSAXParser();
		trySetProperty(parser, XMLConstants.ACCESS_EXTERNAL_DTD);
		trySetProperty(parser, XMLConstants.ACCESS_EXTERNAL_SCHEMA);

		return parser;
	}

	/**
	 * Deny an external-access property, where the implementation has one.
	 *
	 * <p>The one place something is allowed to be unsupported, and it is safe for it to be: the
	 * DOCTYPE refusal above has already removed the declaration these properties would restrict. An
	 * implementation that does not recognise the name has nothing to restrict either.
	 */
	private static void trySetAttribute(DocumentBuilderFactory factory, String name) {
		try {
			factory.setAttribute(name, "");
		} catch (IllegalArgumentException notSupported) {
			// see above
		}
	}

	private static void trySetProperty(SAXParser parser, String name) {
		try {
			parser.setProperty(name, "");
		} catch (Exception notSupported) {
			// see above
		}
	}
}
