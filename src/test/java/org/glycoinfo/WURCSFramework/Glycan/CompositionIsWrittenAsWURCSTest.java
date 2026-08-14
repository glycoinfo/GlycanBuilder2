package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.composition.CompositionResidue;
import org.glycoinfo.application.glycanbuilder.composition.CompositionToWURCS;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a composition can be written as WURCS, and as the same WURCS everyone else writes (#107).
 *
 * <p>Exporting a composition used to produce an empty file. The writer returned nothing for a
 * composition on purpose - the ordinary encoder cannot express one, a composition having no linkages
 * to encode - and nothing was put in its place, so the export said neither what it had written nor
 * why it had not.
 *
 * <p>Composition WURCS is that place: it names the residues and says that nothing is known about
 * what joins them, and it is what GlyTouCan and GlyCosmos match a composition against.
 *
 * <p><b>The figures below are not this implementation's own.</b> They were taken from
 * <a href="https://gitlab.com/glycoinfo/glycompconverter">glycompconverter</a>, which is where the
 * definition of a composition is maintained, and pinned here. A test that only checked this code
 * against itself would hold nothing: what matters is that a composition written here is the same
 * string as a composition written anywhere else, because a WURCS that is correct but spelled
 * differently matches nothing in a database, and looks right while doing it.
 */
public class CompositionIsWrittenAsWURCSTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Each residue a composition can be counted in, against the reference implementation. */
	@Test
	public void everyResidueIsWrittenTheWayTheReferenceWritesIt() throws Exception {
		assertWrites("Hex", "WURCS=2.0/1,1,0/[axxxxh-1x_1-5]/1/");
		assertWrites("HexNAc", "WURCS=2.0/1,1,0/[axxxxh-1x_1-5_2*NCC/3=O]/1/");
		assertWrites("HexN", "WURCS=2.0/1,1,0/[axxxxh-1x_1-5_2*N]/1/");
		assertWrites("HexA", "WURCS=2.0/1,1,0/[axxxxA-1x_1-5]/1/");
		assertWrites("dHex", "WURCS=2.0/1,1,0/[axxxxm-1x_1-5]/1/");
		assertWrites("ddHex", "WURCS=2.0/1,1,0/[adxxxm-1x_1-5]/1/");
		assertWrites("Pen", "WURCS=2.0/1,1,0/[axxxh-1x_1-5]/1/");
		assertWrites("Neu5Ac", "WURCS=2.0/1,1,0/[Aad21122h-2x_2-6_5*NCC/3=O]/1/");
		assertWrites("Neu5Gc", "WURCS=2.0/1,1,0/[Aad21122h-2x_2-6_5*NCCO/3=O]/1/");
		assertWrites("Kdn", "WURCS=2.0/1,1,0/[Aad21122h-2x_2-6]/1/");
		assertWrites("Kdo", "WURCS=2.0/1,1,0/[Aad1122h-2x_2-6]/1/");
		assertWrites("MurNAc", "WURCS=2.0/1,1,0/[a2122h-1x_1-5_2*NCC/3=O_3*OC^RCO/4=O/3C]/1/");
	}

	/**
	 * Residues come out in the canonical order, whatever order they were counted in.
	 *
	 * <p>This is the assertion that would be hardest to get right by writing the string directly, and
	 * the reason none of this does. For Hex₅HexNAc₄Neu5Ac₂dHex₁ the canonical order is Neu5Ac, dHex,
	 * HexNAc, Hex - not alphabetical, not by count, not the order given. {@code WURCSFactory}
	 * normalizes, and what is held here is that it is left to.
	 */
	@Test
	public void theOrderIsCanonicalRatherThanTheOrderGiven() throws Exception {
		String written = write(counted("Hex", 5, "HexNAc", 4, "Neu5Ac", 2, "dHex", 1));

		assertTrue("the residues are not in canonical order: " + written,
				written.startsWith("WURCS=2.0/4,12,11/[Aad21122h-2x_2-6_5*NCC/3=O][axxxxm-1x_1-5]"
						+ "[axxxxh-1x_1-5_2*NCC/3=O][axxxxh-1x_1-5]/1-1-2-3-3-3-3-4-4-4-4-4/"));

		assertEquals("the order counted in should not change the answer",
				written, write(counted("dHex", 1, "Neu5Ac", 2, "HexNAc", 4, "Hex", 5)));
	}

	/** N residues carry N−1 unknown linkages, which is what says they are joined but not how. */
	@Test
	public void whatIsUnknownIsSaidRatherThanLeftOut() throws Exception {
		assertEquals("WURCS=2.0/1,2,1/[axxxxh-1x_1-5]/1-1/a?|b?}-{a?|b?",
				write(counted("Hex", 2)));
	}

	/** A composition of nothing is not a structure, and is not written as an empty one. */
	@Test
	public void nothingCountedIsNotAComposition() throws Exception {
		assertNull(write(new LinkedHashMap<CompositionResidue, Integer>()));
		assertNull(write(counted("Hex", 0)));
	}

	/**
	 * And the export itself writes it, which is what the issue was about.
	 *
	 * <p>Everything above tests the encoder; this tests that {@code exportFromStructure} reaches it.
	 * The export used to hand back an empty file for a composition, and an encoder nothing calls
	 * fixes nothing.
	 */
	@Test
	public void exportingACompositionWritesTheWURCS() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.HEX = 3;
		counts.HEXNAC = 2;

		MassOptions neutral = new MassOptions();
		neutral.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		neutral.ION_CLOUD = new IonCloud();

		GlycanDocument document =
				new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		Glycan composition = counts.getCompositionAsGlycan(neutral);
		document.addStructure(composition);
		document.clearString();
		document.exportFromStructure(document.getStructures(), "wurcs2");

		String written = String.join("", document.getString()).trim();

		assertNotNull(written);
		if (written.isEmpty()) fail("the export wrote nothing for a composition");
		assertTrue(written, written.startsWith("WURCS=2.0/2,5,4/"));
	}

	private static void assertWrites(String residue, String expected) throws Exception {
		assertEquals(residue, expected, write(counted(residue, 1)));
	}

	private static String write(Map<CompositionResidue, Integer> counted) throws Exception {
		return CompositionToWURCS.encode(counted);
	}

	/** {@code counted("Hex", 3, "HexNAc", 2)}. */
	private static Map<CompositionResidue, Integer> counted(Object... nameThenCount) {
		Map<CompositionResidue, Integer> counted = new LinkedHashMap<CompositionResidue, Integer>();
		for (int at = 0; at < nameThenCount.length; at += 2) {
			CompositionResidue residue = CompositionResidue.named((String) nameThenCount[at]);
			assertNotNull("no such composition residue: " + nameThenCount[at], residue);
			counted.put(residue, (Integer) nameThenCount[at + 1]);
		}

		return counted;
	}
}
