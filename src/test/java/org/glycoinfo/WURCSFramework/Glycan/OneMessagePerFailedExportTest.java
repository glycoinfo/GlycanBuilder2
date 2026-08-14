package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a structure which cannot be exported is only converted once (#183).
 *
 * <p>The export wrote everything for the file and then wrote it all over again to find out which
 * structures had come out empty. A structure that cannot be converted says so on the way through, and
 * {@code LogUtils.report} answers one failure with two windows — the message, then the stack — so a
 * second pass produced a second pair. That is the "two sets (error+exception)" per failed structure the
 * report describes, and with two bad structures, four.
 *
 * <p><b>The count is what this tests.</b> Asserting on dialogs would mean driving Swing; asserting that
 * each structure is handed to the parser exactly once says the same thing about the cause, and it was
 * the cause that was doubled.
 *
 * <p><b>The sequence in the report no longer fails.</b> {@code WURCS=2.0/1,1,0/[A111h]/1/} writes back
 * unchanged as of 1.37.0, so the skeleton-code error it was reported with is gone and those steps
 * produce no dialogs at all. The doubling was still there to be found by reading, and would have
 * doubled the next failure just as well — which is why it is fixed here rather than closed as
 * not-reproducible.
 */
public class OneMessagePerFailedExportTest {

	private static final String ORDINARY_GLYCAN =
			"freeEnd--?b1D-GlcNAc,p--4b1D-Gal,p$MONO,Und,0,0,freeEnd";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * Each structure is handed to the parser once, whether it converts or not.
	 *
	 * <p>Before the change this counted two per structure.
	 */
	@Test
	public void everyStructureIsConvertedOnce() throws Exception {
		CountingParser parser = new CountingParser();
		GlycanDocument.toString(twoStructures(), parser, null, new ArrayList<Glycan>());

		assertEquals("each structure should be converted once", 2, parser.calls);
	}

	/** The ones that produced nothing are still named, which is what the warning dialog reads. */
	@Test
	public void theEmptyOnesAreStillNamed() throws Exception {
		LinkedList<Glycan> structures = twoStructures();
		Glycan bad = structures.getLast();

		CountingParser parser = new CountingParser();
		parser.silentAbout = bad;
		List<Glycan> failures = new ArrayList<Glycan>();
		GlycanDocument.toString(structures, parser, null, failures);

		assertEquals("the structure that produced nothing should be named once", 1, failures.size());
		assertTrue("the wrong structure was named", failures.contains(bad));
		assertEquals("and it should still have been converted only once", 2, parser.calls);
	}

	/**
	 * An ordinary export is unaffected: both structures in the file, nothing reported as failed.
	 *
	 * <p>Through the real writer, since the point of collecting the failures during the single pass is
	 * that it still fills in what the warning needs.
	 */
	@Test
	public void anOrdinaryExportStillWritesEverything() throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue(document.importFromString(ORDINARY_GLYCAN, "gws"));
		assertTrue(document.importFromString("WURCS=2.0/1,1,0/[A111h]/1/", "wurcs2"));

		File file = File.createTempFile("glycanbuilder-183-", ".wurcs");
		file.deleteOnExit();
		assertTrue("the export failed outright", document.exportTo(file.getAbsolutePath(), "wurcs2"));

		String written = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		assertEquals("both structures should have been written",
				2, written.trim().split("\\R").length);
		assertTrue("nothing should have been reported as failed: "
				+ document.getLastExportFailures(), document.getLastExportFailures().isEmpty());
	}

	private static LinkedList<Glycan> twoStructures() throws Exception {
		LinkedList<Glycan> structures = new LinkedList<Glycan>();
		structures.add(Glycan.fromString(ORDINARY_GLYCAN));
		structures.add(Glycan.fromString(ORDINARY_GLYCAN));
		return structures;
	}

	/**
	 * Counts what it is asked to write, and can be told to produce nothing for one structure.
	 *
	 * <p>A {@link WURCS2Parser} rather than a bare {@code GlycanParser}, because the encoder picks its
	 * loop by the parser's type and only the sequence formats write more than the first structure.
	 */
	private static final class CountingParser extends WURCS2Parser {

		private int calls;
		private Glycan silentAbout;

		@Override
		public String writeGlycan(Glycan structure) {
			calls++;
			return (structure == silentAbout) ? "" : "written";
		}
	}
}
