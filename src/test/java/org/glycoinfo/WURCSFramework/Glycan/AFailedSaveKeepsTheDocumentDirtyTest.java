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

	/**
	 * It keeps the group the replaced file had, not the one a new file would get.
	 *
	 * <p>Mode bits are not the whole of who can read a file. A fresh file takes its group from the
	 * platform's rule — the containing directory's on macOS, the creating process's on Linux — rather
	 * than from the file being replaced. So a destination whose group had been set for a team came back
	 * with a different group and an identical mode, which is a loss of access that reading the mode
	 * cannot see. On Linux that is the ordinary case for a shared directory rather than an edge one.
	 */
	@Test
	public void replacingAFileKeepsTheGroupItHad() throws Exception {
		File destination = existingDestination("the previous complete save");
		java.nio.file.Path path = destination.toPath();
		org.junit.Assume.assumeTrue(java.nio.file.Files.getFileStore(path)
				.supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class));

		Object groupBefore = java.nio.file.Files.readAttributes(path,
				java.nio.file.attribute.PosixFileAttributes.class).group();

		Document document = dirtyDocument();
		document.failOnWrite = false;
		assertTrue(document.save(destination.getAbsolutePath()));

		assertEquals("saving should not change the file's group", groupBefore,
				java.nio.file.Files.readAttributes(path,
						java.nio.file.attribute.PosixFileAttributes.class).group());
	}

	/** And the owner, for the same reason. */
	@Test
	public void replacingAFileKeepsTheOwnerItHad() throws Exception {
		File destination = existingDestination("the previous complete save");
		java.nio.file.Path path = destination.toPath();
		org.junit.Assume.assumeTrue(java.nio.file.Files.getFileStore(path)
				.supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class));

		Object ownerBefore = java.nio.file.Files.getOwner(path);

		Document document = dirtyDocument();
		document.failOnWrite = false;
		assertTrue(document.save(destination.getAbsolutePath()));

		assertEquals("saving should not change the file's owner",
				ownerBefore, java.nio.file.Files.getOwner(path));
	}

	/**
	 * And anything an extended attribute says, which a move drops in silence.
	 *
	 * <p>Measured before this was carried over: a marker written to the destination was gone after the
	 * save while the mode looked untouched. Whatever an ACL says would go the same way, and is carried
	 * by the same code — this is the half of it a POSIX filesystem can be made to demonstrate.
	 */
	@Test
	public void replacingAFileKeepsItsExtendedAttributes() throws Exception {
		File destination = existingDestination("the previous complete save");
		java.nio.file.Path path = destination.toPath();

		java.nio.file.attribute.UserDefinedFileAttributeView attributes =
				java.nio.file.Files.getFileAttributeView(path,
						java.nio.file.attribute.UserDefinedFileAttributeView.class);
		org.junit.Assume.assumeNotNull(attributes);
		try {
			attributes.write("glycanbuilder.test",
					java.nio.ByteBuffer.wrap("kept".getBytes(StandardCharsets.UTF_8)));
		} catch (Exception notSupportedHere) {
			org.junit.Assume.assumeNoException(notSupportedHere);
		}

		Document document = dirtyDocument();
		document.failOnWrite = false;
		assertTrue(document.save(destination.getAbsolutePath()));

		assertTrue("the extended attribute did not survive the save: "
				+ java.nio.file.Files.getFileAttributeView(path,
						java.nio.file.attribute.UserDefinedFileAttributeView.class).list(),
				java.nio.file.Files.getFileAttributeView(path,
						java.nio.file.attribute.UserDefinedFileAttributeView.class)
						.list().contains("glycanbuilder.test"));
	}

	/**
	 * Saving to a symbolic link writes into what it points at, and leaves the link a link.
	 *
	 * <p>Opening a file for writing follows a link, so copying into the destination always did. Replacing
	 * the destination does not: measured, a save to {@code link.gws} reported success, left a regular
	 * file where the link had been, and left the {@code target.gws} it pointed at holding the previous
	 * contents. The document the user believed they had saved was not the one on disk.
	 */
	@Test
	public void savingThroughASymbolicLinkWritesIntoItsTarget() throws Exception {
		java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("glycanbuilder-link-");
		java.nio.file.Path target = directory.resolve("target.gws");
		java.nio.file.Path link = directory.resolve("link.gws");
		java.nio.file.Files.write(target, "the previous save".getBytes(StandardCharsets.UTF_8));
		try {
			java.nio.file.Files.createSymbolicLink(link, target.getFileName());
		} catch (Exception notSupportedHere) {
			org.junit.Assume.assumeNoException(notSupportedHere);
		}

		Document document = dirtyDocument();
		document.failOnWrite = false;
		assertTrue("the save should have succeeded", document.save(link.toString()));

		assertTrue("the link should still be a link", java.nio.file.Files.isSymbolicLink(link));
		assertEquals("the file the link points at should hold the new contents",
				"written", contents(target.toFile()));
	}

	/** And a chain of links resolves all the way, rather than one hop. */
	@Test
	public void savingThroughAChainOfLinksReachesTheEnd() throws Exception {
		java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("glycanbuilder-link-");
		java.nio.file.Path target = directory.resolve("target.gws");
		java.nio.file.Path middle = directory.resolve("middle.gws");
		java.nio.file.Path link = directory.resolve("link.gws");
		java.nio.file.Files.write(target, "the previous save".getBytes(StandardCharsets.UTF_8));
		try {
			java.nio.file.Files.createSymbolicLink(middle, target.getFileName());
			java.nio.file.Files.createSymbolicLink(link, middle.getFileName());
		} catch (Exception notSupportedHere) {
			org.junit.Assume.assumeNoException(notSupportedHere);
		}

		Document document = dirtyDocument();
		document.failOnWrite = false;
		assertTrue(document.save(link.toString()));

		assertTrue("both links should still be links", java.nio.file.Files.isSymbolicLink(link)
				&& java.nio.file.Files.isSymbolicLink(middle));
		assertEquals("written", contents(target.toFile()));
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
