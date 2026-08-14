package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That copying a structure copies all of it (#211).
 *
 * <p>{@code cloneSubtree} rebuilt the copy through {@code addChild}, so once #34 taught {@code addChild}
 * to refuse a position another child holds, a structure holding two children at one position lost one
 * <em>on the way through the copy</em> — and a clone has nowhere to report, so it said nothing.
 * {@code Glycan.clone()} takes the same route, which is copy-and-paste, undo and redo.
 *
 * <p>Measured before the fix: 1.35.2 and 1.36.0 copied both children, 1.37.0 and 1.38.0 copied one.
 *
 * <p><b>The distinction is between validating a change and reproducing a fact.</b> A copy makes no new
 * claim about a molecule — the question was settled when the original was made — so the rules that
 * decide what may be attached do not apply to it. Reading a file has always been on that side of the
 * line: #34 kept documents containing such a structure openable. Copying belongs there too.
 */
public class CopyingLosesNothingTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Two substituents at one position survive a subtree copy. */
	@Test
	public void aSubtreeCopyKeepsBothChildrenAtOnePosition() throws Exception {
		Residue glc = glcWithSubstituentsAt('2', "N", "Ac");
		assertEquals("the structure under test was not built", 2, glc.getNoChildren());

		assertEquals("the copy lost a substituent", 2, glc.cloneSubtree().getNoChildren());
	}

	/** And so do more than two — nothing here counts to two. */
	@Test
	public void andMoreThanTwo() throws Exception {
		Residue glc = glcWithSubstituentsAt('2', "N", "Ac", "Me");
		assertEquals("the structure under test was not built", 3, glc.getNoChildren());

		assertEquals("the copy lost a substituent", 3, glc.cloneSubtree().getNoChildren());
	}

	/**
	 * And the whole document copies to the same sequence, which is the form the loss reached a user in:
	 * copy-and-paste and undo both go through {@code Glycan.clone()}.
	 */
	@Test
	public void aDocumentCopyWritesTheSameSequence() throws Exception {
		Glycan structure = wrap(glcWithSubstituentsAt('2', "N", "Ac"));

		assertEquals("copying the document changed it",
				structure.toString(), structure.clone().toString());
	}

	/**
	 * Copying does not become a way to build what the rules refuse: the copy is faithful, and adding to
	 * it afterwards is still checked.
	 */
	@Test
	public void copyingIsNotAWayAroundTheRules() throws Exception {
		Residue copy = glcWithSubstituentsAt('2', "N", "Ac").cloneSubtree();

		assertEquals("a further substituent at the same position should still be refused",
				false, copy.addChild(ResidueDictionary.newResidue("Me"), '2'));
	}

	/**
	 * A Glc carrying several substituents at one position.
	 *
	 * <p>Built the way the canvas does it — attach, then set the position — because {@code addChild}
	 * with the position stated is exactly what refuses the second one. That is the state a document read
	 * from a file arrives in, and #34 was explicit that such a file still opens.
	 */
	private static Residue glcWithSubstituentsAt(char position, String... substituents)
			throws Exception {
		Residue glc = ResidueDictionary.newResidue("Glc");

		for (String each : substituents) {
			Residue substituent = ResidueDictionary.newResidue(each);
			glc.addChild(substituent);
			substituent.getParentLinkage().setLinkagePositions(new char[] {position});
		}

		return glc;
	}

	private static Glycan wrap(Residue residue) throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		root.addChild(residue);

		return new Glycan(root, false, MassOptions.empty());
	}
}
