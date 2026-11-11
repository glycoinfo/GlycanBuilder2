package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Which carbons a substituent can be put on (#320, #321, #323).
 *
 * <p>The rows' positions were corrected by one rule, the maintainer's (2026-11-10): a carbon with a
 * hydroxyl, or the residue's own amine, and not a carbon with no hydroxyl or one the residue's name
 * already uses. They offered a 6-deoxy's C6 and a nonulosonate's C3, which gave WURCS no validator
 * accepts; a FucNAc's C2 and a Mur's C3, which failed with "Invalide residue."; and not the amines of
 * Leg, Pse, Aci and Bac, whose acyls the reader then dropped without a word.
 */
public class PositionsASubstituentCanTakeTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** No hydroxyl, no substituent: a 6-deoxy's C6, a nonulosonate's C3, a 2-deoxy's C2. */
	@Test
	public void aCarbonWithNoHydroxylTakesNoSubstituent() throws Exception {
		assertRefused("Fuc", '6', "Me");
		assertRefused("Rha", '6', "S");
		assertRefused("NeuAc", '3', "Ac");
		assertRefused("Leg", '9', "Me");
		assertRefused("Dig", '2', "Me");
		assertAccepted("Fuc", '4', "S");
		assertAccepted("NeuAc", '9', "Ac");
	}

	/** A carbon the name already uses: a FucNAc's C2, a Mur's C3. */
	@Test
	public void aCarbonTheNameUsesTakesNoSecondGroup() throws Exception {
		assertRefused("FucNAc", '2', "Me");
		assertRefused("Mur", '3', "S");
	}

	/** An amine takes an acyl, as GlcN's always has, and not a second nitrogen, a phosphate or a sugar. */
	@Test
	public void anAmineTakesAnAcylAndNothingThatCannotBeWritten() throws Exception {
		assertAccepted("Leg", '5', "Ac");
		assertAccepted("Pse", '7', "Ac");
		assertAccepted("Bac", '4', "Ac");
		assertRefused("Leg", '5', "NAc");
		assertRefused("GlcN", '2', "P");
		assertRefused("Bac", '2', "NS");
		assertFalse("a sugar at an amine",
				ResidueDictionary.newResidue("Leg").addChild(ResidueDictionary.newResidue("Gal"), '5'));
	}

	/** And the reader keeps the acyls on those amines: Leg5,7Ac2 was read as a bare Leg, 84 Da light. */
	@Test
	public void theAcylsOnLegAndPseAreRead() throws Exception {
		for (String wurcs : new String[] { "WURCS=2.0/1,1,0/[Aad21122m-2a_2-6_5*NCC/3=O_7*NCC/3=O]/1/",
				"WURCS=2.0/1,1,0/[Aad22111m-2a_2-6_5*NCC/3=O_7*NCC/3=O]/1/" }) {
			Glycan read = new WURCS2Parser().readGlycan(wurcs, workspace.getDefaultMassOptions());
			assertEquals(wurcs, WURCSMassCalculator.calcMassWURCS(new WURCSFactory(wurcs).getArray()).doubleValue(),
					read.computeMass(), 0.0001);
			assertEquals(wurcs, wurcs, new WURCS2Parser().toWURCS(read));
		}
	}

	/** A structure saved before, with a group where none can be, is refused with the reason, not written wrong. */
	@Test
	public void anOlderFileIsRefusedWithTheReason() throws Exception {
		Glycan saved = Glycan.fromString("freeEnd--1a1L-Fuc,p--6?1Me$MONO,Und,0,0,freeEnd");
		try {
			new WURCS2Parser().toWURCS(saved);
			fail("a methyl on Fuc's 6 was written");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains("Me at 6 of Fuc"));
		}
	}

	private static void assertRefused(String _residue, char _position, String _substituent) throws Exception {
		assertFalse(_substituent + " at " + _position + " of " + _residue + " was accepted",
				ResidueDictionary.newResidue(_residue).addChild(ResidueDictionary.newResidue(_substituent), _position));
	}

	private static void assertAccepted(String _residue, char _position, String _substituent) throws Exception {
		assertTrue(_substituent + " at " + _position + " of " + _residue + " was refused",
				ResidueDictionary.newResidue(_residue).addChild(ResidueDictionary.newResidue(_substituent), _position));
	}
}
