package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A generic residue drawn as D or L keeps it in WURCS (#314).
 *
 * <p>The generic residues - Hex, HexA, HexN, HexNAc, Pen, Non, dHex, dHexNAc, ddHex, ddNon - have no
 * stereo recorded, and were written with {@code x} at every stereocentre whatever was drawn, so a
 * D-Hex came back as a Hex of no configuration. D and L are the configuration of the highest-numbered
 * stereocentre, and WURCS writes it there: {@code axxx2h} for a D-hexose, {@code axxx1h} for an L.
 */
public class GenericResidueKeepsItsConfigurationTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** Each generic residue, drawn D, L and of unknown configuration: what it writes. */
	private static final String[][] WRITTEN = {
			{ "Hex", "[axxx2h-1a_1-5]", "[axxx1h-1a_1-5]", "[axxxxh-1a_1-5]" },
			{ "HexA", "[axxx2A-1a_1-5]", "[axxx1A-1a_1-5]", "[axxxxA-1a_1-5]" },
			{ "HexN", "[axxx2h-1a_1-5_2*N]", "[axxx1h-1a_1-5_2*N]", "[axxxxh-1a_1-5_2*N]" },
			{ "HexNAc", "[axxx2h-1a_1-5_2*NCC/3=O]", "[axxx1h-1a_1-5_2*NCC/3=O]", "[axxxxh-1a_1-5_2*NCC/3=O]" },
			{ "Pen", "[axx2h-1a_1-5]", "[axx1h-1a_1-5]", "[axxxh-1a_1-5]" },
			{ "Non", "[Aadxxxx2h-2a_2-6]", "[Aadxxxx1h-2a_2-6]", "[Aadxxxxxh-2a_2-6]" },
			{ "dHex", "[axxx2m-1a_1-5]", "[axxx1m-1a_1-5]", "[axxxxm-1a_1-5]" },
			{ "dHexNAc", "[axxx2m-1a_1-5_2*NCC/3=O]", "[axxx1m-1a_1-5_2*NCC/3=O]", "[axxxxm-1a_1-5_2*NCC/3=O]" },
			{ "ddHex", "[adxx2m-1a_1-5]", "[adxx1m-1a_1-5]", "[adxxxm-1a_1-5]" },
			{ "ddNon", "[Aadxxxx2m-2a_2-6_5*N_7*N]", "[Aadxxxx1m-2a_2-6_5*N_7*N]", "[Aadxxxxxm-2a_2-6_5*N_7*N]" },
	};

	private static final String[] CONFIGURATIONS = { "D", "L", "?" };

	@Test
	public void eachIsWrittenInTheConfigurationDrawn() throws Exception {
		List<String> wrong = new ArrayList<String>();
		for (String[] row : WRITTEN) {
			for (int i = 0; i < CONFIGURATIONS.length; i++) {
				String written = new WURCS2Parser().toWURCS(Glycan.fromString(gws(row[0], CONFIGURATIONS[i])));
				String expected = "WURCS=2.0/1,1,0/" + row[i + 1] + "/1/";
				if (!expected.equals(written)) wrong.add(CONFIGURATIONS[i] + "-" + row[0] + ": " + written);
			}
		}
		assertEquals(wrong.toString(), 0, wrong.size());
	}

	@Test
	public void eachIsReadBackInTheConfigurationWritten() throws Exception {
		List<String> wrong = new ArrayList<String>();
		for (String[] row : WRITTEN) {
			for (int i = 0; i < CONFIGURATIONS.length; i++) {
				Residue read = new WURCS2Parser().readGlycan("WURCS=2.0/1,1,0/" + row[i + 1] + "/1/", new MassOptions())
						.getRoot().firstChild();
				String got = read.getTypeName() + " " + read.getChirality();
				if (!got.equals(row[0] + " " + CONFIGURATIONS[i])) wrong.add(row[i + 1] + " read as " + got);
			}
		}
		assertEquals(wrong.toString(), 0, wrong.size());
	}

	/**
	 * A dHexNAc taken from the menu has no configuration, as the other generic residues have none: its
	 * row gave it a D, which it would now have written as axxx2m. Corrected to ? (the maintainer,
	 * 2026-11-28).
	 */
	@Test
	public void aDHexNAcFromTheMenuHasNoConfiguration() throws Exception {
		Residue fromTheMenu = ResidueDictionary.newResidue("dHexNAc");

		assertEquals('?', fromTheMenu.getChirality());
		assertEquals("WURCS=2.0/1,1,0/[axxxxm-1x_1-5_2*NCC/3=O]/1/",
				new WURCS2Parser().toWURCS(new Glycan(fromTheMenu, true, new MassOptions())));
	}

	private static String gws(String _type, String _configuration) {
		String anomer = _type.endsWith("Non") ? "?a2" : "?a1";
		String configuration = _configuration.equals("?") ? "" : _configuration + "-";
		return "freeEnd--" + anomer + configuration + _type + ",p$MONO,Und,0,0,freeEnd";
	}
}
