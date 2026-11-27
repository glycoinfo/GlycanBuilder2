package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.AnomericCarbons;
import org.glycoinfo.application.glycanbuilder.util.exchange.GlycanToWURCSException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A monosaccharide whose anomeric carbon is set to {@code ?} is written with its type's (#332).
 *
 * <p>WURCS builds the ring from the anomeric carbon. With {@code ?} the structure GlyCosmos's search by
 * graphic reported could not be written at all, and a single beta-galactopyranose went out as the open
 * chain {@code o2112h}, its ring and anomer gone. Each is written here as the same structure with the
 * carbon its type has, which is what a residue taken from the menu has.
 */
public class UnknownAnomericCarbonTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	private static String write(String _gws) throws Exception {
		return new WURCS2Parser().toWURCS(Glycan.fromString(_gws));
	}

	/** The structure reported: every residue's anomeric carbon ?, and nothing written. */
	@Test
	public void theReportedStructureIsWritten() throws Exception {
		String unknown = "freeEnd--?D-GlcNAc,p(--?L-Fuc,p)--?D-GlcNAc,p--?D-Man,p(--?D-Man,p)--?D-Man,p$MONO,perMe,Na,0,freeEnd";
		String known = "freeEnd--??1D-GlcNAc,p(--??1L-Fuc,p)--??1D-GlcNAc,p--??1D-Man,p(--??1D-Man,p)--??1D-Man,p$MONO,perMe,Na,0,freeEnd";

		assertEquals("WURCS=2.0/3,6,5/[a2122h-1x_1-5_2*NCC/3=O][a1122h-1x_1-5][a1221m-1x_1-5]/1-1-2-2-2-3/"
				+ "a?-b1_a?-f1_b?-c1_c?-d1_c?-e1", write(unknown));
		assertEquals(write(known), write(unknown));
	}

	/** A pyranose and a furanose keep their ring and their anomer. */
	@Test
	public void theRingAndTheAnomerAreKept() throws Exception {
		assertEquals("WURCS=2.0/2,2,1/[h2122h][a2112h-1b_1-5]/1-2/a4-b1",
				write("redEnd--??1D-Glc,p--4b?D-Gal,p$MONO,perMe,Na,0,redEnd"));
		assertEquals("WURCS=2.0/2,2,1/[h2122h][a2112h-1b_1-4]/1-2/a4-b1",
				write("redEnd--??1D-Glc,p--4b?D-Gal,f$MONO,perMe,Na,0,redEnd"));
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1b_1-5]/1/",
				write("freeEnd--?b?D-Glc,p$MONO,perMe,Na,0,freeEnd"));
	}

	/** A ketose takes its C2: NeuAc was written as an open chain linked the wrong way round. */
	@Test
	public void aKetoseTakesItsSecondCarbon() throws Exception {
		assertEquals("WURCS=2.0/2,2,1/[h2112h][Aad21122h-2a_2-6_5*NCC/3=O]/1-2/a3-b2",
				write("redEnd--??1D-Gal,p--3a?D-NeuAc,p$MONO,perMe,Na,0,redEnd"));
	}

	/**
	 * A residue further out with neither its ring nor its anomeric carbon known - WURCS's u, which only
	 * a reducing end can be - is refused, as it was before; but saying why, where it threw an
	 * IndexOutOfBoundsException or went out as an open chain.
	 */
	@Test
	public void anUnknownFormFurtherOutIsRefused() throws Exception {
		assertRefused("GlcNAc", "freeEnd--?D-GlcNAc,?--?D-GlcNAc,?--?D-Man,?$MONO,perMe,Na,0,freeEnd");
		assertRefused("GlcNAc", "freeEnd--??1D-GlcNAc,p--4b?D-GlcNAc,?--4b1D-Man,p$MONO,perMe,Na,0,freeEnd");

		Glycan read = new WURCS2Parser().readGlycan("WURCS=2.0/2,2,1/[a2122h-1x_1-5][u2112h]/1-2/a4-b?",
				new org.eurocarbdb.application.glycanbuilder.massutil.MassOptions());
		try {
			new WURCS2Parser().toWURCS(read);
			fail("a u further out than the reducing end was written");
		} catch (GlycanToWURCSException refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains("Gal is joined to its parent"));
		}
	}

	private static void assertRefused(String _residue, String _gws) throws Exception {
		try {
			fail(_gws + " was written: " + write(_gws));
		} catch (GlycanToWURCSException refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains(_residue + " is joined to its parent"));
			assertTrue(refused.getMessage(), refused.getMessage().contains("u in WURCS"));
		}
	}

	/**
	 * An open chain has no ring to close, and is written as it was. Nor is a reducing end of unknown
	 * ring size: with its carbon unknown it is WURCS's u, a ring or an open chain, and reads and writes
	 * as that.
	 */
	@Test
	public void anOpenChainAndAnUnknownRingAreLeftAsTheyWere() throws Exception {
		assertEquals("WURCS=2.0/2,2,1/[h2122h][h2112h]/1-2/a4-b?",
				write("redEnd--??1D-Glc,p--4??D-Gal,o$MONO,perMe,Na,0,redEnd"));

		String u = "WURCS=2.0/2,2,1/[u2122h][a2112h-1b_1-5]/1-2/a4-b1";
		assertEquals(u, new WURCS2Parser().toWURCS(new WURCS2Parser().readGlycan(u, new org.eurocarbdb.application.glycanbuilder.massutil.MassOptions())));
	}

	/** Only the writing sees the carbon: the structure keeps what it was given. */
	@Test
	public void theStructureIsNotChanged() throws Exception {
		String gws = "freeEnd--?D-GlcNAc,p(--?L-Fuc,p)--?D-GlcNAc,p--?D-Man,p(--?D-Man,p)--?D-Man,p$MONO,perMe,Na,0,freeEnd";
		Glycan structure = Glycan.fromString(gws);
		new WURCS2Parser().toWURCS(structure);

		assertEquals(gws, structure.toString());
		for (Residue residue : structure.getAllResidues())
			if (residue.isSaccharide()) {
				assertEquals('?', residue.getAnomericCarbon());
				assertEquals('?', residue.getParentLinkage().getAnomericCarbon());
			}
	}

	/**
	 * The toolbar and the properties dialog offer ? only where it may stay unknown, and show a
	 * residue read with ? with the carbon it is written with.
	 */
	@Test
	public void theMenusOfferAnUnknownCarbonOnlyWhereItMayStayUnknown() throws Exception {
		Residue gal = Glycan.fromString("freeEnd--?b?D-Gal,p$MONO,perMe,Na,0,freeEnd").getRoot().firstChild();
		assertEquals(Arrays.asList("1", "2", "3"), Arrays.asList(AnomericCarbons.choices(gal, 'p')));
		// of unknown ring size: ? for the reducing end, which may be WURCS's u; not for one further out
		assertEquals(Arrays.asList("?", "1", "2", "3"), Arrays.asList(AnomericCarbons.choices(gal, '?')));
		Residue outer = Glycan.fromString("freeEnd--??1D-Glc,p--4b?D-Gal,?$MONO,perMe,Na,0,freeEnd")
				.getRoot().firstChild().firstChild();
		assertEquals(Arrays.asList("1", "2", "3"), Arrays.asList(AnomericCarbons.choices(outer, '?')));
		assertEquals('1', AnomericCarbons.of(gal, 'p'));

		// an open chain, about to be one or one already
		assertEquals(Arrays.asList("?", "1", "2", "3"), Arrays.asList(AnomericCarbons.choices(gal, 'o')));
		assertEquals('?', AnomericCarbons.of(gal, 'o'));
		gal.setAlditol(true);
		gal.setRingSize('o');
		assertFalse(AnomericCarbons.mustBeKnown(gal));
		assertEquals('?', AnomericCarbons.of(gal));

		Residue neuAc = Glycan.fromString("freeEnd--?a?D-NeuAc,p$MONO,perMe,Na,0,freeEnd").getRoot().firstChild();
		assertEquals('2', AnomericCarbons.of(neuAc, 'p'));

		// a substituent is no monosaccharide in a ring
		ResidueType sulfate = ResidueDictionary.findResidueType("S");
		Residue residue = new Residue(sulfate);
		assertFalse(AnomericCarbons.mustBeKnown(residue));
		assertTrue(Arrays.asList(AnomericCarbons.choices(residue, residue.getRingSize())).contains("?"));
	}
}
