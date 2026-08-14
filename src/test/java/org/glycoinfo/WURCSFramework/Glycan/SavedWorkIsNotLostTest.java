package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The two ways a document could quietly lose what was in it, and the setting that outlived its
 * session.
 *
 * <p>#178: opening a second file <em>into</em> the current one left the document counted as
 * unchanged, so no asterisk appeared, Save stayed disabled, and closing threw the merge away without
 * asking. #179: with "Remember files after restarting" on, the reducing end last chosen in the
 * dialog was applied to structures imported afterwards, on sequences that never mentioned one.
 */
public class SavedWorkIsNotLostTest {

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * Merging a file in leaves the document changed, and still called what it was.
	 *
	 * <p>Both halves matter. Unchanged meant it closed without asking; taking the merged file's name
	 * would have meant a later Save writing over a file the user never edited.
	 */
	@Test
	public void mergingAFileInLeavesTheDocumentChanged() throws Exception {
		File first = gwsFile("freeEnd--?b1D-GlcNAc,p$MONO,perMe,Na,0,freeEnd");
		File second = gwsFile("freeEnd--?b1D-Man,p$MONO,perMe,Na,0,freeEnd");

		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue(document.open(first, false, true));
		assertFalse("a document just opened is not changed", document.hasChanged());

		assertTrue(document.open(second, true, true));

		assertTrue("a document with a file merged into it has changed", document.hasChanged());
		assertEquals("it should keep its own file, not take the merged one's",
				first.getAbsolutePath(), document.getFileName());
		assertEquals("both structures should be there", 2, document.getStructures().size());
	}

	/** Opening a file normally still counts as unchanged, and takes that file's name. */
	@Test
	public void openingAFileNormallyIsNotAChange() throws Exception {
		File file = gwsFile("freeEnd--?b1D-GlcNAc,p$MONO,perMe,Na,0,freeEnd");

		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue(document.open(file, false, true));

		assertFalse(document.hasChanged());
		assertEquals(file.getAbsolutePath(), document.getFileName());
	}

	/**
	 * A sequence that names no reducing end gets a free end, whatever was chosen last.
	 *
	 * <p>The mass options handed to the parser carry the dialog's last state, and with the history
	 * setting on that survives a restart - so an import arrived carrying an aglycone from a previous
	 * sitting.
	 */
	@Test
	public void animportedSequenceDoesNotInheritTheLastReducingEnd() throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());

		MassOptions remembered = workspace.getDefaultMassOptions();
		remembered.setReducingEndTypeString("Asn");
		workspace.setDefaultMassOptions(remembered);

		GlycanDocument document = workspace.getStructures();
		assertTrue(document.importFromString(
				"WURCS=2.0/1,1,0/[a2122h-1b_1-5_2*NCC/3=O]/1/", "wurcs2"));

		Glycan imported = document.getStructures().get(0);
		String reducingEnd = imported.getMassOptions().getReducingEndTypeString();

		assertEquals("the sequence named no aglycone, so it should have a free end",
				"freeEnd", reducingEnd);
	}

	private static File gwsFile(String contents) throws Exception {
		File file = File.createTempFile("gb2-", ".gws");
		file.deleteOnExit();
		try (FileWriter out = new FileWriter(file)) {
			out.write(contents);
		}

		return file;
	}
}
