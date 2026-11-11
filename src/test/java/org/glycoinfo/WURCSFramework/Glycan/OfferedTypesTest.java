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

	/** The structure templates: eight GAG repeats, sulfated where no hydroxyl is free. */
	@Test
	public void theTemplatesLeaveOutEightGagRepeats() {
		TreeSet<String> withheld = new TreeSet<String>();
		for (CoreType core : CoreDictionary.getCores())
			if (!OfferedTypes.core(core)) withheld.add(core.getName());

		assertEquals(new TreeSet<String>(Arrays.asList("gagdermatan5", "gagdermatan45", "gagdermatan25",
				"gagdermatan245", "gagheparan24", "gagheparan224", "gagheparan246", "gagheparan2246")), withheld);
	}

	/**
	 * The residues with no symbol of their own: 60 the writer has no skeleton for. Gro among them on
	 * its own; glycerol, Gro made an alditol, is offered apart and writes.
	 */
	@Test
	public void theChooserLeavesOutSixty() {
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
				"?-gro-D-glcHep", "?-gro-D-manHep", "D-gro-D-altHep", "D-gro-D-galHep", "D-gro-D-glcHep",
				"D-gro-L-glcHep", "D-gro-L-manHep", "L-gro-D-galHep", "L-gro-D-glcHep", "L-gro-D-idoHep",
				"L-gro-L-manHep", "Ole", "The",
				"araOct", "D-gro-D-galOct", "D-gro-D-talOct", "L-gro-D-glcOct", "manOct",
				"8eLeg", "D-gro-D-manNon", "D-gro-D-talNon", "D-gro-L-altNon", "D-gro-L-galNon", "D-gro-L-manNon",
				"Ko", "L-gro-D-galNon", "L-gro-D-manNon", "L-gro-D-talNon", "L-gro-L-altNon", "L-gro-L-glcNon",
				"L-gro-L-manNon", "NeuAcLac", "NeuGcLac",
				"D-ery-D-glcDEC", "D-ery-L-glcDEC",
				"Sugar")), withheld);
		assertEquals(60, withheld.size());
	}
}
