package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import javax.swing.filechooser.FileFilter;

import org.eurocarbdb.application.glycanbuilder.BaseDocument;
import org.eurocarbdb.application.glycanbuilder.GlycanBuilder;
import org.junit.Test;

/**
 * That a failed Save is reported to whoever asked for it.
 *
 * <p>The Save branch for a document with a writable file called {@code save} and then returned
 * {@code true} regardless of what it said. This is the branch "Save changes to …?" takes when a user
 * answers Yes on exit, so an I/O failure read as a successful save and the application was allowed to
 * close over work that had never been written.
 *
 * <p>Two separate faults needed two separate fixes and need two separate tests: the model marking an
 * unwritten document as saved ({@code AFailedSaveKeepsTheDocumentDirtyTest}) and this, the caller
 * throwing the answer away. Fixing either alone leaves the other.
 *
 * <p>Tested through {@code GlycanBuilder.saveToItsOwnFile}, which is the decision without the window
 * around it. Constructing the frame would need a display and would test the file chooser.
 */
public class AFailedSaveStopsTheCloseTest {

	/** A save that fails is reported as failing. */
	@Test
	public void aFailedSaveIsReportedAsFailed() throws Exception {
		Document document = savedDocumentIn(existingWritableFile());

		assertEquals("a failed save should be reported as false",
				Boolean.FALSE, GlycanBuilder.saveToItsOwnFile(document));
	}

	/** And one that works, as working. */
	@Test
	public void aSuccessfulSaveIsReportedAsSuccessful() throws Exception {
		Document document = savedDocumentIn(existingWritableFile());
		document.failOnWrite = false;

		assertEquals(Boolean.TRUE, GlycanBuilder.saveToItsOwnFile(document));
	}

	/**
	 * A document with no file of its own is neither, and says so: the caller offers Save As.
	 *
	 * <p>This is the case the discarded return value had been written for, and it still works — which
	 * is the half that says the fix did not turn every failure into a file chooser.
	 */
	@Test
	public void aDocumentWithNoFileAsksTheCallerToChooseOne() {
		assertNull("a document with no writable file should fall through to Save As",
				GlycanBuilder.saveToItsOwnFile(new Document()));
	}

	/** And so is no document at all. */
	@Test
	public void noDocumentAsksNothing() {
		assertNull(GlycanBuilder.saveToItsOwnFile(null));
	}

	/** A document that believes it came from this file, so {@code getFile} hands it back. */
	private static Document savedDocumentIn(File file) {
		Document document = new Document();
		document.setFilename(file.getAbsolutePath());
		document.setChanged(true);

		return document;
	}

	private static File existingWritableFile() throws Exception {
		File file = File.createTempFile("glycanbuilder-close-", ".tmp");
		file.deleteOnExit();

		FileWriter writer = new FileWriter(file);
		try {
			writer.write("something that was already there");
		} finally {
			writer.close();
		}

		return file;
	}

	/** The smallest document there can be, with a write that can be told to fail. */
	private static final class Document extends BaseDocument {

		private boolean failOnWrite = true;

		@Override
		public void write(OutputStream os) throws Exception {
			if (failOnWrite) throw new Exception("the disk said no");
			os.write("written".getBytes("UTF-8"));
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public String getName() {
			return "Test document";
		}

		@Override
		public void initData() {
		}

		@Override
		public void fromString(String str, boolean merge) throws Exception {
		}

		@Override
		public Collection<FileFilter> getFileFormats() {
			return Collections.emptyList();
		}

		@Override
		public FileFilter getAllFileFormats() {
			return null;
		}
	}
}
