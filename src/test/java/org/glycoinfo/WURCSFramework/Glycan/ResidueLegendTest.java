package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The legend that identifies a residue drawn without a symbol of its own.
 *
 * <p>It used to be written onto the residue's type, which is the instance the dictionary holds and
 * every residue of that name shares. So reading a second structure changed what the first one said,
 * and the type's own description - the one the menus show - was destroyed for the rest of the
 * session. The legend belongs to the residue.
 */
public class ResidueLegendTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Two structures of the same residue type, held at once, each keeping its own legend. */
	@Test
	public void readingOneStructureDoesNotChangeAnother() throws Exception {
		Glycan alpha = read("WURCS=2.0/1,1,0/[a21d2h-1a_1-5]/1/");
		Glycan beta = read("WURCS=2.0/1,1,0/[a21d2h-1b_1-5]/1/");

		assertEquals("α-4-deoxy-D-xylHexp", legendOf(alpha));
		assertEquals("β-4-deoxy-D-xylHexp", legendOf(beta));
	}

	/** And the dictionary keeps the description it was loaded with, which is what the menus show. */
	@Test
	public void theResidueTypeKeepsItsOwnDescription() throws Exception {
		read("WURCS=2.0/1,1,0/[a21d2h-1a_1-5]/1/");

		assertEquals("XyloHexose",
				NonSymbolicResidueDictionary.findResidueType("xylHex").getDescription());
	}

	/** A residue with nothing of its own to say falls back to what its type is called. */
	@Test
	public void aResidueWithNoLegendOfItsOwnUsesItsTypes() throws Exception {
		assertEquals("Glucose", legendOf(read("WURCS=2.0/1,1,0/[a2122h-1x_1-5]/1/")));
	}

	private String legendOf(Glycan _glycan) {
		for (Residue residue : _glycan.getAllResidues())
			if (residue.isSaccharide()) return residue.getLegend();

		return null;
	}

	private Glycan read(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, new MassOptions());
	}
}
