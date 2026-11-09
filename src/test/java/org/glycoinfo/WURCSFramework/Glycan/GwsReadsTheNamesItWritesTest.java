package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
	 * Five names are still unreadable, and not one of them is a grammar problem. Widening the name
	 * class took this from thirty-five to these five, and each of the five is a <strong>bad row in
	 * conf/non_symbolic_residue_types</strong> that no reader can be written to accommodate:
	 *
	 * <ul>
	 * <li>{@code araOct} and {@code manOct} record their <em>anomeric carbon</em> as {@code -}, where
	 * a position or {@code ?} belongs, so the writer emits {@code --??-manOct,p} - a dash standing
	 * where the carbon should be.
	 * <li>{@code L-gro-D-glcOct} records its <em>chirality</em> as {@code /}, which the writer emits
	 * as {@code /-} and the reader reads as the start of a cleavage. Its synonym column says
	 * {@code D-gro-D-glcOct} and its description {@code D-glycero-L-glcoOctose}, so the row disagrees
	 * with itself three ways.
	 * <li>{@code ?-gro-D-manHep} and {@code ?-gro-D-glcHep} <em>begin</em> with {@code ?}, which marks
	 * an unknown position everywhere else in the format. A name cannot start with it and stay
	 * unambiguous; these two need either the escape of §1.3.1 or a different name.
	 * </ul>
	 *
	 * <p>All five are data to correct rather than code, and correcting them is a chemistry call, so
	 * they are recorded here and raised rather than quietly edited. This assertion is on the exact
	 * set: a sixth name appearing fails the test, and a name leaving it fails it too, so neither can
	 * pass unnoticed.
	 */
	private static final List<String> KNOWN_BAD_ROWS = java.util.Arrays.asList(
			"?-gro-D-glcHep", "?-gro-D-manHep", "L-gro-D-glcOct", "araOct", "manOct");

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
	 * <p><strong>The form does not actually load, and did not before this change either.</strong> The
	 * grammar splits it correctly and then {@code LinkageTypeOptimizer.start} throws a
	 * {@code NullPointerException}, because the old-style branch builds a {@code Linkage} with no
	 * parent. Measured on 1.40.0 before the grammar was touched, so it is a defect of its own and not
	 * a regression of this one - filed separately. What this test pins is the distinction that
	 * matters here: the failure is in building the structure, never in reading the name. If the
	 * grammar regressed, the message would be a format error instead.
	 */
	@Test
	public void theOldStyleLinkageIsStillReadAsALinkage() throws Exception {
		// readSubtree, not fromString: the grammar is what is under test, and fromString would go on
		// to the structure build that throws for reasons of its own.
		Residue root = GWSParser.readSubtree("freeEnd-1b1D-GlcNAc,p", false);

		assertNotNull("the pre-2007 linkage form no longer parses", root);
		assertEquals("freeEnd", root.getTypeName());
		assertEquals("the linkage was swallowed into the name", 1, root.getChildrenLinkages().size());
		assertEquals("GlcNAc", root.getChildrenLinkages().get(0).getChildResidue().getTypeName());
		assertEquals('1', root.getChildrenLinkages().get(0).getParentPositionsSingle());
	}

	/**
	 * And the form still does not <em>load</em>, which is a separate defect this change does not
	 * touch.
	 *
	 * <p>Measured on 1.40.0 before the grammar was widened: the tree above is built, and then
	 * {@code LinkageTypeOptimizer.start} throws a {@code NullPointerException} on a linkage with no
	 * parent, so {@code fromString} hands back null. Recorded here rather than left to be rediscovered
	 * — a row that goes from fail to pass is the evidence the defect is fixed.
	 */
	@Test
	public void andStillDoesNotLoad() throws Exception {
		assertNull("the pre-2007 linkage form loads now - delete this test and close the issue",
				Glycan.fromString("freeEnd-1b1D-GlcNAc,p$MONO,Und,0,0,freeEnd"));
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
