package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A repeat read from GWS is written to WURCS closed, as one read from WURCS or drawn is (#301).
 *
 * <p>GWS writes a repeat's markers as "[" and "]", and reading them only paired the two. Neither knew
 * the repeat's first residue or the carbon it closes on, which drawing a repeat and reading one from
 * WURCS both set, and the writer closes the repeat from them. So the repeat went out open, {@code
 * c4~n}, a WURCS that WURCSFramework calls invalid: a structure with a repeat, saved and opened again,
 * could not be exported.
 */
public class RepeatReadFromGwsTest {

	@BeforeClass
	public static void loadDictionaries() {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Through GWS and back, a repeat writes the WURCS it wrote before. */
	@Test
	public void aRepeatSurvivesGws() throws Exception {
		for (String wurcs : new String[] {
				"WURCS=2.0/3,3,3/[a2122A-1b_1-5][a2122h-1a_1-5_2*N][a2121A-1a_1-5]/1-2-3/a4-b1_b4-c1_c4-b1~n",
				"WURCS=2.0/3,3,3/[a2122A-1b_1-5][a2122h-1a_1-5_2*NSO/3=O/3=O_6*OSO/3=O/3=O][a2121A-1a_1-5_2*OSO/3=O/3=O]/1-2-3/a4-b1_b4-c1_c4-b1~n",
				"WURCS=2.0/2,3,3/[a2122h-1b_1-5][a2112h-1b_1-5]/1-2-1/a4-b1_b3-c1_c3-b1~n" }) {
			Glycan read = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
			String written = write(read);
			Glycan reopened = Glycan.fromString(read.toString());
			assertEquals(wurcs, written, write(reopened));
			assertValid(write(reopened));
		}
	}

	/** Each marker knows the repeat's first residue and the carbon it closes on. */
	@Test
	public void theMarkersKnowTheRepeatsFirstResidue() throws Exception {
		Glycan read = Glycan.fromString("freeEnd--1b1D-GlcA,p--4[--4a1D-GlcN,p--4a1L-IdoA,p--4]$MONO,Und,0,0,freeEnd");
		Residue start = null, end = null, first = null;
		for (Residue residue : read.getAllResidues()) {
			if (residue.isStartRepetition()) start = residue;
			if (residue.isEndRepetition()) end = residue;
			if ("GlcN".equals(residue.getTypeName())) first = residue;
		}
		assertSame(first, end.getStartResidue());
		assertSame(start, first.getStartRepetitionResidue());
		assertEquals('1', start.getAnomericCarbon());
		assertEquals('1', end.getAnomericCarbon());
	}

	/**
	 * The structure it was reported with, its N-sulfate drawn at 2 (position N is #222): heparin's
	 * repeat on the linkage region. The WURCS is valid and reads back as itself.
	 */
	@Test
	public void theReportedStructureIsWrittenValid() throws Exception {
		Glycan drawn = Glycan.fromString("freeEnd--1b1D-Xyl,p--4b1D-Gal,p--3b1D-Gal,p--3b1D-GlcA,p--4[--4a1D-GlcN,p(((--2?1S)"
				+ "--4a1L-IdoA,p(--4])--2?1S)--6?1S)--3?1S$MONO,Und,0,0,freeEnd");
		String wurcs = write(drawn);
		assertTrue(wurcs, wurcs.endsWith("_e1-f4~n"));
		assertValid(wurcs);
		assertEquals(wurcs, write(new WURCS2Parser().readGlycan(wurcs, new MassOptions())));
	}

	// ------------------------------------------------------------------------------------------------

	private static String write(Glycan glycan) throws Exception {
		return new WURCS2Parser().toWURCS(glycan);
	}

	private static void assertValid(String wurcs) {
		WURCSValidator validator = new WURCSValidator();
		validator.start(wurcs);
		assertTrue(wurcs + " " + validator.getReport().getErrors(), validator.getReport().getErrors().isEmpty());
	}
}
