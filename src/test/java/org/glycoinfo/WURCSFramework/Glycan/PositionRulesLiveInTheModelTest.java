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
 * That the rules about which positions take a linkage are in the model, and are all of them.
 *
 * <p>They had been in three places: the residue type's list, which only the linkage dialog asked;
 * the ring and anomeric rules, which only that dialog knew; and what a sibling had taken, which only
 * the model knew. A structure drawn through the dialog therefore obeyed rules a structure built any
 * other way did not.
 *
 * <p>See {@code docs/linkage-positions-and-anomers.md}, which is where the chemistry these assert
 * is written down and attributed.
 */
public class PositionRulesLiveInTheModelTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** What the residue type already knows: a built-in substituent, and a sugar's size. */
	@Test
	public void theTypesOwnListIsObeyed() throws Exception {
		assertFalse("GlcNAc's 2 carries the N-acetyl",
				sugar("GlcNAc").addChild(ResidueDictionary.newResidue("Me"), '2'));
		assertFalse("a pentose has no 6",
				sugar("Xyl").addChild(ResidueDictionary.newResidue("Me"), '6'));
		assertFalse("Neu5Ac's 5 carries the N-acetyl",
				sugar("Neu5Ac").addChild(ResidueDictionary.newResidue("Me"), '5'));
	}

	/**
	 * But a built-in substituent does not always close its position.
	 *
	 * <p>GlcA's carboxyl at 6 leaves it open, because a carboxyl can be esterified. That is a
	 * chemical judgement made per residue rather than a rule to be derived, which is why the type's
	 * list is consulted rather than reasoned about - and this is the case that would be got wrong by
	 * reasoning.
	 */
	@Test
	public void aCarboxylDoesNotCloseItsPosition() throws Exception {
		assertTrue("GlcA's 6 should stay open for an ester",
				sugar("GlcA").addChild(ResidueDictionary.newResidue("Me"), '6'));
	}

	/** The ring oxygen occupies a position, and it takes no glycosidic bond. */
	@Test
	public void theRingClosesItsOwnPosition() throws Exception {
		Residue pyranose = sugar("Glc");
		pyranose.setRingSize('p');
		assertFalse("a pyranose closing from 1 has its 5 in the ring",
				pyranose.acceptsPosition('5', null));
		assertTrue(pyranose.acceptsPosition('4', null));

		Residue furanose = sugar("Glc");
		furanose.setRingSize('f');
		assertFalse("a furanose closing from 1 has its 4 in the ring",
				furanose.acceptsPosition('4', null));
		assertTrue(furanose.acceptsPosition('5', null));
	}

	/**
	 * An alditol has no ring and no anomeric centre, so its 1 is an ordinary hydroxyl.
	 *
	 * <p>The type's list does not offer 1, because in a ring the anomeric carbon is where the residue
	 * attaches to its parent. Reduced, there is no anomeric carbon and nothing is attached there.
	 */
	@Test
	public void anAlditolOpensPositionOne() throws Exception {
		Residue reduced = sugar("Glc");
		reduced.setAlditol(true);

		assertTrue("an alditol's 1 is an ordinary hydroxyl", reduced.acceptsPosition('1', null));
		assertTrue("and it has no ring to close a position", reduced.acceptsPosition('5', null));
	}

	/**
	 * A bridge is not an ordinary glycosidic bond and is not bound by the type's list.
	 *
	 * <p>1,6-anhydro attaches at the anomeric carbon, which no type's list offers. Enforcing the list
	 * against it stops the structure round-tripping - measured, before this was separated out.
	 */
	@Test
	public void aBridgeMayAttachWhereAnOrdinaryBondMayNot() throws Exception {
		GlycanDocument document =
				new BuilderWorkspace(new GlycanRendererAWT()).getStructures();

		assertTrue(document.importFromString("WURCS=2.0/1,1,0/[a2122h-1x_1-5_1-6]/1/", "wurcs2"));

		document.clearString();
		document.exportFromStructure(document.getStructures(), "wurcs2");
		assertEquals("1,6-anhydro should survive the round trip",
				"WURCS=2.0/1,1,0/[a2122h-1x_1-5_1-6]/1/",
				String.join("", document.getString()).trim());
	}

	/** A residue type with no list of its own constrains nothing, which 47 of the 134 do not have. */
	@Test
	public void noListMeansNoConstraintRatherThanNoPositions() throws Exception {
		Residue substituent = ResidueDictionary.newResidue("Me");

		assertTrue("a type with no position list should not refuse everything",
				substituent.availableLinkagePositions().length > 0);
	}

	private static Residue sugar(String name) throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		Residue residue = ResidueDictionary.newResidue(name);
		root.addChild(residue);

		return residue;
	}
}
