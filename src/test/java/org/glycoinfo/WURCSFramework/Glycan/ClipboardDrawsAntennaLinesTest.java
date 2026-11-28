package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.GlycanSelection;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * The drawing copied to the clipboard is the drawing on the canvas (#231).
 *
 * <p>An antenna of uncertain parent is drawn with its line only when it has candidate parents, and the
 * canvas's copy of a selection, which is what is drawn onto the clipboard, gave it none: pasted into
 * Word or GitHub, the uncertain terminal residue had no line, though an exported PNG had one. #368
 * gave the copy its candidate parents; this keeps it so. The SVG on the clipboard also showed the
 * reducing end only when the masses were shown.
 */
public class ClipboardDrawsAntennaLinesTest {

	/** #230's structure: a Man9 with two Gal of uncertain parent, one of them at 6. */
	private static final String GWS = "freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p(--3a1D-Man,p--2a1D-Man,p"
			+ "--2a1D-Man,p)--6a1D-Man,p(--3a1D-Man,p--2a1D-Man,p)--6a1D-Man,p--2a1D-Man,p}(--6?1D-Gal,p)--??1D-Gal,p"
			+ "$MONO,perMe,Na,0,freeEnd";

	private BuilderWorkspace workspace;
	private GlycanRendererAWT renderer;
	private GlycanDocument document;
	private Glycan original;

	@Before
	public void open() throws Exception {
		renderer = new GlycanRendererAWT();
		workspace = new BuilderWorkspace(renderer);
		workspace.setAutoSave(false);
		document = workspace.getStructures();
		document.setStructures(new ArrayList<Glycan>(document.parseString(GWS)));
		original = document.getStructures().iterator().next();
	}

	private Collection<Glycan> copyOfAll() {
		return document.extractView(new HashSet<Residue>(original.getAllResidues()));
	}

	/** The copy's antennae have candidate parents, which is what draws their line. */
	@Test
	public void theCopysAntennaeHaveTheirParents() {
		Glycan copy = copyOfAll().iterator().next();
		for (Linkage antenna : copy.getBracket().getChildrenLinkages())
			assertFalse("an antenna with no candidate parent is drawn with no line",
					antenna.getChildResidue().getParentsOfFragment().isEmpty());
	}

	/** The SVG on the clipboard is the SVG of the structure itself. */
	@Test
	public void theClipboardsDrawingIsTheStructures() {
		java.util.List<Glycan> structure = new ArrayList<Glycan>();
		structure.add(original);
		String drawn = SVGUtils.getVectorGraphics(renderer, structure, false, false);
		String copied = new GlycanSelection(renderer, copyOfAll()).getSVG();
		// 13: the eleven bonds of Man9GlcNAc2 and the two antennae's; without theirs, 11
		assertEquals(13, lines(drawn));
		assertEquals(lines(drawn), lines(copied));
	}

	/** The SVG on the clipboard shows the reducing end as the options say, whatever they say of masses. */
	@Test
	public void theReducingEndIsShownAsTheOptionsSay() {
		renderer.getGraphicOptions().SHOW_MASSES = false;
		renderer.getGraphicOptions().SHOW_REDEND = true;
		Collection<Glycan> copy = copyOfAll();
		assertEquals(SVGUtils.getVectorGraphics(renderer, copy, false, true), new GlycanSelection(renderer, copy).getSVG());
	}

	/** How many linkage lines an SVG draws: the renderer draws each as a polygon. */
	private static int lines(String _svg) {
		int count = 0;
		for (int at = _svg.indexOf("<polygon"); at >= 0; at = _svg.indexOf("<polygon", at + 1)) count++;
		return count;
	}
}
