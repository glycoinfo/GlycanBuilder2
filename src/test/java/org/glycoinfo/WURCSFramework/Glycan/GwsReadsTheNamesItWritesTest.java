package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.converterGWS.GWSParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Every residue name this application can write to a {@code .gws} file, it can read back.
 *
 * <p>It could not. The GWS residue-name class has been {@code [a-zA-z0-9_#=.]} since 2007, when the
 * dictionary held 81 types and every one of them fitted it. Thirty-five of today's names do not:
 * thirty carry a {@code -} (the heptoses, the octoses, {@code Tri-P}), three are parenthesised
 * ({@code (S)Lac}), and two carry both. Drawing one of them, saving and reopening lost the file -
 * <em>invalid format for linkage: -gro-D-manHep,p</em> - which is #256, reported nine years later by
 * somebody who had drawn a heptose.
 *
 * <p>The reader was widened rather than the writer changed, so the round trip closes on the
 * <em>identical</em> string and no file written before the change means anything different
 * afterwards. See {@code docs/gws-format.md} §1.3.
 *
 * <p>The first test is the one that matters, and it asks the dictionaries rather than a list: the
 * count of unreadable names has been given as 6, then 33, and is 35, and every one of those was a
 * hand count. A test that walks {@code allResidues()} stays true when somebody adds a row.
 */
public class GwsReadsTheNamesItWritesTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * Written by this application, read by this application, and the same residue comes back.
	 *
	 * <p>Built through the model and written with the writer, not assembled as a string: a test that
	 * builds its input the way the code under test likes it will not find a fault on the way in.
	 */
	@Test
	public void everyDictionaryNameSurvivesTheRoundTrip() {
		List<String> unreadable = new ArrayList<String>();
		List<String> renamed = new ArrayList<String>();
		int checked = 0;

		List<ResidueType> types = new ArrayList<ResidueType>();
		types.addAll(ResidueDictionary.allResidues());
		// The thirty names this test exists for are in the second dictionary, not the first:
		// allResidues() returns conf/residue_types alone, and the heptoses and octoses live in
		// conf/non_symbolic_residue_types. Counting only the first is how the figure 6 was arrived at.
		types.addAll(NonSymbolicResidueDictionary.getResidues());

		for (ResidueType type : types) {
			String written;
			try {
				written = writeAlone(type);
			} catch (Exception cannotBeDrawn) {
				continue;
			}
			checked++;

			Glycan readBack;
			try {
				readBack = Glycan.fromString(written);
			} catch (Exception e) {
				unreadable.add(type.getName() + " -> " + written + " : " + e.getMessage());
				continue;
			}
			if (readBack == null) {
				unreadable.add(type.getName() + " -> " + written + " : read back as null");
				continue;
			}
			if (!written.equals(readBack.toString())) {
				renamed.add(type.getName() + " : wrote " + written + ", read back " + readBack);
			} else if (!written.isEmpty() && !readsAs(readBack, type.getName())) {
				// (a reducing-end marker hung off a reducing end writes nothing, and there is nothing to read)
				// The same text is not enough: D-gro-D-galHep and 23 others came back as the same text
				// and as an unknown residue named gro-D-galHep, of no mass (#252).
				renamed.add(type.getName() + " : wrote " + written + ", read back the same text as "
						+ residueNames(readBack));
			}
		}

		assertTrue("expected both dictionaries to be loaded, checked " + checked, checked > 200);
		assertTrue(renamed.size() + " names do not survive unchanged: " + renamed, renamed.isEmpty());

		List<String> stillUnreadable = new ArrayList<String>();
		for (String one : unreadable) stillUnreadable.add(one.substring(0, one.indexOf(' ')));
		java.util.Collections.sort(stillUnreadable);

		assertEquals("the set of unreadable names changed - read the comment above before"
				+ " adjusting this list\n  " + unreadable,
				KNOWN_BAD_ROWS.toString(), stillUnreadable.toString());
	}

	/**
	 * No name is unreadable any more. Widening the name class took this from thirty-five to five, and the
	 * five were bad rows in conf/non_symbolic_residue_types that no reader could accommodate. #273
	 * corrected them (the maintainer, 2026-11-28): araOct and manOct give their anomeric carbon as
	 * {@code ?} rather than {@code -}; {@code ?-gro-D-manHep} and {@code ?-gro-D-glcHep} are
	 * {@code gro-D-manHep} and {@code gro-D-glcHep}, as their iupac_name column had them; and
	 * {@code L-gro-D-glcOct}, whose name, iupac_name and description disagreed, is gone. The assertion
	 * is still on the exact set, so a name that becomes unreadable fails it.
	 */
	private static final List<String> KNOWN_BAD_ROWS = java.util.Collections.emptyList();

	/** #256, as the reporter's own file had it - saved by this application, and refused by it. */
	@Test
	public void theHeptoseFromIssue256Reopens() throws Exception {
		assertReopensUnchanged("freeEnd--??1D-GlcNAc,p--??1D-D-gro-D-manHep,p$MONO,Und,0,0,freeEnd");
	}

	/** The second offending form: a name that opens with a parenthesised character. */
	@Test
	public void aParenthesisedNameReopens() throws Exception {
		assertReopensUnchanged("freeEnd--??1D-GlcNAc,p--??1(S)Lac$MONO,Und,0,0,freeEnd");
	}

	/** And the plain one, which needs nothing but the dash. */
	@Test
	public void aDashInTheMiddleOfANameReopens() throws Exception {
		assertReopensUnchanged("freeEnd--??1D-GlcNAc,p--??1Tri-P$MONO,Und,0,0,freeEnd");
	}

	/**
	 * A branch is still a branch.
	 *
	 * <p>This is why the parenthesised form is one character wide and not {@code [()]}. Parentheses
	 * delimit branches, and with an unconstrained class the name in the second string below reads as
	 * {@code Man(} - which only happens for a residue with <em>no ring form</em>, so it looks safe
	 * until it is not.
	 */
	@Test
	public void aBranchIsStillABranch() throws Exception {
		assertReopensUnchanged("freeEnd--?b1D-Man,p(--3a1D-Man,p)--4b1D-Man,p$MONO,perMe,Na,0,freeEnd");
		assertReopensUnchanged("freeEnd--?b1D-Man(--3a1D-Man,p)--4b1D-Man,p$MONO,perMe,Na,0,freeEnd");
		assertReopensUnchanged("freeEnd--??1D-GlcNAc,p--??1D-gro-D-manHep,p(--3a1D-Man,p)"
				+ "--4b1D-Gal,p$MONO,Und,0,0,freeEnd");
	}

	/**
	 * The pre-2007 linkage form is still read as a linkage and not swallowed into the name.
	 *
	 * <p>{@code -[1-9N?]} is how linkages were written before {@code --} replaced them. So a {@code -}
	 * is admitted into a name only when what follows is not {@code -}, a digit, {@code N} or
	 * {@code ?}. No name in either dictionary has a {@code -} followed by one of those, so the
	 * exclusion costs nothing, and this test is what it buys.
	 *
	 * <p>This pins the grammar alone, through {@code readSubtree}. Whether the form then loads as a
	 * whole structure is {@link PreDoubleDashLinkagesLoadTest}'s question (#274).
	 */
	@Test
	public void theOldStyleLinkageIsStillReadAsALinkage() throws Exception {
		Residue root = GWSParser.readSubtree("freeEnd-1b1D-GlcNAc,p", false);

		assertNotNull("the pre-2007 linkage form no longer parses", root);
		assertEquals("freeEnd", root.getTypeName());
		assertEquals("the linkage was swallowed into the name", 1, root.getChildrenLinkages().size());
		assertEquals("GlcNAc", root.getChildrenLinkages().get(0).getChildResidue().getTypeName());
		assertEquals('1', root.getChildrenLinkages().get(0).getParentPositionsSingle());
	}

	/**
	 * A residue whose name begins with a configuration, with none of its own, is read as itself and not
	 * as that configuration on an unknown residue (#252).
	 */
	@Test
	public void aConfigurationThatBelongsToTheNameStaysInTheName() throws Exception {
		Glycan unknown = Glycan.fromString("freeEnd--??1D-GlcNAc,p--??1D-gro-D-galHep,p$MONO,Und,0,0,freeEnd");
		Residue heptose = unknown.getRoot().getChildAt(0).getChildAt(0);
		assertEquals("D-gro-D-galHep", heptose.getTypeName());
		assertEquals('?', heptose.getChirality());
		assertTrue("read as an unknown residue", heptose.isSaccharide());

		// with a configuration of its own, as before
		Glycan known = Glycan.fromString("freeEnd--??1D-GlcNAc,p--??1L-D-gro-D-galHep,p$MONO,Und,0,0,freeEnd");
		heptose = known.getRoot().getChildAt(0).getChildAt(0);
		assertEquals("D-gro-D-galHep", heptose.getTypeName());
		assertEquals('L', heptose.getChirality());

		// and a configuration on a name that is a residue without it is a configuration, as it always was
		Residue glcNAc = unknown.getRoot().getChildAt(0);
		assertEquals("GlcNAc", glcNAc.getTypeName());
		assertEquals('D', glcNAc.getChirality());
	}

	private static boolean readsAs(Glycan _read, String _typeName) {
		for (Residue r : _read.getAllResidues())
			if (_typeName.equals(r.getTypeName())) return true;
		return false;
	}

	private static List<String> residueNames(Glycan _read) {
		List<String> names = new ArrayList<String>();
		for (Residue r : _read.getAllResidues()) names.add(r.getTypeName());
		return names;
	}

	private void assertReopensUnchanged(String _gws) throws Exception {
		Glycan read = Glycan.fromString(_gws);
		assertNotNull("cannot be read at all: " + _gws, read);
		assertEquals("does not come back as itself", _gws, read.toString());
	}

	/** This residue on a free reducing end, as the writer writes it. */
	private String writeAlone(ResidueType _type) throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		root.addChild(ResidueDictionary.newResidue(_type.getName()));
		return new Glycan(root, false, new MassOptions()).toString();
	}
}
