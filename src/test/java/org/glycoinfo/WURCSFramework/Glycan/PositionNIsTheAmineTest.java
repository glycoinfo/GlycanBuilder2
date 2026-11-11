package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A group drawn on an amino sugar's nitrogen, position N, is written on the carbon that carries it
 * (#222, #189).
 *
 * <p>GlcN, GalN, Neu and the other amino sugars offer N among their positions, and a sulfate there is
 * the N-sulfate of heparin's GlcNS. It attached, and the export failed - "NumberFormatException: For
 * input string: N" - so a structure drawn that way could not be written, and a web page that converts on
 * reading could not open it. The same sulfate drawn at 2 was written, as {@code 2*NSO/3=O/3=O}. N is
 * now that carbon.
 */
public class PositionNIsTheAmineTest {

	@BeforeClass
	public static void loadDictionaries() {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** The amine's carbon is what the residue's name owns: 2 for the hexosamines, 5 for Neu. */
	@Test
	public void theAmineIsWhereTheNameSays() {
		assertEquals('2', NativeMonosaccharideDictionary.forResidueName("GlcN").getAminePosition());
		assertEquals('2', NativeMonosaccharideDictionary.forResidueName("GalN").getAminePosition());
		assertEquals('2', NativeMonosaccharideDictionary.forResidueName("MurNAc").getAminePosition());
		assertEquals('5', NativeMonosaccharideDictionary.forResidueName("Neu").getAminePosition());
		assertEquals('5', NativeMonosaccharideDictionary.forResidueName("NeuAc").getAminePosition());
		// none, and two: no one carbon
		assertEquals('\0', NativeMonosaccharideDictionary.forResidueName("Glc").getAminePosition());
		assertEquals('\0', NativeMonosaccharideDictionary.forResidueName("Bac").getAminePosition());
	}

	/** At N it is written as at the amine's carbon, and weighs what WURCSFramework says. */
	@Test
	public void atNItIsWrittenOnTheAminesCarbon() throws Exception {
		String[][] cases = {
				{ "freeEnd--?a1D-GlcN,p--N?1S$MONO,Und,0,0,freeEnd", "freeEnd--?a1D-GlcN,p--2?1S$MONO,Und,0,0,freeEnd",
						"WURCS=2.0/1,1,0/[a2122h-1a_1-5_2*NSO/3=O/3=O]/1/" },
				{ "freeEnd--?a1D-GalN,p(--N?1S)--6?1S$MONO,Und,0,0,freeEnd", "freeEnd--?a1D-GalN,p(--2?1S)--6?1S$MONO,Und,0,0,freeEnd",
						"WURCS=2.0/1,1,0/[a2112h-1a_1-5_2*NSO/3=O/3=O_6*OSO/3=O/3=O]/1/" },
				{ "freeEnd--?a1D-GlcN,p--N?1Ac$MONO,Und,0,0,freeEnd", "freeEnd--?a1D-GlcN,p--2?1Ac$MONO,Und,0,0,freeEnd",
						"WURCS=2.0/1,1,0/[a2122h-1a_1-5_2*NCC/3=O]/1/" },
				{ "freeEnd--?a2D-Neu,p--N?1Ac$MONO,Und,0,0,freeEnd", "freeEnd--?a2D-Neu,p--5?1Ac$MONO,Und,0,0,freeEnd",
						"WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_5*NCC/3=O]/1/" } };
		for (String[] one : cases) {
			Glycan atN = Glycan.fromString(one[0]);
			assertEquals(one[0], one[2], write(atN));
			assertEquals(one[0], write(Glycan.fromString(one[1])), write(atN));
			assertEquals(one[0], wurcsMass(one[2]), underivatized(atN).computeMass(), 1e-3);
		}
	}

	/**
	 * The structure it was reported with, less its repeat (#301): heparin's GlcNS on the linkage
	 * region. It is written, the WURCS is valid, and it reads back as itself.
	 */
	@Test
	public void theReportedStructureIsWritten() throws Exception {
		Glycan drawn = Glycan.fromString("freeEnd--1b1D-Xyl,p--4b1D-Gal,p--3b1D-Gal,p--3b1D-GlcA,p--4a1D-GlcN,p(((--N?1S)"
				+ "--4a1L-IdoA,p--2?1S)--6?1S)--3?1S$MONO,Und,0,0,freeEnd");
		String wurcs = write(drawn);
		assertTrue(wurcs, wurcs.contains("[a2122h-1a_1-5_2*NSO/3=O/3=O_3*OSO/3=O/3=O_6*OSO/3=O/3=O]"));

		WURCSValidator validator = new WURCSValidator();
		validator.start(wurcs);
		assertTrue(validator.getReport().getErrors().toString(), validator.getReport().getErrors().isEmpty());
		assertEquals(wurcsMass(wurcs), underivatized(drawn).computeMass(), 1e-3);
		assertEquals(wurcs, write(new WURCS2Parser().readGlycan(wurcs, new MassOptions())));
	}

	// ------------------------------------------------------------------------------------------------

	private static String write(Glycan glycan) throws Exception {
		return new WURCS2Parser().toWURCS(glycan);
	}

	private static Glycan underivatized(Glycan glycan) {
		MassOptions none = glycan.getMassOptions().clone();
		none.setDerivatization(MassOptions.NO_DERIVATIZATION);
		glycan.setMassOptions(none);
		return glycan;
	}

	private static double wurcsMass(String wurcs) throws Exception {
		return WURCSMassCalculator.calcMassWURCS(new WURCSFactory(wurcs).getArray()).doubleValue();
	}
}
