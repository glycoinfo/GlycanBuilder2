package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That the PDF, PS and EPS exports actually produce their formats.
 *
 * <p>These are the only callers of Apache FOP, and none of them was exercised by any test - so a
 * FOP upgrade could only be judged by whether the code still compiled. It is exercised here at
 * runtime: a structure is transcoded and the output has to begin with the magic bytes of the format
 * it claims to be. This is what let FOP move off 2.0, which carries CVE-2017-5661 (an XXE in its
 * readers), with something more than compilation as evidence.</p>
 */
public class TranscoderSmokeTest {

	private static BuilderWorkspace workspace;
	private static Glycan chitobiose;

	@BeforeClass
	public static void newWorkspace() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setNotation("snfg");
		chitobiose = Glycan.fromString("freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p$MONO,Und,0,0,freeEnd");
	}

	/** A PDF starts with %PDF, or it is not a PDF. */
	@Test
	public void thePdfExportIsAPdf() {
		byte[] pdf = SVGUtils.getPDFGraphics(
				workspace.getGlycanRenderer(), Collections.singleton(chitobiose));

		assertTrue("nothing came out", pdf != null && pdf.length > 100);
		assertTrue("does not start with %PDF", startsWith(pdf, "%PDF"));
	}

	/** PostScript announces itself with %!PS. */
	@Test
	public void thePsExportIsPostScript() {
		byte[] ps = SVGUtils.getPSGraphics(
				workspace.getGlycanRenderer(), Collections.singleton(chitobiose));

		assertTrue("nothing came out", ps != null && ps.length > 100);
		assertTrue("does not start with %!PS", startsWith(ps, "%!PS"));
	}

	/** And EPS is PostScript with the EPSF marker on its first line. */
	@Test
	public void theEpsExportIsEncapsulatedPostScript() {
		byte[] eps = SVGUtils.getEPSGraphics(
				workspace.getGlycanRenderer(), Collections.singleton(chitobiose));

		assertTrue("nothing came out", eps != null && eps.length > 100);
		assertTrue("does not start with %!PS", startsWith(eps, "%!PS"));
		assertTrue("first line carries no EPSF marker",
				new String(eps, 0, Math.min(80, eps.length), StandardCharsets.US_ASCII)
						.contains("EPSF"));
	}

	private static boolean startsWith(byte[] bytes, String magic) {
		byte[] expected = magic.getBytes(StandardCharsets.US_ASCII);
		if (bytes.length < expected.length) return false;
		for (int at = 0; at < expected.length; at++) {
			if (bytes[at] != expected[at]) return false;
		}

		return true;
	}
}
