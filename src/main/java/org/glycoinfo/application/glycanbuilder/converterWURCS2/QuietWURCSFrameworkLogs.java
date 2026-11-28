package org.glycoinfo.application.glycanbuilder.converterWURCS2;

/**
 * Quiets a warning WURCSFramework logs for structures it handles correctly (#228).
 *
 * <p>Ordering a WURCS canonically, WURCSFramework's {@code WURCSVisitorCollectSequenceComparator}
 * logs "Leaf edge is not connected to any sequence" for a repeat whose closing bond has a position
 * not known - G67119YN's polysialic acids and polyfucose, {@code k2-k?~n}. The structure is read
 * whole, and the validator finds nothing wrong with it; the warning says nothing a user of this
 * application can act on. It came once for every normalization, and a read normalizes several times
 * and validates beside it: G67119YN logged it 24 times, at WARN, through a default configuration
 * that glycanformatconverter ships inside its jar.
 *
 * <p>Set to ERROR only where nobody has set a level for it. A program that configures this logger
 * itself keeps what it configured, and one that logs through anything other than logback is left
 * alone.
 */
final class QuietWURCSFrameworkLogs {

	static final String COMPARATOR = "org.glycoinfo.WURCSFramework.util.graph.comparator.WURCSVisitorCollectSequenceComparator";

	private QuietWURCSFrameworkLogs() {}

	/** Done once, before the first WURCS is read or written. */
	static void quiet() {
		try {
			org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(COMPARATOR);
			if (!(logger instanceof ch.qos.logback.classic.Logger)) return;
			ch.qos.logback.classic.Logger logback = (ch.qos.logback.classic.Logger) logger;
			if (logback.getLevel() == null) logback.setLevel(ch.qos.logback.classic.Level.ERROR);
		} catch (LinkageError notLogback) {
			// slf4j or logback is not there: nothing logs the warning through them, so nothing to quiet
		}
	}
}
