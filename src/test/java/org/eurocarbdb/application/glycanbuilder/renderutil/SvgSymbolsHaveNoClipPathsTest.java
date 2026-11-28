package org.eurocarbdb.application.glycanbuilder.renderutil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.junit.Test;

/**
 * A symbol in an SVG is its own shapes, with no clip-path for a program to drop (#187).
 *
 * <p>A coloured symbol was painted as the whole of its fill's box, clipped to the symbol's shape. In
 * an SVG that is a rect with a clip-path, and PowerPoint drops clip-paths when a picture is
 * ungrouped: a Man or a Fuc came apart into a coloured square. The SVG now carries the coloured part
 * as that part's own shape. Screen and bitmaps are painted as before.
 */
public class SvgSymbolsHaveNoClipPathsTest {

	/** An N-glycan with circles, squares, a triangle, a diamond and a divided diamond. */
	private static final String N_GLYCAN = "freeEnd--?b1D-GlcNAc,p(--4b1D-GlcNAc,p--4b1D-Man,p(--3a1D-Man,p"
			+ "--2b1D-GlcNAc,p--4b1D-GlcA,p)--6a1D-Man,p--2b1D-GalNAc,p--4b1D-Gal,p--3a2D-NeuAc,p)--6a1L-Fuc,p"
			+ "$MONO,Und,0,0,freeEnd";

	@Test
	public void noClipPathsInSnfgOrCfg() throws Exception {
		for (String notation : new String[] { GraphicOptions.NOTATION_SNFG, GraphicOptions.NOTATION_CFG }) {
			String svg = svg(notation);
			assertFalse(notation + ": a clip-path", svg.contains("clip-path"));
			assertFalse(notation + ": a clipPath", svg.contains("<clipPath"));
		}
	}

	/** Man's green is not painted as a square: the coloured part of a circle is not a rect. */
	@Test
	public void aCircleIsNotASquare() throws Exception {
		String svg = svg(GraphicOptions.NOTATION_SNFG);
		Matcher rects = Pattern.compile("<rect[^>]*>").matcher(svg);
		while (rects.find())
			assertFalse(rects.group(), rects.group().contains("fill:rgb(0,166,81)"));
		assertTrue("Man's green is not there at all", svg.contains("fill:rgb(0,166,81)"));
	}

	private static String svg(String notation) {
		GlycanRendererAWT renderer = new GlycanRendererAWT();
		BuilderWorkspace workspace = new BuilderWorkspace(renderer);
		workspace.setAutoSave(false);
		workspace.setNotation(notation);
		Glycan structure = Glycan.fromString(N_GLYCAN);
		return SVGUtils.getVectorGraphics((GlycanRendererAWT) workspace.getGlycanRenderer(),
				Collections.singletonList(structure), false, true);
	}
}
