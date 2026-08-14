package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

	/**
	 * The destination is replaced rather than written into.
	 *
	 * <p>This is the assertion that tells the two implementations apart, and the reason the two above it
	 * do not: writing to a temporary file first has always meant that a *serialization* failure left the
	 * destination alone, so they pass against the old code as well. What was not transactional was the
	 * step after it — copying the finished bytes *into* the destination, which truncates it first, so a
	 * failure partway through left the last good save half-written.
	 *
	 * <p>A move cannot half-happen. It is observable as the file's identity: copying into a file keeps
	 * its inode, moving a new file over it does not. Skipped where the filesystem has no such notion,
	 * rather than asserted loosely enough to pass everywhere.
	 */
	@Test
	public void theDestinationIsReplacedRatherThanWrittenInto() throws Exception {
		File destination = existingDestination("the previous complete save");
		Object inodeBefore = inodeOf(destination);
		org.junit.Assume.assumeNotNull(inodeBefore);

		Document document = dirtyDocument();
		document.failOnWrite = false;
		assertTrue(document.save(destination.getAbsolutePath()));

		assertEquals("the new contents should be there", "written", contents(destination));
		assertFalse("the destination was written into rather than replaced — a copy into an existing"
				+ " file cannot be transactional, since it truncates before it writes",
				inodeBefore.equals(inodeOf(destination)));
	}

	/**
	 * And it keeps the permissions the replaced file had.
	 *
	 * <p>A move puts a new file in place of the old one, so without this the saved document carries the
	 * temporary file's permissions — and a fresh temporary file is owner-only. A document in a shared
	 * directory went from {@code rw-r--r--} to {@code rw-------} the first time it was saved, and
	 * everyone but its owner lost work they had been reading. That is not a change a save should make.
	 */
	@Test
	public void replacingAFileKeepsThePermissionsItHad() throws Exception {
		File destination = existingDestination("the previous complete save");
		java.nio.file.Path path = destination.toPath();
		org.junit.Assume.assumeTrue(java.nio.file.Files.getFileStore(path)
				.supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class));

		java.nio.file.Files.setPosixFilePermissions(path,
				java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));

		Document document = dirtyDocument();
		document.failOnWrite = false;
		assertTrue(document.save(destination.getAbsolutePath()));

		assertEquals("saving should not change who can read the file", "rw-r--r--",
				java.nio.file.attribute.PosixFilePermissions.toString(
						java.nio.file.Files.getPosixFilePermissions(path)));
	}

	/** An existing good save is not truncated when the replacement cannot be serialized. */
	@Test
	public void aFailedSaveDoesNotDamageThePreviousFile() throws Exception {
		File destination = existingDestination("the previous complete save");

		dirtyDocument().save(destination.getAbsolutePath());

		assertEquals("the previous file should remain byte-for-byte intact",
				"the previous complete save", contents(destination));
	}

	/** A failure while replacing the destination also cleans up the completed temporary file. */
	@Test
	public void aFailedReplacementLeavesNoTemporaryFileBehind() throws Exception {
		File destination = File.createTempFile("glycanbuilder-save-directory-", "");
		assertTrue(destination.delete());
		assertTrue(destination.mkdir());
		File child = new File(destination,"keep");
		assertTrue(child.createNewFile());
		destination.deleteOnExit();
		child.deleteOnExit();

		File temporaryDirectory = destination.getAbsoluteFile().getParentFile();
		String[] before = temporaryDirectory.list(gwbTemporaries());
		Document document = dirtyDocument();
		document.failOnWrite = false;

		assertFalse("a non-empty directory cannot be replaced by a saved document",
				document.save(destination.getAbsolutePath()));

		String[] after = temporaryDirectory.list(gwbTemporaries());
		assertEquals("a completed temporary file was left behind after replacement failed",
				(before == null) ? 0 : before.length, (after == null) ? 0 : after.length);
		assertTrue("the destination directory should still exist", destination.isDirectory());
		assertTrue("the destination's previous content should still exist", child.exists());
		assertTrue("the document should remain dirty", document.hasChanged());
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
		assertEquals("written", contents(destination));
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

	private static File existingDestination(String contents) throws Exception {
		File destination = File.createTempFile("glycanbuilder-save-", ".tmp");
		destination.deleteOnExit();
		FileWriter writer = new FileWriter(destination);
		try {
			writer.write(contents);
		} finally {
			writer.close();
		}
		return destination;
	}

	private static String contents(File file) throws Exception {
		return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
	}

	/**
	 * The file's identity on the filesystem, or {@code null} where it has no such notion.
	 *
	 * <p>Two files with the same path and different inodes are two different files, which is the whole
	 * difference between replacing a file and writing into it.
	 */
	private static Object inodeOf(File file) {
		try {
			return Files.getAttribute(file.toPath(),"unix:ino");
		} catch (Exception noSuchNotion) {
			return null;
		}
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
