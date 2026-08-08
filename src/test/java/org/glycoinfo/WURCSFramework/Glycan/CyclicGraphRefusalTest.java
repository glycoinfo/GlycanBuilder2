package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converterGWS.GWSParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a structure graph with a cycle in it is refused with a sentence, not a StackOverflowError
 * (#125).
 *
 * <p>A WURCS with two connections between the same pair of residues - a bridge plus a direct bond,
 * as in G11127BT - came out of the importer as a genuine cycle in what every walker downstream
 * assumes is a tree. The first walker to touch it descended forever, and the report was a
 * StackOverflowError far from the cause, which nothing can usefully catch. The importer now walks
 * what it built and refuses a cycle before the document sees it, and the GWS writer carries its own
 * guard for any cyclic graph that arrives another way.</p>
 */
public class CyclicGraphRefusalTest {

	/** The two-connection WURCS from #125, minus the repeat that hides it. */
	private static final String TWO_CONNECTIONS =
			"WURCS=2.0/2,2,2/[u2122h][a2211m-1x_1-5]/1-2/a3-b2*OCCCCO*/6=O/3=O_a?-b1";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The cyclic WURCS is refused cleanly, and the document is left exactly as it was. */
	@Test
	public void theCyclicWurcsIsRefusedAndTheDocumentUntouched() {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();

		boolean imported;
		try {
			imported = document.importFromString(TWO_CONNECTIONS, "wurcs2");
		} catch (Exception refused) {
			imported = false;
		}

		assertFalse("the cycle was imported", imported);
		assertEquals("something was left in the document", 0, document.getStructures().size());
	}

	/** The GWS writer refuses a cyclic graph with a sentence rather than dying in it. */
	@Test
	public void theGwsWriterRefusesACycleWithASentence() throws Exception {
		Residue one = ResidueDictionary.newResidue("Glc");
		Residue other = ResidueDictionary.newResidue("Gal");
		one.addChild(other, '4');
		other.addChild(one, '6');

		try {
			GWSParser.writeSubtree(one, false);
			fail("a cyclic graph was written");
		} catch (IllegalArgumentException refused) {
			assertTrue(refused.getMessage(), refused.getMessage().contains("cycle"));
		}
	}

	/** A bridge with one connection is not a cycle, and still imports and writes. */
	@Test
	public void aSingleConnectionBridgeStillGoesThrough() throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();

		assertTrue(document.importFromString(
				"WURCS=2.0/2,2,1/[a2122h-1b_1-5][a2112h-1b_1-5]/1-2/a6-b1*OPO*/3O/3=O", "wurcs2"));
		assertTrue("its GWS came out empty", !document.toString("GWS").isBlank());
	}

	/** A repeat closes its ring through markers, not a raw cycle, and still imports and writes. */
	@Test
	public void aRepeatStillGoesThrough() throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();

		assertTrue(document.importFromString(
				"WURCS=2.0/2,3,3/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5]/1-1-2/a4-b1_b4-c1_c4-c?~n",
				"wurcs2"));
		assertTrue(!document.toString("GWS").isBlank());
	}

	/** And an ordinary chain is untouched by either guard. */
	@Test
	public void anOrdinaryChainIsUntouched() throws Exception {
		Glycan chain = Glycan.fromString("freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p$MONO,Und,0,0,freeEnd");

		assertTrue(chain.toString().contains("GlcNAc"));
	}
}
