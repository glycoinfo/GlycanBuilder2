package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The residue class "Unknown" is shown as "Others" (#182).
 *
 * <p>It holds the monosaccharides SNFG draws as flat hexagons - Kdo, Bac, Mur and the rest - and the
 * Unknown placeholder. SNFG's "Unknown" is the white flat hexagon alone; the menu called Kdo unknown.
 * Only the label changes, so everything that looks the class up by its name still finds it.
 */
public class UnknownClassShownAsOthersTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	@Test
	public void theClassIsShownAsOthers() {
		assertEquals("Others", ResidueDictionary.displayNameOf("Unknown"));
		assertEquals("Hexose", ResidueDictionary.displayNameOf("Hexose"));

		List<String> shown = new ArrayList<String>();
		for (String superclass : ResidueDictionary.getSuperclasses())
			shown.add(ResidueDictionary.displayNameOf(superclass));
		assertTrue(shown.toString(), shown.contains("Others"));
		assertFalse(shown.toString(), shown.contains("Unknown"));
	}

	/** The class keeps its name: Kdo is still found under "Unknown", and the placeholder too. */
	@Test
	public void theClassKeepsItsName() {
		List<String> members = new ArrayList<String>();
		for (ResidueType type : ResidueDictionary.getResidues("Unknown")) members.add(type.getName());
		assertTrue(members.toString(), members.contains("Kdo"));
		assertTrue(members.toString(), members.contains("Unknown"));
	}
}
