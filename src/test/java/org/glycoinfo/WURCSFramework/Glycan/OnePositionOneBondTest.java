package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That the same linkage position cannot be given to two children (#34).
 *
 * <p>A carbon carries one glycosidic bond. The position list offered when a linkage is edited was
 * built from what the residue type allows without asking what the residue's other children had
 * already taken, so the same position could be handed out twice - a Man with two branches both at 4,
 * which is not a molecule. It draws, and the WURCS export then writes nothing, which is how it was
 * usually found out.
 */
public class OnePositionOneBondTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** A position another child holds is refused. */
	@Test
	public void aPositionIsNotGivenTwice() throws Exception {
		Residue parent = manOnAFreeEnd();

		assertTrue(parent.addChild(ResidueDictionary.newResidue("Gal"), '4'));
		assertFalse("a second child was added at position 4",
				parent.addChild(ResidueDictionary.newResidue("Glc"), '4'));
		assertFalse("canAddChild should say so too",
				parent.canAddChild(ResidueDictionary.newResidue("Glc"), '4'));

		assertEquals(1, parent.getNoChildren());
	}

	/** Other positions are unaffected, which is most of what anyone does. */
	@Test
	public void everyOtherPositionStillTakesABranch() throws Exception {
		Residue parent = manOnAFreeEnd();

		assertTrue(parent.addChild(ResidueDictionary.newResidue("Gal"), '2'));
		assertTrue(parent.addChild(ResidueDictionary.newResidue("Glc"), '3'));
		assertTrue(parent.addChild(ResidueDictionary.newResidue("Man"), '6'));

		assertEquals(3, parent.getNoChildren());
	}

	/**
	 * Two children at an unknown position are not a conflict.
	 *
	 * <p>Most of what comes out of a database says only that a residue is attached somewhere. If
	 * two of those were treated as the same position, ordinary structures would stop being
	 * drawable - and nothing would have been said about where either one is.
	 */
	@Test
	public void twoUnknownPositionsAreNotTheSamePosition() throws Exception {
		Residue parent = manOnAFreeEnd();

		assertTrue(parent.addChild(ResidueDictionary.newResidue("Fuc"), '?'));
		assertTrue("an unknown position is not a claim on any position",
				parent.addChild(ResidueDictionary.newResidue("Xyl"), '?'));

		assertEquals(2, parent.getNoChildren());
	}

	/**
	 * A file already containing the impossible structure still opens.
	 *
	 * <p>The point is to stop one being made, not to make saved work unreadable - a structure drawn
	 * before this and saved is still somebody's, and refusing to open it would be a worse fault than
	 * the one being fixed.
	 */
	@Test
	public void aFileThatAlreadyHasOneStillOpens() throws Exception {
		GlycanDocument document =
				new BuilderWorkspace(new GlycanRendererAWT()).getStructures();

		assertTrue(document.importFromString(
				"freeEnd--??1D-Man,p(--4?1D-Gal,p)--4?1D-Glc,p$MONO,perMe,Na,0,freeEnd", "gws"));
		assertEquals(1, document.getStructures().size());
	}

	private static Residue manOnAFreeEnd() throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		Residue man = ResidueDictionary.newResidue("Man");
		root.addChild(man);

		return man;
	}

	/**
	 * A substituent occupies a position exactly as a monosaccharide does.
	 *
	 * <p>The rule is about the carbon, not about what kind of thing is hanging off it: a methyl at 4
	 * and a branch at 4 are the same claim on the same atom. Substituents are attached as child
	 * residues here, so they were already covered - this holds that they stay covered, in both
	 * orders and between two substituents, since none of that is obvious from the code that does it.
	 */
	@Test
	public void aSubstituentTakesAPositionTooAndInEitherOrder() throws Exception {
		Residue afterSugar = manOnAFreeEnd();
		afterSugar.addChild(ResidueDictionary.newResidue("Gal"), '4');
		assertFalse("a methyl was added onto a position a branch already holds",
				afterSugar.addChild(ResidueDictionary.newResidue("Me"), '4'));

		Residue afterSubstituent = manOnAFreeEnd();
		afterSubstituent.addChild(ResidueDictionary.newResidue("Me"), '4');
		assertFalse("a branch was added onto a position a methyl already holds",
				afterSubstituent.addChild(ResidueDictionary.newResidue("Gal"), '4'));

		Residue betweenSubstituents = manOnAFreeEnd();
		betweenSubstituents.addChild(ResidueDictionary.newResidue("Me"), '4');
		assertFalse("two substituents were put on the same position",
				betweenSubstituents.addChild(ResidueDictionary.newResidue("S"), '4'));
	}

	/**
	 * And several substituents on their own positions are untouched, which is the case from #66 -
	 * the heavily modified fucose that started this line of work.
	 */
	@Test
	public void substituentsOnDifferentPositionsAreFine() throws Exception {
		Residue fucose = manOnAFreeEnd();

		assertTrue(fucose.addChild(ResidueDictionary.newResidue("Me"), '2'));
		assertTrue(fucose.addChild(ResidueDictionary.newResidue("N"), '3'));
		assertTrue(fucose.addChild(ResidueDictionary.newResidue("Me"), '4'));

		assertEquals(3, fucose.getNoChildren());
	}

	/** An unknown position does not block a stated one, whichever arrived first. */
	@Test
	public void anUnknownPositionBlocksNothing() throws Exception {
		Residue parent = manOnAFreeEnd();
		parent.addChild(ResidueDictionary.newResidue("S"), '?');

		assertTrue("a sulfate of unknown position should not close position 4",
				parent.addChild(ResidueDictionary.newResidue("Gal"), '4'));
	}
}
