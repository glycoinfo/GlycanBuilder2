package org.glycoinfo.application.glycanbuilder.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Checking for a newer release: reading GitHub's answer, comparing versions, and knowing where to
 * send someone.
 *
 * <p>Nothing here reaches the network. What the live check does is fetch one redirect; what it then
 * does with the answer is all below, and is where it could be wrong quietly - announcing an update
 * that is not one, or missing one that is.
 */
public class UpdateCheckTest {

	/** The application knows what it was built as; the About window has shown it all along. */
	@Test
	public void itKnowsItsOwnVersion() {
		String version = ApplicationVersion.get();

		assertFalse("the version was not filled in at build time: " + version,
				ApplicationVersion.UNKNOWN.equals(version));
		assertTrue("not a version: " + version, version.matches("\\d+\\.\\d+.*"));
	}

	/** GitHub answers with the tag's page; the version is the tail of it, without the "v". */
	@Test
	public void itReadsTheVersionOutOfTheRedirect() {
		assertEquals("1.34.3", UpdateCheck.versionFromTagUrl(
				"https://github.com/glycoinfo/GlycanBuilder2/releases/tag/v1.34.3"));
		assertEquals("2.0.0", UpdateCheck.versionFromTagUrl(
				"https://github.com/glycoinfo/GlycanBuilder2/releases/tag/2.0.0"));
	}

	/** And says nothing rather than something wrong when the answer is not one it can read. */
	@Test
	public void anAnswerItCannotReadIsNoAnswer() {
		assertNull(UpdateCheck.versionFromTagUrl(null));
		assertNull(UpdateCheck.versionFromTagUrl(""));
		assertNull(UpdateCheck.versionFromTagUrl("https://github.com/.../releases/tag/"));
	}

	/**
	 * Compared part by numeric part, not as text.
	 *
	 * <p>The case that matters is the tenth patch: as strings "1.34.10" sorts before "1.34.9", so a
	 * text comparison would stop telling anyone about updates the moment a version reached double
	 * digits - quietly, and only then.
	 */
	@Test
	public void versionsAreComparedAsNumbers() {
		assertTrue(UpdateCheck.isNewer("1.34.10", "1.34.9"));
		assertTrue(UpdateCheck.isNewer("1.35.0", "1.34.3"));
		assertTrue(UpdateCheck.isNewer("2.0.0", "1.34.3"));
		assertTrue("a shorter version is the same as trailing zeros",
				UpdateCheck.isNewer("1.35", "1.34.3"));
	}

	/** The same version, or an older one, is not an update. */
	@Test
	public void whatIsNotNewerIsNotAnUpdate() {
		assertFalse(UpdateCheck.isNewer("1.34.3", "1.34.3"));
		assertFalse(UpdateCheck.isNewer("1.34.2", "1.34.3"));
		assertFalse(UpdateCheck.isNewer("1.34", "1.34.0"));
	}

	/**
	 * Anything that cannot be compared is not announced.
	 *
	 * <p>Better to say nothing than to send someone to a download page over a version this could not
	 * make sense of - including when the running version is unknown, which is what happens outside a
	 * built artifact.
	 */
	@Test
	public void whatCannotBeComparedIsNotAnnounced() {
		assertFalse(UpdateCheck.isNewer(null, "1.34.3"));
		assertFalse(UpdateCheck.isNewer("1.34.4", null));
		assertFalse(UpdateCheck.isNewer("1.34.4", ApplicationVersion.UNKNOWN));
		assertFalse("a tag that is not a version", UpdateCheck.isNewer("nightly", "1.34.3"));
		assertFalse(UpdateCheck.isNewer("1.34.4-rc1", "1.34.3"));
	}

	/**
	 * Where a person is sent depends on how their copy was published.
	 *
	 * <p>Windows goes to the Microsoft Store, and this is the assertion worth having: the releases
	 * carry no Windows installer - the workflow excludes it deliberately - so sending a Windows user
	 * to GitHub would offer them nothing, and sending a Store user outside the Store would be wrong
	 * besides.
	 */
	@Test
	public void eachPlatformIsSentWhereItsBuildsAre() {
		String os = System.getProperty("os.name");
		try {
			System.setProperty("os.name", "Windows 11");
			assertEquals(UpdateCheck.DownloadPage.WINDOWS_STORE,
					UpdateCheck.DownloadPage.forThisPlatform());

			System.setProperty("os.name", "Mac OS X");
			assertEquals(UpdateCheck.DownloadPage.MACOS, UpdateCheck.DownloadPage.forThisPlatform());

			System.setProperty("os.name", "Linux");
			assertEquals(UpdateCheck.DownloadPage.RELEASES,
					UpdateCheck.DownloadPage.forThisPlatform());

			// A jar run directly, or a build from source: the releases page lists what there is.
			System.setProperty("os.name", "SunOS");
			assertEquals(UpdateCheck.DownloadPage.RELEASES,
					UpdateCheck.DownloadPage.forThisPlatform());
		} finally {
			System.setProperty("os.name", os);
		}
	}
}
