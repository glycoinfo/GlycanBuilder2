package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That every vector format writes something, and that a format nobody has is refused rather than
 * written as nothing (#185).
 *
 * <p>The transcoders report a failure by logging it and returning null, and the export wrote that
 * null - so a PDF or an EPS that could not be made came out as a 0-byte file with no message. The
 * file had already been created by then, which is what made it look like a successful export of an
 * empty picture.
 *
 * <p>The underlying failure was reported on Windows and does not reproduce here: all five formats
 * write on this platform with the batik and fop versions currently pinned. What is held here is the
 * half that is platform-independent - that a failure is not delivered as an empty file.
 */
public class AFailedExportSaysSoTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void newWorkspace() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Each format writes a file with something in it. */
	@Test
	public void everyFormatWritesSomething() throws Exception {
		for (String format : new String[] { "svg", "png", "pdf", "ps", "eps" }) {
			ByteArrayOutputStream written = new ByteArrayOutputStream();
			SVGUtils.export(written, (GlycanRendererAWT) workspace.getGlycanRenderer(), structures(),
					false, true, 1.0, format, new PositionManager(), new BBoxManager());

			assertTrue(format + " wrote nothing", written.size() > 0);
		}
	}

	/** And a format that is not one is refused by name, rather than writing an empty file. */
	@Test
	public void aFormatThatIsNotOneIsRefused() {
		try {
			SVGUtils.export(new ByteArrayOutputStream(),
					(GlycanRendererAWT) workspace.getGlycanRenderer(), structures(), false, true, 1.0,
					"tiff", new PositionManager(), new BBoxManager());
			fail("tiff was accepted");
		} catch (Exception refused) {
			assertTrue(String.valueOf(refused.getMessage()),
					String.valueOf(refused.getMessage()).contains("tiff"));
		}
	}

	private static Collection<Glycan> structures() {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		try {
			document.importFromString("freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p", "gws");
		} catch (Exception cannotRead) {
			throw new IllegalStateException(cannotRead);
		}

		return document.getStructures();
	}
}
