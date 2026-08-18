package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a WURCS the conversion cannot handle fails in WURCS terms, not in Java's (#123).
 *
 * <p>The conversion's own failures came out as raw NullPointerExceptions and
 * StringIndexOutOfBounds - "String index out of range: 8" for a substituent placed at position 9
 * of a hexose - which name nothing, and read as crashes rather than as answers about the sequence.
 * Every WURCS enters through one door, {@code WURCS2Parser.readGlycan}, so the translation lives
 * there: what kind of failure, on which sequence, with the original chained underneath for whoever
 * needs the trace.</p>
 */
public class WurcsErrorTranslationTest {

	/** A substituent at position 9 of a six-carbon sugar, which the conversion indexes past. */
	private static final String POSITION_PAST_THE_SUGAR =
			"WURCS=2.0/1,1,0/[axxxxh-1x_1-5_2*NCC/3=O_9*OSO/3=O/3=O]/1/";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The failure names the failure, the sequence, and carries the original underneath. */
	@Test
	public void aConversionFailureSpeaksWurcs() throws Exception {
		try {
			new WURCS2Parser().readGlycan(POSITION_PAST_THE_SUGAR, new MassOptions());
			fail("it converted");
		} catch (WURCSToGlycanException translated) {
			assertTrue(translated.getMessage(),
					translated.getMessage().contains(WURCSToGlycanException.badSubstituentMessage));
			assertTrue(translated.getMessage(),
					translated.getMessage().contains("*OSO/3=O/3=O"));
			assertTrue("the original trace was lost",
					translated.getCause() != null);
		} catch (Exception raw) {
			fail("still a raw Exception: "+raw.getClass().getSimpleName());
		}
	}

	/** The failure names the failure, the sequence, and carries the original underneath. */
	@Test
	public void aConversionFailureSpeaksWurcs2() throws Exception {
		try {
			// G00028QS // in analyzeGRES use in WURCSSequence2ToGlycan.start
			new WURCS2Parser().readGlycan("WURCS=2.0/3,3,2/[AUdxxxxh][AUxxxxxh][uxxxh_4*NCC/3=O]/1-2-3/a?|b?|c?}-{a?|b?|c?_a?|b?|c?}-{a?|b?|c?", 
				new MassOptions());
			fail("it converted");
		} catch (WURCSToGlycanException translated) {
			assertTrue(translated.getMessage(),
					translated.getMessage().contains(WURCSToGlycanException.badResidueMessage));
			assertTrue(translated.getMessage(),
					translated.getMessage().contains("AUdxxxxh"));
			assertTrue("the original trace was lost",
			 		translated.getCause() != null);
		} catch (Exception raw) {
			fail("still a raw Exception: "+raw.getClass().getSimpleName());
		}
	}

	/** Through the document it is an orderly false, and the document is left as it was. */
	@Test
	public void throughTheDocumentItIsAnOrderlyNo() {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();

		boolean imported;
		try {
			imported = document.importFromString(POSITION_PAST_THE_SUGAR, "wurcs2");
		} catch (Exception refused) {
			imported = false;
		}

		assertTrue("it imported an unconvertible WURCS", !imported);
		assertEquals(0, document.getStructures().size());
	}

	/**
	 * A failure that already speaks - "this substituent could not support: *XYZQW" - is passed
	 * through untouched, not wrapped into noise.
	 */
	@Test
	public void aFailureThatAlreadySpeaksIsNotRewrapped() throws Exception {
		try {
			new WURCS2Parser().readGlycan(
					"WURCS=2.0/1,1,0/[a2122h-1b_1-5_2*XYZQW]/1/", new MassOptions());
			fail("it converted");
		} catch (WURCSToGlycanException wrapped) {
			assertTrue("does not indicate a bad substituent", 
				wrapped.getMessage().contains(WURCSToGlycanException.badSubstituentMessage));
			assertTrue("does not indicate the problematic substituent", 
				wrapped.getMessage().contains("*XYZQW"));
		} catch (Exception ex) {
			fail("a generic exception was raised: " + ex.getMessage());
		}
	}

	/** And an ordinary WURCS converts exactly as before. */
	@Test
	public void anOrdinaryWurcsStillConverts() throws Exception {
		assertTrue(new WURCS2Parser().readGlycan(
				"WURCS=2.0/1,2,1/[a2122h-1b_1-5_2*NCC/3=O]/1-1/a4-b1", new MassOptions()) != null);
	}
}
