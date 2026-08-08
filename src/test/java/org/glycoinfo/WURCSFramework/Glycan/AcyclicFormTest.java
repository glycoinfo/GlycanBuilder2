package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The two acyclic forms a sugar can be drawn in, and what a reducing end has to do with them.
 *
 * <p>A sugar is acyclic when it has been reduced, and it is reduced by its reducing end - the
 * alditol marker, and every one of the labels, each of which is a reductive amination. That fact is
 * carried on the residue as {@code alditol} and {@code aldehyde}, and it is those flags rather than
 * the ring letter that the exporters read: a Glc flagged alditol writes {@code [h2122h]}, one
 * flagged aldehyde writes {@code [o2122h]}, and one flagged neither writes a ring.</p>
 *
 * <p>Three places wrote only half of it, and each is measured here.</p>
 */
public class AcyclicFormTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void newWorkspace() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setNotation("snfg");
	}

	/**
	 * A reducing end that reduces its sugar leaves it acyclic, which the WURCS then says. Only the
	 * alditol marker used to, so a 2AB wrote what a free reducing end wrote while weighing 120 Da
	 * more - two structures, one string.
	 */
	@Test
	public void everyReducingEndThatReducesItsSugarSaysSo() {
		assertEquals("freeEnd", "WURCS=2.0/1,1,0/[a2122h-1b_1-5]/1/", wurcsWithReducingEnd("freeEnd"));

		for (String reduced : new String[] {"redEnd", "PA", "2AB", "AA", "DH"}) {
			assertEquals(reduced, "WURCS=2.0/1,1,0/[h2122h]/1/", wurcsWithReducingEnd(reduced));
		}
	}

	/** And the masses differ, which is what says the one string was wrong rather than the drawing. */
	@Test
	public void theyDifferInMassByTheirOwnLabel() {
		double free = massWithReducingEnd("freeEnd");

		assertEquals("the alditol adds 2H", 2.0157, massWithReducingEnd("redEnd") - free, 0.001);
		assertEquals("2AB", 120.0687, massWithReducingEnd("2AB") - free, 0.001);
		assertEquals("PA", 78.0582, massWithReducingEnd("PA") - free, 0.001);
	}

	/**
	 * Setting the ring letter sets the flags that go with it. Every caller that is not the desktop
	 * canvas set only the letter, and the GWS parser is one of them - so a saved alditol came back
	 * as a ring, and undo, which goes through GWS, quietly un-reduced whatever it touched.
	 */
	@Test
	public void theRingLetterAndTheFlagsSayOneThing() {
		Residue sugar = ResidueDictionary.findResidueType("Glc") == null ? null : newGlc();

		sugar.setRingSize('o');
		assertTrue("alditol", sugar.isAlditol());
		assertFalse("not an aldehyde", sugar.isAldehyde());

		sugar.setRingSize('a');
		assertTrue("aldehyde", sugar.isAldehyde());
		assertFalse("not an alditol", sugar.isAlditol());

		sugar.setRingSize('p');
		assertFalse("a ring is neither", sugar.isAlditol());
		assertFalse("a ring is neither", sugar.isAldehyde());
	}

	/** So both acyclic forms survive being written to GWS and read back. */
	@Test
	public void bothAcyclicFormsSurviveGws() {
		assertEquals("alditol", "WURCS=2.0/1,1,0/[h2122h]/1/", afterGwsRoundTrip('o'));
		assertEquals("open chain", "WURCS=2.0/1,1,0/[o2122h]/1/", afterGwsRoundTrip('a'));
	}

	/**
	 * A label is still drawn on an acyclic sugar. In SNFG an acyclic sugar carries its own marker, so
	 * a free or reduced reducing end has nothing left to add beside it - but a label names what the
	 * sample was tagged with. Suppressing every acyclic sugar suppressed every label with them, and
	 * since every label reduces its sugar, that was all of them.
	 */
	@Test
	public void aLabelIsDrawnOnAnAcyclicSugarAndAPlainAlditolIsNot() {
		assertNotNull("2AB", reducingEndBox("2AB"));
		assertNotNull("PA", reducingEndBox("PA"));
		assertNotNull("a free reducing end", reducingEndBox("freeEnd"));

		assertEquals("the alditol marker is the sugar's own", null, reducingEndBox("redEnd"));
	}

	/**
	 * A structure built through the API rather than parsed has no bracket, and cloning one reached
	 * through it regardless - which took {@code computeMass(String)} with it, since that clones
	 * before it changes the isotope.
	 */
	@Test
	public void aStructureBuiltRatherThanParsedCanBeCloned() {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		root.addChild(newGlc());

		Glycan built = new Glycan(root, false, new MassOptions());

		assertNotNull(built.clone());
		assertTrue(built.computeMass(MassOptions.ISOTOPE_MONO) > 0);
	}

	private static Residue newGlc() {
		try {
			return ResidueDictionary.newResidue("Glc");
		} catch (Exception noSuchResidue) {
			throw new AssertionError(noSuchResidue);
		}
	}

	/** A Glc on the given reducing end, reached by changing it rather than by naming it in the GWS. */
	private static Glycan glcOn(String reducingEnd) {
		Glycan structure = Glycan.fromString("freeEnd--?b1D-Glc,p$MONO,Und,0,0,freeEnd");
		structure.setReducingEndType(ResidueDictionary.findResidueType(reducingEnd));

		return structure;
	}

	private static String wurcsWithReducingEnd(String reducingEnd) {
		return wurcsOf(glcOn(reducingEnd));
	}

	private static double massWithReducingEnd(String reducingEnd) {
		return glcOn(reducingEnd).computeMass();
	}

	/** What the structure writes after its ring letter has been set and it has been through GWS. */
	private static String afterGwsRoundTrip(char form) {
		Glycan structure = Glycan.fromString("freeEnd--?b1D-Glc,p$MONO,Und,0,0,freeEnd");
		structure.getRoot().getChildAt(0).setRingSize(form);

		Glycan readBack = Glycan.fromString(structure.toString());
		assertNotNull("GWS could not be read back for form " + form, readBack);

		return wurcsOf(readBack);
	}

	/** The box the layout gives the reducing end, or null when it draws none. */
	private static java.awt.Rectangle reducingEndBox(String reducingEnd) {
		Glycan structure = glcOn(reducingEnd);
		BBoxManager boxes = new BBoxManager();
		workspace.getGlycanRenderer().computeBoundingBoxes(
				Collections.singletonList(structure), false, true, new PositionManager(), boxes);

		return boxes.getComplete(structure.getRoot(true));
	}

	private static String wurcsOf(Glycan structure) {
		try {
			GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
			document.setStructures(List.of(structure));

			return document.toString("wurcs2").trim();
		} catch (Exception cannotBeWritten) {
			throw new AssertionError(cannotBeWritten);
		}
	}
}
