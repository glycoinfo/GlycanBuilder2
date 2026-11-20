package org.glycoinfo.application.glycanbuilder.util.exchange;

import org.glycoinfo.WURCSFramework.util.WURCSException;

/**
 * A structure that has no WURCS, and why.
 *
 * <p>The write-side counterpart of {@link WURCSToGlycanException}. Both extend {@link WURCSException},
 * so a caller that wants to treat "could not read" and "could not write" alike can catch that.
 *
 * <p>The reason was always known and was handed to the caller as an empty string, with the sentence
 * itself left in a global last-error slot (#275). That works for the desktop application, which shows
 * the slot in a dialog and runs one export at a time; it does not work for anything that writes WURCS
 * for more than one person at once, where the slot belongs to whoever wrote last.
 */
public class GlycanToWURCSException extends WURCSException {

	private static final long serialVersionUID = 1L;

	public GlycanToWURCSException(String a_strMessage) {
		super(a_strMessage);
	}

	public GlycanToWURCSException(String a_strMessage, Throwable a_oCause) {
		super(a_strMessage, a_oCause);
	}

	/**
	 * Wrap what the conversion threw, keeping its sentence when it has one.
	 *
	 * <p>A checked exception is something the conversion meant to say - "D-gro-D-galHep has no stereo
	 * recorded for it, so it has no skeleton to write.", "A composition cannot be written in 'NeuAc'" -
	 * and is passed through word for word, since prefixing a class name to a sentence only makes it
	 * harder to read. A runtime exception is a crash rather than an answer, a NullPointerException out
	 * of the middle of the conversion, and is named as one. That is the same line
	 * {@code WURCS2Parser.readGlycan} draws on the way in.
	 */
	public static GlycanToWURCSException wrapping(Exception a_oCause) {
		if (a_oCause instanceof GlycanToWURCSException)
			return (GlycanToWURCSException) a_oCause;

		String said = a_oCause.getMessage();
		if (!(a_oCause instanceof RuntimeException) && said != null && !said.trim().isEmpty())
			return new GlycanToWURCSException(said, a_oCause);

		return new GlycanToWURCSException("could not write this structure as WURCS ("
				+ a_oCause.getClass().getSimpleName()
				+ (said != null ? ": " + said : "") + ")", a_oCause);
	}
}
