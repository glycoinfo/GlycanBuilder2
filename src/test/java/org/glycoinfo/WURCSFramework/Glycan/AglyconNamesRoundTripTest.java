package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.renderutil.AbstractGlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.AglyconInWURCS;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * An aglycon of the user's - Ser, Ser/Thr, a ceramide - is kept by the name it was given, and GWS
 * writes it so and reads it back (#305).
 *
 * <p>A reducing end was read with the grammar of a residue, which took the name apart: "/" began a
 * cleavage, "D-" a configuration, "(" a branch. "Ser/Thr=87.0320u" was written and could not be
 * opened. The root is now read the way GlycanCore reads it: anything that is not a residue a dictionary
 * has is an aglycon, named by the whole of it, with or without a mass.
 */
public class AglyconNamesRoundTripTest {

	private static final String GALNAC = "--?a1D-GalNAc,p";
	private static final String OPTIONS = "$MONO,Und,0,0,";

	@BeforeClass
	public static void loadDictionaries() {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Names a residue's grammar cannot hold come back as written, and as typed. */
	@Test
	public void aNameComesBackAsWritten() throws Exception {
		List<String[]> cases = Arrays.asList(
				new String[] {"Ser/Thr=87.0320u", "Ser/Thr"},
				new String[] {"Cer(d18:1/16:0)=537.5121u", "Cer(d18:1/16:0)"},
				new String[] {"β-Ala=89.0477u", "β-Ala"},
				new String[] {"D-Ser=87.0320u", "D-Ser"},
				new String[] {"Ser Thr=87.0320u", "Ser Thr"},
				new String[] {"a=b=1.0000u", "a=b"});
		for (String[] one : cases) {
			String gws = one[0] + GALNAC + OPTIONS + one[0];
			Glycan read = Glycan.fromString(gws);

			assertNotNull(gws, read);
			assertEquals(gws, one[1], read.getRoot().getResidueName());
			assertEquals(gws, gws, read.toString());
		}
	}

	/** What would split the text before the name is looked at is escaped, and shown as typed. */
	@Test
	public void whatSplitsTheTextIsEscaped() throws Exception {
		ResidueType typed = ResidueType.createOtherReducingEnd("Ser,Thr; $1 {}--x", 87.032);
		assertEquals("Ser,Thr; $1 {}--x", typed.getResidueName());

		String gws = Glycan.fromString("freeEnd" + GALNAC + OPTIONS + "freeEnd").toString();
		Glycan glycan = Glycan.fromString(gws);
		// as the Mass dialog sets it: through the mass options, which write it again as field 5
		org.eurocarbdb.application.glycanbuilder.massutil.MassOptions options = glycan.getMassOptions().clone();
		options.REDUCING_END_TYPE = typed;
		glycan.setMassOptions(options);
		String written = glycan.toString();

		assertFalse(written, written.contains(";"));
		assertEquals(written, 2, written.split("\\$").length);
		Glycan readBack = Glycan.fromString(written);
		assertEquals("Ser,Thr; $1 {}--x", readBack.getRoot().getResidueName());
		assertEquals(typed.getResidueMassMain(), readBack.getRoot().getType().getResidueMassMain(), 1e-4);
		assertEquals(written, readBack.toString());
	}

	/**
	 * Only a name marked "_e_" is decoded. One written before, with what looks like an escape in it,
	 * reads as it always did; and one typed with "_e_" at its start survives being typed so.
	 */
	@Test
	public void onlyAMarkedNameIsDecoded() throws Exception {
		Glycan old = Glycan.fromString("Ser_2F_Thr=87.0320u" + GALNAC + OPTIONS + "Ser_2F_Thr=87.0320u");
		assertEquals("Ser_2F_Thr", old.getRoot().getResidueName());

		assertEquals("Ser/Thr", ResidueType.escapeAglyconName("Ser/Thr"));
		assertEquals("_e_Ser_2C_Thr", ResidueType.escapeAglyconName("Ser,Thr"));
		for (String typed : new String[] {"_e_Ser", "a_2C_b,c", "x--y", "\u30bb\u30ea\u30f3,1", "Ser_2F_Thr"})
			assertEquals(typed, typed, ResidueType.unescapeAglyconName(ResidueType.escapeAglyconName(typed)));

		assertEquals("a byte-wise escape reads as its letter", "\u30bb",
				ResidueType.unescapeAglyconName("_e__E3__82__BB_"));
	}

	/**
	 * Every label stored by the escape as first decided (docs/gws-format.md, label-escapes.tsv) still
	 * reads as it was typed: the writer now escapes less, and the reader decodes the same.
	 */
	@Test
	public void theFirstEscapesStillReadAsTyped() throws Exception {
		java.util.List<String> rows = java.nio.file.Files.readAllLines(
				java.nio.file.Paths.get("src/test/resources/gws/label-escapes.tsv"),
				java.nio.charset.StandardCharsets.UTF_8);
		int read = 0;
		for (String row : rows) {
			if (row.startsWith("#") || row.trim().isEmpty()) continue;
			String[] columns = row.split("\t");
			assertEquals(row, columns[0], ResidueType.unescapeAglyconName(columns[1]));
			read++;
		}
		assertEquals(9, read);
	}

	/**
	 * Only a name no dictionary has is an aglycon. Kdo, Mur and Bac are of the class "Unknown" too -
	 * SNFG's flat hexagons - and a name of theirs stays the residue it names.
	 */
	@Test
	public void aResidueOfTheClassUnknownIsNotAnAglycon() {
		for (String name : new String[] {"Kdo", "Mur", "Bac", "Unknown"}) {
			ResidueType type = org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary.findReducingEndType(name);
			assertFalse(name, type.isAglycon());
			assertEquals(name, name, type.getName());
		}
		assertTrue(org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary.findReducingEndType("NoSuchName").isAglycon());
	}

	/** A root of a name and no mass, from an older file or another tool, is an aglycon of no mass. */
	@Test
	public void aNameWithNoMassIsAnAglyconOfNoKnownMass() throws Exception {
		String gws = "Ser" + GALNAC + OPTIONS + "Ser";
		Glycan read = Glycan.fromString(gws);

		assertTrue(read.getRoot().isReducingEnd());
		assertTrue(read.hasAglycon());
		assertTrue(read.hasAglyconOfUnknownMass());
		assertEquals("Ser", read.getRoot().getResidueName());
		assertEquals(gws, read.toString());
	}

	/** A composition's root has no linkage after it, and its aglycon is read all the same. */
	@Test
	public void aCompositionKeepsItsAglycon() throws Exception {
		String gws = "Ser/Thr=NaNu}--?b1D-Gal,p" + OPTIONS + "Ser/Thr=NaNu";
		Glycan read = Glycan.fromString(gws);

		assertEquals("Ser/Thr", read.getRoot().getResidueName());
		assertEquals(gws, read.toString());
	}

	/** Ser/Thr is one of two masses: written NaN, read back so, and not shown as a figure. */
	@Test
	public void aMassNotKnownIsSaidAndNotShown() throws Exception {
		String gws = "Ser/Thr=NaNu" + GALNAC + OPTIONS + "Ser/Thr=NaNu";
		Glycan read = Glycan.fromString(gws);

		assertTrue(read.hasAglyconOfUnknownMass());
		assertFalse("a NaN would reach every sum", Double.isNaN(read.computeMass()));
		assertEquals(gws, read.toString());
		assertTrue(new MassText().of(read), new MassText().of(read).contains(AbstractGlycanRenderer.AGLYCON_UNWEIGHED));

		Glycan known = Glycan.fromString("Ser=87.0320u" + GALNAC + OPTIONS + "Ser=87.0320u");
		assertFalse(known.hasAglyconOfUnknownMass());
		assertFalse(new MassText().of(known).contains(AbstractGlycanRenderer.AGLYCON_UNWEIGHED));
	}

	/** The reducing ends the dictionaries have read as they always did. */
	@Test
	public void theDictionariesReducingEndsReadAsBefore() throws Exception {
		for (String gws : new String[] {
				"freeEnd--?b1D-Glc,p$MONO,Und,0,0,freeEnd",
				"redEnd--?b1D-Glc,o--4b1D-Gal,p$MONO,Und,0,0,redEnd",
				"freeEnd(--?a1L-Fuc,p)--?b1D-Glc,p$MONO,Und,0,0,freeEnd",
				"freeEnd--?b1D-Glc,p}--?b1D-Gal,p$MONO,Und,0,0,freeEnd"}) {
			Glycan read = Glycan.fromString(gws);
			assertFalse(gws, read.hasAglycon());
			assertEquals(gws, gws, read.toString());
		}

		Glycan oldForm = Glycan.fromString("freeEnd-?b1D-Glc,p-4b1D-Gal,p");
		assertNotNull("the pre-2007 form still reads", oldForm);
		assertEquals("freeEnd", oldForm.getRoot().getTypeName());
		assertEquals(2, oldForm.getAllResidues().size() - 1);
	}

	/** WURCS writes the glycan alone, and the export says the aglycon was left out. */
	@Test
	public void aWurcsExportSaysTheAglyconIsLeftOut() throws Exception {
		Glycan serThr = Glycan.fromString("Ser/Thr=NaNu" + GALNAC + OPTIONS + "Ser/Thr=NaNu");
		Glycan free = Glycan.fromString("freeEnd" + GALNAC + OPTIONS + "freeEnd");

		assertEquals(new WURCS2Parser().writeGlycan(free), new WURCS2Parser().writeGlycan(serThr));

		String warning = AglyconInWURCS.warning(Arrays.asList(serThr, free), null, "wurcs2");
		assertNotNull(warning);
		assertTrue(warning, warning.contains("Ser/Thr"));

		assertNull("GWS keeps it", AglyconInWURCS.warning(Collections.singletonList(serThr), null, "gws"));
		assertNull("nothing to say", AglyconInWURCS.warning(Collections.singletonList(free), null, "wurcs2"));
		assertNull("a structure not written is said elsewhere",
				AglyconInWURCS.warning(Collections.singletonList(serThr), Collections.singletonList(serThr), "wurcs2"));
	}

	/** The renderer's m/z line, which is protected. */
	private static class MassText extends GlycanRendererAWT {
		String of(Glycan structure) {
			return getMassText(structure);
		}
	}
}
