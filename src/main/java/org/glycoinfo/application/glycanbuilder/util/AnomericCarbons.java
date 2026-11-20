package org.glycoinfo.application.glycanbuilder.util;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;

/**
 * The anomeric carbon a monosaccharide is written and shown with (#332).
 *
 * <p>A monosaccharide in a ring has its anomeric carbon where its type says: C1 of an aldose, C2 of
 * a ketose. A residue taken from the menus has it so. The linkage toolbar and the residue properties
 * dialog also offered {@code ?}, and a GWS such as {@code --?D-GlcNAc,p} reads as {@code ?}. WURCS
 * builds the ring from that carbon, and with {@code ?} the writer either failed - a GlcNAc with a
 * child threw an {@code IndexOutOfBoundsException} and nothing was written - or wrote something
 * else: a beta-galactopyranose went out as the open chain {@code o2112h}, its anomer and ring gone.
 *
 * <p>So a monosaccharide in a pyranose or furanose ring is written with its type's anomeric carbon
 * when its own is not known, and the menus no longer offer {@code ?} for one. An alditol and an open
 * chain keep {@code ?}: they have no ring to close, and the properties dialog sets {@code ?} on them
 * itself. A reducing end of unknown ring size keeps it, as WURCS's {@code u} - ring or open chain, not
 * known. A bridge keeps it, and so does a type that names no anomeric carbon of its own.
 *
 * <p>A residue further out with neither its ring nor its anomeric carbon known - a {@code u} that is
 * not the reducing end, as in {@code [a2122h-1x_1-5][u2112h]/1-2/a4-b?} - is not written: it is joined
 * to its parent, and a monosaccharide joined so cannot be in doubt about being a ring. The writer
 * refuses it, as it did before, now saying why. The menus offer no {@code ?} for one, so it is not
 * drawn; one read from a file shows its type's carbon, and takes it when its properties are set.
 */
public final class AnomericCarbons {

	/** What the menus offer for a residue whose anomeric carbon has to be known. */
	private static final String[] KNOWN = { "1", "2", "3" };
	/** What they offer for any other: an alditol, an open chain, a bridge. */
	private static final String[] OR_UNKNOWN = { "?", "1", "2", "3" };

	private AnomericCarbons() {}

	/**
	 * Whether a residue in this ring form has an anomeric carbon that has to be known: a
	 * monosaccharide whose type says where its anomeric carbon is, in a pyranose or furanose ring, or
	 * in a ring of unknown size and joined to a parent by that carbon.
	 *
	 * <p>The reducing end in a ring of unknown size is left out. With its anomeric carbon unknown too it
	 * is how WURCS's {@code u2122h} is read - a reducing end that may be a ring or an open chain - and
	 * it is written back as that. A residue further out has no such choice: its anomeric carbon is
	 * what joins it to its parent, so it is a ring.
	 *
	 * @param _ringSize the ring form to ask about, which a toolbar or a dialog may be about to set: an
	 *        alditol closed into a ring is asked about as the ring it is becoming
	 */
	public static boolean mustBeKnown(Residue _residue, char _ringSize) {
		if (_residue == null || _residue.getType() == null || !_residue.isSaccharide()) return false;
		if ("Bridge".equals(_residue.getType().getSuperclass())) return false;
		if (_ringSize == '?' && isTheReducingEnd(_residue)) return false;
		if (_ringSize != 'p' && _ringSize != 'f' && _ringSize != '?') return false;
		return Character.isDigit(_residue.getType().getAnomericCarbon());
	}

	/** Whether a residue is the first of its structure, on the reducing-end marker or on nothing. */
	private static boolean isTheReducingEnd(Residue _residue) {
		Residue parent = _residue.getParent();
		return parent == null || parent.isReducingEnd();
	}

	/** Whether a residue, as it is, has an anomeric carbon that has to be known. */
	public static boolean mustBeKnown(Residue _residue) {
		return _residue != null && !_residue.isAlditol() && !_residue.isAldehyde()
				&& mustBeKnown(_residue, _residue.getRingSize());
	}

	/** The anomeric carbon a residue, as it is, is written with. */
	public static char of(Residue _residue) {
		char own = _residue.getAnomericCarbon();
		if (own == '?' && mustBeKnown(_residue)) return _residue.getType().getAnomericCarbon();
		return own;
	}

	/**
	 * The anomeric carbon a residue stands for in this ring form: its own, or its type's where its own
	 * is not known and has to be.
	 */
	public static char of(Residue _residue, char _ringSize) {
		char own = _residue.getAnomericCarbon();
		if (own == '?' && mustBeKnown(_residue, _ringSize)) return _residue.getType().getAnomericCarbon();
		return own;
	}

	/** The anomeric carbons to offer for a residue in this ring form: {@code ?} only where it may stay unknown. */
	public static String[] choices(Residue _residue, char _ringSize) {
		return (mustBeKnown(_residue, _ringSize) ? KNOWN : OR_UNKNOWN).clone();
	}

	/**
	 * The first residue of a structure that is joined to a parent with neither its ring nor its anomeric
	 * carbon known - WURCS's {@code u} further out than the reducing end - or null when there is none.
	 * Such a structure is not written.
	 */
	public static Residue anUnknownFormFurtherOut(Glycan _structure) {
		for (Residue residue : _structure.getAllResidues())
			if (residue.getAnomericCarbon() == '?' && residue.getRingSize() == '?' && !residue.isAlditol()
					&& !residue.isAldehyde() && mustBeKnown(residue, '?'))
				return residue;
		return null;
	}

	/** Whether a residue is stated to be a pyranose or a furanose and has an anomeric carbon to take. */
	private static boolean isAStatedRing(Residue _residue) {
		char ring = _residue.getRingSize();
		return (ring == 'p' || ring == 'f') && mustBeKnown(_residue);
	}

	/**
	 * Gives every pyranose and furanose of a structure whose anomeric carbon is not known its type's,
	 * for as long as it is written.
	 *
	 * <p>The structure is the person's: what they drew stays as they drew it. Run what is returned when
	 * the writing is done, whether or not it succeeded, and every residue is as it was.
	 *
	 * @return what puts the residues back
	 */
	public static Runnable takeTheirTypes(Glycan _structure) {
		final List<Residue> residues = new ArrayList<Residue>();
		final List<Character> childPositions = new ArrayList<Character>();
		for (Residue residue : _structure.getAllResidues()) {
			if (residue.getAnomericCarbon() != '?' || !isAStatedRing(residue)) continue;

			Linkage parent = residue.getParentLinkage();
			residues.add(residue);
			childPositions.add((parent == null) ? null : parent.getAnomericCarbon());
			// sets the parent linkage's child position too
			residue.setAnomericCarbon(residue.getType().getAnomericCarbon());
		}

		return new Runnable() {
			public void run() {
				for (int i = 0; i < residues.size(); i++) {
					Residue residue = residues.get(i);
					residue.setAnomericCarbon('?');
					Linkage parent = residue.getParentLinkage();
					if (parent != null && childPositions.get(i) != null)
						parent.setAnomericCarbon(childPositions.get(i));
				}
			}
		};
	}
}
