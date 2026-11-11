package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eurocarbdb.application.glycanbuilder.Glycan;

/**
 * What a WURCS export leaves out of a structure ending in an aglycon of the user's - Ser, Ser/Thr,
 * Cer(d18:1/16:0) (#305).
 *
 * <p>WURCS describes the glycan, and has no place for what it is attached to: the structure is
 * written as the glycan alone, the anomer kept, and nothing in the text says an aglycon was there.
 * Exported and read back, it comes back with a free reducing end. The place to say so is the export.
 */
public final class AglyconInWURCS {

	private AglyconInWURCS() {}

	/** The WURCS format's key, as the export dialogs name it. */
	private static final String WURCS = "wurcs2";

	/**
	 * The aglycons a WURCS export of these structures leaves out, each named once, in the order the
	 * structures hold them. Nothing for any other format, and nothing of a structure that was not
	 * written at all, which is said elsewhere.
	 *
	 * @param _exported The structures exported.
	 * @param _failed Those of them that produced no output, or null.
	 * @param _format The format exported to.
	 */
	public static Set<String> leftOut(Collection<Glycan> _exported, Collection<Glycan> _failed, String _format) {
		Set<String> names = new LinkedHashSet<String>();
		if (_exported == null || _format == null || !WURCS.equalsIgnoreCase(_format)) return names;
		for (Glycan structure : _exported) {
			if (structure == null || !structure.hasAglycon()) continue;
			if (_failed != null && _failed.contains(structure)) continue;
			names.add(structure.getRoot().getResidueName());
		}
		return names;
	}

	/** The warning to show for {@link #leftOut}, or null when there is nothing to say. */
	public static String warning(Collection<Glycan> _exported, Collection<Glycan> _failed, String _format) {
		Set<String> names = leftOut(_exported, _failed, _format);
		if (names.isEmpty()) return null;

		StringBuilder message = new StringBuilder("WURCS has no place for an aglycon, so these were not written:\n\n");
		for (String name : names) message.append("    ").append(name).append('\n');
		message.append("\nEach structure was written as its glycan alone. Read back, it has a free reducing end.\n"
				+ "To keep the aglycon, save the document as GWS.");
		return message.toString();
	}
}
