package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.GlycanBuilder;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That an export to a sequence format reports what happened.
 *
 * <p>The sequence branch of Export To called {@code exportTo} and returned {@code true} regardless of
 * what it said, so an export that wrote nothing reported success. The graphical formats in the same
 * method had always returned their real result — only the sequence formats lied.
 *
 * <p>It also made a liar of the warning that follows a successful export, whose whole purpose is to
 * tell a user which structures did not come out.
 *
 * <p>The failure is a destination that cannot be written — a directory — rather than a permission bit,
 * so it is deterministic and needs no privileged setup.
 */
public class AFailedSequenceExportSaysSoTest {

	private static final String A_GLYCAN = "freeEnd--?b1D-GlcNAc,p--4b1D-Gal,p$MONO,Und,0,0,freeEnd";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** An export that cannot be written is reported as failed. */
	@Test
	public void anExportThatCannotBeWrittenIsReportedAsFailed() throws Exception {
		assertFalse("a sequence export to an unwritable destination should be reported as false",
				GlycanBuilder.exportSequenceTo(documentWithAGlycan(),
						aDirectory().getAbsolutePath(), "wurcs2"));
	}

	/** And one that works, as working — the half that says the fix did not refuse everything. */
	@Test
	public void anExportThatWorksIsReportedAsWorking() throws Exception {
		File destination = File.createTempFile("glycanbuilder-export-", ".wurcs");
		destination.deleteOnExit();

		assertTrue("an ordinary sequence export should be reported as true",
				GlycanBuilder.exportSequenceTo(documentWithAGlycan(),
						destination.getAbsolutePath(), "wurcs2"));
		assertTrue("and should have written something", destination.length() > 0);
	}

	/** No document is no export. */
	@Test
	public void noDocumentIsNoExport() throws Exception {
		File destination = File.createTempFile("glycanbuilder-export-", ".wurcs");
		destination.deleteOnExit();

		assertFalse(GlycanBuilder.exportSequenceTo(null, destination.getAbsolutePath(), "wurcs2"));
	}

	private static GlycanDocument documentWithAGlycan() throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue(document.importFromString(A_GLYCAN, "gws"));

		return document;
	}

	/** A destination that is not a file, which is the simplest write failure to arrange. */
	private static File aDirectory() throws Exception {
		File directory = File.createTempFile("glycanbuilder-export-", "");
		assertTrue(directory.delete());
		assertTrue(directory.mkdir());
		directory.deleteOnExit();

		return directory;
	}
}
