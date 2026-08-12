package org.glycoinfo.application.glycanbuilder.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The version this application was built as, taken from the build rather than written out again.
 *
 * <p>Maven fills {@code version.properties} from {@code pom.xml} - {@code src/main/resources} is
 * filtered - so what is reported is what was built. The About window has read the same number from
 * the same place all along, but only as text inside its HTML; nothing could ask for it.
 */
public final class ApplicationVersion {

	/** What is reported when the version cannot be read, e.g. running from a class directory. */
	public static final String UNKNOWN = "unknown";

	private static final String VERSION = read();

	private ApplicationVersion() {
	}

	/** @return Returns this application's version, e.g. "1.34.3", or {@link #UNKNOWN}. */
	public static String get() {
		return VERSION;
	}

	private static String read() {
		Properties build = new Properties();
		try (InputStream properties = ApplicationVersion.class.getResourceAsStream("/version.properties")) {
			if (properties == null) return UNKNOWN;
			build.load(properties);
		} catch (IOException cannotBeRead) {
			return UNKNOWN;
		}

		String version = build.getProperty("application.version", "").trim();

		// Maven leaves the placeholder as it stands when the file has not been filtered.
		return (version.isEmpty() || version.startsWith("${")) ? UNKNOWN : version;
	}
}
