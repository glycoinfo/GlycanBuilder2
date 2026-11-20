package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.GlycanToWURCSException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A structure that cannot be written as WURCS says why, to whoever asked (#275).
 *
 * <p>21 of the 23 heptose and octose residues have no stereo recorded, so the conversion refuses them
 * - "D-gro-D-galHep has no stereo recorded for it, so it has no skeleton to write." That sentence was
 * handed to {@code LogUtils.report} and the caller received {@code ""}. The desktop application shows
 * the report as a dialog and so was never silent. Everything headless was: the web application's
 * WURCS panel and its MCP encode tool got an empty string, with the reason in a global last-error slot
 * that, on a server, belongs to whichever request wrote last.
 *
 * <p>{@code toWURCS} is the same conversion with the reason on the call. {@code writeGlycan} is left
 * exactly as it was, because the desktop's export reads the slot back to name the structures that
 * came out blank, and that works.
 */
public class AFailedWurcsExportSaysWhyTest {

	/** A heptose with no stereo recorded - one of the 21. */
	private static final String NO_STEREO = "D-gro-D-galHep";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The reason arrives with the exception, in the conversion's own words. */
	@Test
	public void aResidueWithNoStereoSaysWhy() throws Exception {
		try {
			new WURCS2Parser().toWURCS(alone(NO_STEREO));
			fail("a heptose with no stereo recorded was written");
		} catch (GlycanToWURCSException refused) {
			String why = String.valueOf(refused.getMessage());
			assertTrue("the reason should name the residue: " + why, why.contains(NO_STEREO));
			assertTrue("the reason should say what is missing: " + why, why.contains("no stereo recorded"));
			assertFalse("a deliberate sentence should not be wrapped in a class name: " + why,
					why.contains("Exception"));
		}
	}

	/**
	 * And it arrives through the call, not through the global slot.
	 *
	 * <p>This is the property the change exists for. Two people exporting at once on the same server
	 * would otherwise read each other's reasons, or none.
	 */
	@Test
	public void throughTheCallNotTheLog() throws Exception {
		LogUtils.clearLastError();
		try {
			new WURCS2Parser().toWURCS(alone(NO_STEREO));
			fail("a heptose with no stereo recorded was written");
		} catch (GlycanToWURCSException expected) {
			// the reason is here
		}
		assertFalse("toWURCS reported to the global last-error slot: " + LogUtils.getLastError(),
				LogUtils.hasLastError());
	}

	/** Catchable alongside the read side's exception, which is what a caller converting both ways wants. */
	@Test
	public void isAWurcsExceptionLikeTheReadSide() {
		assertTrue(WURCSException.class.isAssignableFrom(GlycanToWURCSException.class));
	}

	/**
	 * {@code writeGlycan} keeps its contract: an empty string, with the reason in the slot.
	 *
	 * <p>The desktop's export depends on exactly this - it clears the slot, writes, and reads the slot
	 * back to say which structures came out blank and why. If this changed, that warning would go back
	 * to saying "N structures were left blank" and nothing about what.
	 */
	@Test
	public void writeGlycanStillAnswersEmptyAndReports() throws Exception {
		LogUtils.clearLastError();

		assertEquals("", new WURCS2Parser().writeGlycan(alone(NO_STEREO)));
		assertTrue("writeGlycan stopped reporting the reason", LogUtils.hasLastError());
		assertTrue("and should report it in the conversion's own words, not wrapped: "
				+ LogUtils.getLastError(), LogUtils.getLastError().startsWith(NO_STEREO));
	}

	/** And where there is a WURCS, both doors give the same one. */
	@Test
	public void aStructureThatWritesWritesTheSameThroughBoth() throws Exception {
		Glycan lacNAc = Glycan.fromString("freeEnd--?b1D-GlcNAc,p--4b1D-Gal,p$MONO,Und,0,0,freeEnd");
		String viaWrite = new WURCS2Parser().writeGlycan(lacNAc);

		assertFalse("the control did not write at all", viaWrite.isEmpty());
		assertEquals(viaWrite, new WURCS2Parser().toWURCS(lacNAc));
	}

	/**
	 * A composition goes through its own encoder, and both doors still agree on it.
	 *
	 * <p>Built the way the composition dialog builds one. A composition read <em>from</em> WURCS is a
	 * different object - a structure carrying a no-linkages marker - and does not take this road.
	 */
	@Test
	public void aCompositionWritesTheSameThroughBoth() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.HEX = 3;
		counts.HEXNAC = 2;
		Glycan composition = counts.getCompositionAsGlycan(neutral());

		String viaWrite = new WURCS2Parser().writeGlycan(composition);

		assertFalse("the composition control did not write at all", viaWrite.isEmpty());
		assertEquals(viaWrite, new WURCS2Parser().toWURCS(composition));
	}

	/**
	 * And a composition the encoder cannot name says so in the encoder's words.
	 *
	 * <p>{@code dPen} has no composition residue to be counted as (#220). The encoder's sentence -
	 * "A composition cannot be written in ..." - is a checked exception, something meant rather than a
	 * crash, so it is passed through unwrapped.
	 */
	@Test
	public void aCompositionThatCannotBeWrittenSaysWhy() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.DPEN = 1;
		Glycan composition = counts.getCompositionAsGlycan(neutral());

		try {
			new WURCS2Parser().toWURCS(composition);
			fail("a composition of dPen was written");
		} catch (GlycanToWURCSException refused) {
			String why = String.valueOf(refused.getMessage());
			assertTrue("the encoder's own sentence should come through: " + why,
					why.startsWith("A composition cannot be written in"));
		}
	}

	private static MassOptions neutral() {
		MassOptions neutral = new MassOptions();
		neutral.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		neutral.ION_CLOUD = new IonCloud();

		return neutral;
	}

	/** This residue alone on a free reducing end. */
	private static Glycan alone(String _typeName) throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		root.addChild(ResidueDictionary.newResidue(_typeName));
		return new Glycan(root, false, new MassOptions());
	}
}
