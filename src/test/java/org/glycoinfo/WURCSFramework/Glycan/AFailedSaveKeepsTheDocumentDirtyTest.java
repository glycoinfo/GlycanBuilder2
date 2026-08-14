package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import javax.swing.filechooser.FileFilter;

import org.eurocarbdb.application.glycanbuilder.BaseDocument;
import org.junit.Test;

/**
 * That a save which did not happen is not reported as one.
 *
 * <p>{@code save} called {@code setFilename} before writing anything, and {@code setFilename} sets
 * {@code was_saved} and clears {@code has_changed} as a side effect. So an unwritable destination, a
 * full disk or a serialization failure returned {@code false} while leaving the document marked saved
 * and clean: the asterisk went away, Save went grey, and the next close let the work go without asking.
 *
 * <p>The failure is injected through {@code write}, which is the seam a real one arrives at — no
 * filesystem is filled and no permissions are relied on.
 */
public class AFailedSaveKeepsTheDocumentDirtyTest {

	/** A write that fails leaves the save reporting failure. */
	@Test
	public void aFailedWriteIsAFailedSave() throws Exception {
		Document document = dirtyDocument();

		assertFalse("a save whose write threw should return false",
				document.save(destination().getAbsolutePath()));
	}

	/** And leaves the document exactly as dirty, as named and as unsaved as it was. */
	@Test
	public void aFailedSaveChangesNothingAboutTheDocument() throws Exception {
		Document document = dirtyDocument();
		String nameBefore = document.getFileName();
		boolean savedBefore = document.wasSaved();

		document.save(destination().getAbsolutePath());

		assertTrue("the document should still be dirty", document.hasChanged());
		assertEquals("the filename should not have moved to the destination",
				nameBefore, document.getFileName());
		assertEquals("wasSaved should not have changed", savedBefore, document.wasSaved());
	}

	/** The destination is not created, let alone truncated, by a save that could not be written. */
	@Test
	public void aFailedSaveLeavesTheDestinationAlone() throws Exception {
		File destination = destination();

		dirtyDocument().save(destination.getAbsolutePath());

		assertFalse("the destination should not have been written", destination.exists());
	}

	/** The temporary file the attempt made is its own to clean up, and it does. */
	@Test
	public void aFailedSaveLeavesNoTemporaryFileBehind() throws Exception {
		File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));
		String[] before = temporaryDirectory.list(gwbTemporaries());

		dirtyDocument().save(destination().getAbsolutePath());

		String[] after = temporaryDirectory.list(gwbTemporaries());
		assertEquals("a temporary file was left behind",
				(before == null) ? 0 : before.length, (after == null) ? 0 : after.length);
	}

	/** And a save that works still does everything it did: the name, the flags, the file. */
	@Test
	public void aSuccessfulSaveStillTakesTheFilenameAndGoesClean() throws Exception {
		Document document = dirtyDocument();
		document.failOnWrite = false;
		File destination = destination();

		assertTrue("the save should have succeeded", document.save(destination.getAbsolutePath()));

		assertEquals(destination.getAbsolutePath(), document.getFileName());
		assertTrue("the document should be marked saved", document.wasSaved());
		assertFalse("the document should be clean", document.hasChanged());
		assertTrue("the destination should exist", destination.exists());
	}

	private static Document dirtyDocument() {
		Document document = new Document();
		document.setChanged(true);
		return document;
	}

	/** A path in the temporary directory that nothing has created. */
	private static File destination() throws Exception {
		File destination = File.createTempFile("glycanbuilder-save-", ".tmp");
		assertTrue(destination.delete());
		destination.deleteOnExit();

		return destination;
	}

	private static java.io.FilenameFilter gwbTemporaries() {
		return new java.io.FilenameFilter() {
			@Override
			public boolean accept(File directory, String name) {
				return name.startsWith("gwb");
			}
		};
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
