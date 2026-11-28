package org.eurocarbdb.application.glycanbuilder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A sequence export keeps the .txt it was given (#237).
 *
 * <p>The save dialog for WURCS offers a .txt filter, and the name chosen was then checked against
 * the format's name, wurcs2, rather than txt: 1.txt was saved as 1.txt.txt, and choosing that file
 * to overwrite gave 1.txt.txt.txt. Images and GWS files were not affected.
 */
public class ExportFileExtensionTest {

	/** A name that already ends in .txt is kept, whatever the sequence format. */
	@Test
	public void aTextFileKeepsItsName() {
		assertEquals("/data/1.txt", FileUtils.enforceExtension("/data/1.txt", "wurcs2"));
		assertEquals("/data/1.txt.txt", FileUtils.enforceExtension("/data/1.txt.txt", "wurcs2"));
		assertEquals("/data/1.TXT", FileUtils.enforceExtension("/data/1.TXT", "wurcs2"));
		assertEquals("/data/1.txt", FileUtils.enforceExtension("/data/1.txt", "GWS"));
	}

	/** A name without it is given .txt, as before. */
	@Test
	public void aNameWithoutItIsGivenTxt() {
		assertEquals("/data/1.txt", FileUtils.enforceExtension("/data/1", "wurcs2"));
		assertEquals("/data/1.wurcs.txt", FileUtils.enforceExtension("/data/1.wurcs", "wurcs2"));
	}

	/** Images and documents keep their own extensions, as before. */
	@Test
	public void otherFormatsAreUnchanged() {
		assertEquals("/data/1.png", FileUtils.enforceExtension("/data/1.png", "png"));
		assertEquals("/data/1.PNG", FileUtils.enforceExtension("/data/1.PNG", "png"));
		assertEquals("/data/1.svg", FileUtils.enforceExtension("/data/1", "svg"));
		assertEquals("/data/1.gws", FileUtils.enforceExtension("/data/1.gws", "gws"));
		assertEquals("/data/1.gws", FileUtils.enforceExtension("/data/1", "gws"));
	}
}
