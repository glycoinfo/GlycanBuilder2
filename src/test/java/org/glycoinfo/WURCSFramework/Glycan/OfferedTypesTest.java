package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.TreeSet;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.CoreType;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.OfferedTypes;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * What the menus, galleries and the monosaccharide chooser leave out, because it cannot be written to
 * WURCS once drawn (#315).
 *
 * <p>The lists are written out so that a change either way is noticed. One that starts to write - GB3
 * settling dHexA, say - fails here, and is the moment to take it off the list: the menus will already
 * offer it again, since {@link OfferedTypes} asks the writer.
 */
public class OfferedTypesTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** The residue menus and galleries: four left out; the Assigned placeholder kept. */
	@Test
	public void theResidueMenusLeaveOutFour() {
		TreeSet<String> withheld = new TreeSet<String>();
		for (String superclass : ResidueDictionary.getSuperclasses())
			for (ResidueType type : ResidueDictionary.getResidues(superclass))
				if (!OfferedTypes.residue(type)) withheld.add(type.getName());

		assertEquals(new TreeSet<String>(Arrays.asList("Hept", "Ino", "Unknown", "dHexA")), withheld);
	}

	/** The placeholder cannot be written, and is kept: it is a step on the way to a residue (#252). */
	@Test
	public void thePlaceholderIsKept() {
		assertTrue(OfferedTypes.residue(ResidueDictionary.findResidueType("Assigned")));
	}

	/**
	 * The structure templates: every one is offered. Eight GAG repeats were left out until #330, sulfated
	 * where no hydroxyl is free - a dermatan GalNAc at C5, its ring carbon, and a heparan GlcN at C4,
	 * where the next GlcA is bonded.
	 */
	@Test
	public void theTemplatesAreAllOffered() {
		TreeSet<String> withheld = new TreeSet<String>();
		for (CoreType core : CoreDictionary.getCores())
			if (!OfferedTypes.core(core)) withheld.add(core.getName());

		assertEquals(new TreeSet<String>(), withheld);
	}

	/**
	 * The residues with no symbol of their own: 48 the writer has no skeleton for, or writes a WURCS
	 * no reader takes back. Gro among them on its own; glycerol, Gro made an alditol, is offered apart
	 * and writes. Since #273, galNon too, which writes Aad4334xh, and araOct and manOct, which write
	 * now that their anomeric carbon is ? and are read back by nothing until GB3 (#298).
	 *
	 * <p>60 until #275 recorded the stereo of the heptoses and octuloses named by two prefixes: ten
	 * heptoses and the two octuloses are written and read back, and offered. gro-D-manHep, of unknown
	 * C6, is written as a1122xh, which the reader refuses; gro-D-glcHep's a2122xh reads as Hept.
	 */
	@Test
	public void theChooserLeavesOutFortyEight() {
		TreeSet<String> withheld = new TreeSet<String>();
		for (ResidueType type : NonSymbolicResidueDictionary.getResidues())
			if (!type.getName().startsWith("#") && !ResidueDictionary.hasResidueType(type.getName())
					&& !OfferedTypes.residue(type))
				withheld.add(type.getName());

		assertEquals(new TreeSet<String>(Arrays.asList(
				"Gro", "Eru",
				"2dPen", "4dPen", "4dPenN", "dRib", "Rul", "Xul",
				"4uGalA", "4uGlcA", "4uHexA", "4uIdoA", "4uManA", "Ami", "Asc", "Boi", "Cym", "GalNA", "GlcNA",
				"HexNA", "IdoNA", "ManNA", "MeH", "Rho",
				"gro-D-manHep", "Ole", "The",
				"araOct", "manOct", "galNon",
				"8eLeg", "D-gro-D-manNon", "D-gro-D-talNon", "D-gro-L-altNon", "D-gro-L-galNon", "D-gro-L-manNon",
				"Ko", "L-gro-D-galNon", "L-gro-D-manNon", "L-gro-D-talNon", "L-gro-L-altNon", "L-gro-L-glcNon",
				"L-gro-L-manNon", "NeuAcLac", "NeuGcLac",
				"D-ery-D-glcDEC", "D-ery-L-glcDEC",
				"Sugar")), withheld);
		assertEquals(48, withheld.size());
	}
}
