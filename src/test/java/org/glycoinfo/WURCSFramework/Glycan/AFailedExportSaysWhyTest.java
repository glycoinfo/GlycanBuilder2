package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That an export which produced nothing says why, and not only that it did.
 *
 * <p>The reason existed and was thrown away. A composition of something the encoder cannot name says so
 * — {@code A composition cannot be written in 'NeuAc'} — and the writer reports it before handing back an
 * empty string. The warning that followed could then only say that N of M structures "were left blank",
 * which tells somebody that something went wrong and nothing about what to do.
 *
 * <p>The failure used here is a real one rather than a contrived seam: a composition containing sialic
 * acid cannot be written at all (#219), which is the most ordinary way a user meets this.
 */
public class AFailedExportSaysWhyTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** A composition the encoder cannot name is reported as a failure, with the reason attached. */
	@Test
	public void aFailureCarriesTheReasonThatCausedIt() throws Exception {
		GlycanDocument document = documentHolding(compositionWithSialicAcid());

		document.exportFromStructure(document.getStructures(), "wurcs2");

		assertEquals("the composition should have been reported as unexportable",
				1, document.getLastExportFailures().size());

		String why = document.getLastExportFailureReason(document.getStructures().get(0));
		assertNotNull("the reason was thrown away", why);
		assertTrue("the reason should name what could not be written, and says: " + why,
				why.contains("NeuAc"));
	}

	/**
	 * A structure that exported fine has no reason attached.
	 *
	 * <p>The reason is read from where the writers report, so the guard that matters is that a reason
	 * left over from something earlier is not pinned to a structure that succeeded.
	 */
	@Test
	public void somethingThatExportedCarriesNoReason() throws Exception {
		GlycanDocument document = documentHolding(compositionThatWrites());

		document.exportFromStructure(document.getStructures(), "wurcs2");

		assertTrue("this composition should export", document.getLastExportFailures().isEmpty());
		assertNull("a structure that exported should carry no reason",
				document.getLastExportFailureReason(document.getStructures().get(0)));
	}

	/**
	 * And a reason from one export does not follow a later one that worked.
	 *
	 * <p>This is the arrangement's weak point — the reason comes from the last reported error rather
	 * than from a value handed back — so it is the thing worth holding.
	 */
	@Test
	public void aReasonDoesNotOutliveTheExportItBelongsTo() throws Exception {
		GlycanDocument first = documentHolding(compositionWithSialicAcid());
		first.exportFromStructure(first.getStructures(), "wurcs2");
		assertNotNull(first.getLastExportFailureReason(first.getStructures().get(0)));

		GlycanDocument second = documentHolding(compositionThatWrites());
		second.exportFromStructure(second.getStructures(), "wurcs2");

		assertNull("the earlier failure's reason was carried into a later, successful export",
				second.getLastExportFailureReason(second.getStructures().get(0)));
	}

	/** Hex3 HexNAc2 Neu5Ac1 — ordinary, and unwritable until #219 is fixed. */
	private static Glycan compositionWithSialicAcid() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.HEX = 3;
		counts.HEXNAC = 2;
		counts.NEU5AC = 1;

		return counts.getCompositionAsGlycan(neutral());
	}

	/** Hex3 HexNAc2 — the same composition without the sialic acid, which does write. */
	private static Glycan compositionThatWrites() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.HEX = 3;
		counts.HEXNAC = 2;

		return counts.getCompositionAsGlycan(neutral());
	}

	private static MassOptions neutral() {
		MassOptions neutral = new MassOptions();
		neutral.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		neutral.ION_CLOUD = new IonCloud();

		return neutral;
	}

	private static GlycanDocument documentHolding(Glycan structure) {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		document.addStructure(structure);
		document.clearString();

		return document;
	}
}
