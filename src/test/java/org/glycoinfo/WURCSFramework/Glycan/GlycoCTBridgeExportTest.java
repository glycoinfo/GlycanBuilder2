package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a structure with a bridge exports to GlycoCT at all, and correctly (#27).
 *
 * <p>Any structure with a phosphate bridge exported to an empty string. The bridge residue went to
 * the namescheme converter decorated like a sugar - "?-P", which nothing could resolve - and
 * undecorated it resolved to a substituent whose exchange table only knows the single-attachment
 * form. The exporter now creates bridges as typed substituent nodes in GlycoCT's own vocabulary,
 * with both attachments at position 1 and the sugar's side of each bond typed by the atom the
 * bridge attaches through: oxygen keeps the sugar's OH ({@code o}), nitrogen and sulfur replace it
 * ({@code d}).</p>
 */
public class GlycoCTBridgeExportTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The reported case: a phosphodiester bridge exports as the phosphate substituent. */
	@Test
	public void aPhosphateBridgeExportsAsPhosphate() throws Exception {
		String glycoct = glycoctOfWurcs(
				"WURCS=2.0/2,2,1/[a2122h-1b_1-5][a2112h-1b_1-5]/1-2/a6-b1*OPO*/3O/3=O");

		assertTrue(glycoct, glycoct.contains("s:phosphate"));
		assertTrue("the edge into it is not o(6+1)n:\n" + glycoct, glycoct.contains("1o(6+1)2n"));
		assertTrue("the edge out of it is not n(1+1)o:\n" + glycoct, glycoct.contains("2n(1+1)3o"));
	}

	/** Every mapped bridge exports non-empty GlycoCT that its own reader accepts back. */
	@Test
	public void everyMappedBridgeExportsAndReadsBack() throws Exception {
		for (String bridge : new String[] {"P", "S", "SH", "N", "Suc", "PyrP"}) {
			String glycoct = glycoctOfGws(
					"freeEnd--?b1D-Glc,p--6?1" + bridge + "--1b1D-Gal,p$MONO,Und,0,0,freeEnd");

			assertTrue(bridge + " exported nothing", !glycoct.isBlank());
			assertTrue(bridge + " still attaches at -1:\n" + glycoct, !glycoct.contains("+-1)"));

			GlycanDocument reader = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
			assertTrue(bridge + "'s own GlycoCT cannot be read back:\n" + glycoct,
					reader.importFromString(glycoct, "glycoct_condensed"));
		}
	}

	/**
	 * The sugar's side of the bond follows the atom the bridge attaches through.
	 *
	 * <p>An oxygen bridge leaves the sugar's OH in place ({@code o}); an amino or thio bridge
	 * replaces it ({@code d}). Writing {@code o} for an amino bridge would claim an oxygen that is
	 * not there.</p>
	 */
	@Test
	public void theSugarsSideFollowsTheAttachingAtom() throws Exception {
		String oxygen = glycoctOfGws("freeEnd--?b1D-Glc,p--6?1P--1b1D-Gal,p$MONO,Und,0,0,freeEnd");
		String nitrogen = glycoctOfGws("freeEnd--?b1D-Glc,p--6?1N--1b1D-Gal,p$MONO,Und,0,0,freeEnd");

		assertTrue("phosphate is not O-attached:\n" + oxygen, oxygen.contains("1o(6+1)2n"));
		assertTrue("amino is not N-attached:\n" + nitrogen, nitrogen.contains("1d(6+1)2n"));
	}

	/**
	 * A bridge whose two attachments go through different atoms is refused, not guessed.
	 *
	 * <p>NS attaches through nitrogen on one side and sulfur-oxygen on the other, and nothing in
	 * the model records which sugar got which - so it keeps the old failing path (an empty export)
	 * rather than exporting with an invented linkage type.</p>
	 */
	@Test
	public void anAsymmetricBridgeIsStillRefused() throws Exception {
		assertEquals("", glycoctOfGws(
				"freeEnd--?b1D-Glc,p--6?1NS--1b1D-Gal,p$MONO,Und,0,0,freeEnd").strip());
	}

	private static String glycoctOfWurcs(String wurcs) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue("the WURCS itself would not import", document.importFromString(wurcs, "wurcs2"));

		return document.toString("glycoct_condensed");
	}

	private static String glycoctOfGws(String gws) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		Glycan structure = Glycan.fromString(gws);
		assertTrue("the GWS itself would not parse", structure != null);
		document.addStructure(structure);

		return document.toString("glycoct_condensed");
	}
}
