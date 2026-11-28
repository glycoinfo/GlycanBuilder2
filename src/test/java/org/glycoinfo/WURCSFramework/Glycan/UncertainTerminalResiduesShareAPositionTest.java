package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ListModel;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanCanvas;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ThemeManager;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * Two uncertain terminal residues can both be at 6 (#230).
 *
 * <p>The residues on a bracket each sit on one of the antennae, which one not known. Two Gal there at
 * 6 are at two different 6s. The canvas's position list and the residue's own rules counted the
 * bracket as the parent they share, so once one Gal was set to 6, 6 was gone from the other's list.
 */
public class UncertainTerminalResiduesShareAPositionTest {

	/** The structure of #230, as M. Matsubara gave its GWS: one Gal set to 6, the other not yet. */
	private static final String ONE_AT_6 = "freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p(--3a1D-Man,p--2a1D-Man,p"
			+ "--2a1D-Man,p)--6a1D-Man,p(--3a1D-Man,p--2a1D-Man,p)--6a1D-Man,p--2a1D-Man,p}(--6?1D-Gal,p)--??1D-Gal,p"
			+ "$MONO,perMe,Na,0,freeEnd";

	private BuilderWorkspace workspace;
	private Glycan structure;
	private Residue first;
	private Residue second;

	@Before
	public void read() throws Exception {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		structure = Glycan.fromString(ONE_AT_6);
		List<Residue> onTheBracket = new ArrayList<Residue>();
		for (Linkage link : structure.getBracket().getChildrenLinkages())
			onTheBracket.add(link.getChildResidue());
		assertEquals(2, onTheBracket.size());
		first = onTheBracket.get(0);
		second = onTheBracket.get(1);
		assertEquals('6', first.getParentLinkage().getParentPositionsSingle());
		assertEquals('?', second.getParentLinkage().getParentPositionsSingle());
	}

	/** The residue's own rules offer 6 to the second Gal. */
	@Test
	public void theBracketStillOffersSix() {
		Residue bracket = structure.getBracket();
		assertTrue(new String(bracket.availableLinkagePositions()).indexOf('6') >= 0);
		assertTrue(bracket.acceptsPosition('6', second));
	}

	/** Set to 6, both are at 6, and it is written and read back so. */
	@Test
	public void bothAtSixAreWrittenAndReadBack() throws Exception {
		second.getParentLinkage().setLinkagePositions(new char[] { '6' });

		String gws = structure.toString();
		assertTrue(gws, gws.contains("}(--6?1D-Gal,p)--6?1D-Gal,p"));
		Glycan reopened = Glycan.fromString(gws);
		assertEquals(gws, reopened.toString());

		String wurcs = new WURCS2Parser().toWURCS(structure);
		assertEquals(wurcs, new WURCS2Parser().toWURCS(reopened));
	}

	/** The canvas's position list offers 6 to the second Gal; an ordinary parent still does not. */
	@Test
	public void theCanvasOffersSix() throws Exception {
		org.junit.Assume.assumeFalse("no display", java.awt.GraphicsEnvironment.isHeadless());
		workspace.getStructures().setStructures(Collections.singletonList(structure));
		GlycanCanvas canvas = new GlycanCanvas(null, workspace,
				new ThemeManager(null, UncertainTerminalResiduesShareAPositionTest.class), false);

		assertTrue(positionsOffered(canvas, second, structure.getBracket()).contains("6"));

		// a Man whose 6 is taken by one child does not offer it to another
		Residue man = null;
		for (Residue residue : structure.getAllResidues())
			if ("Man".equals(residue.getTypeName()) && residue.getParentLinkage() != null
					&& residue.getParentLinkage().getParentPositionsSingle() == '4')
				man = residue;
		Residue onThree = null;
		for (Linkage link : man.getChildrenLinkages())
			if (link.getParentPositionsSingle() == '3') onThree = link.getChildResidue();
		assertFalse(positionsOffered(canvas, onThree, man).contains("6"));
	}

	/** The positions the canvas lists for the selected residue, as its toolbar does. */
	private static List<String> positionsOffered(GlycanCanvas canvas, Residue selected, Residue parent)
			throws Exception {
		// selecting through setSelection scrolls to the residue, which a canvas in no window cannot
		Field current = GlycanCanvas.class.getDeclaredField("current_residue");
		current.setAccessible(true);
		current.set(canvas, selected);
		Method create = GlycanCanvas.class.getDeclaredMethod("createPositions", Residue.class);
		create.setAccessible(true);
		ListModel<?> model = (ListModel<?>) create.invoke(canvas, parent);
		List<String> offered = new ArrayList<String>();
		for (int i = 0; i < model.getSize(); i++)
			offered.add(String.valueOf(model.getElementAt(i)));
		return offered;
	}
}
