package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The mass of the generic deoxy-HexNAc, which was one oxygen short (#26).
 *
 * <p>A generic residue is the same sugar with the stereochemistry left unsaid, so it weighs exactly
 * what its specific forms weigh: dHex weighs what Fuc weighs, and dHexNAc has to weigh what FucNAc,
 * RhaNAc and QuiNAc weigh. It did not - the dictionary gave it C8H15NO4 where the deoxy form of
 * HexNAc (C8H15NO6) is C8H15NO5 - so a structure drawn with the generic was quietly 15.9949 lighter
 * than the same structure drawn with any specific residue it stands for.</p>
 */
public class DeoxyHexNAcMassTest {

	/** One oxygen, which is what a deoxy takes away and what the generic was missing. */
	private static final double OXYGEN = 15.9949;

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The generic weighs what every one of its specific forms weighs. */
	@Test
	public void theGenericWeighsWhatItsSpecificFormsWeigh() throws Exception {
		double generic = massOf("dHexNAc");

		assertEquals("FucNAc", massOf("FucNAc"), generic, 0.0001);
		assertEquals("RhaNAc", massOf("RhaNAc"), generic, 0.0001);
		assertEquals("QuiNAc", massOf("QuiNAc"), generic, 0.0001);
	}

	/** And a deoxy takes away one oxygen from its parent, no more. */
	@Test
	public void aDeoxyTakesAwayOneOxygen() throws Exception {
		assertEquals("dHexNAc is HexNAc less an oxygen",
				massOf("HexNAc") - OXYGEN, massOf("dHexNAc"), 0.0001);
		assertEquals("as dHex has always been Hex less an oxygen",
				massOf("Hex") - OXYGEN, massOf("dHex"), 0.0001);
	}

	private static double massOf(String name) throws Exception {
		ResidueType type = ResidueDictionary.getResidueType(name);

		return type.getResidueMassMain();
	}
}
