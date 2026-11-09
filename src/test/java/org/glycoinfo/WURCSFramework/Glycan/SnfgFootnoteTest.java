package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.DefaultPaintable;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.SNFGFootnote;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.junit.Test;

/**
 * The SNFG footnote: letters rather than numbers, a letter that stays on its symbol, and room under
 * the structure for the lines that name them (#262, #251, #250).
 *
 * <p>The structure is G63532WE, the one all three were reported against (#247), read from GlyCosmos's
 * own WURCS. It has three residues with no symbol of their own: a β-D-Altp, a β-L-thrHexpA4(E)en and a
 * β-D-Altf - the last a furanose, which is what #251 needed.
 *
 * <p>Asserted on what the renderer actually draws. Batik writes text as outlines, so an exported SVG
 * holds no strings to look for; instead the real renderer paints into a graphics context that records
 * every {@code drawString} and where it went.
 */
public class SnfgFootnoteTest {

	/** G63532WE, as GlyCosmos serves it. */
	private static final String G63532WE = "WURCS=2.0/14,16,15/[a2122h-1a_1-5_2*N]"
			+ "[a2122h-1b_1-5_2*N_4*OPO/3O/3=O][Aad1122h-2a_2-6_4*OPO/3O/3=O][a11221h-1a_1-5][a2112h-1b_1-5]"
			+ "[ha1222h-2b_2-6][a2122m-1b_1-5_2*N][a2112A-1b_1-5][Aad21122h-2a_2-6_5*N][a21EEA-1b_1-5]"
			+ "[a2122h-1a_1-?_2*N][a2122h-1b_1-5][ha1222h-2b_2-5][a2122h-1a_1-5]"
			+ "/1-2-3-4-4-4-5-6-7-8-9-10-11-12-13-14/a6-b1_b6-c2_c5-d1_d3-e1_d4-n1_d6-p1_e2-f1_f3-g1_f7-m1"
			+ "_g3-h2_h4-i1_i3-j1_j3-k2_k4-l1_n6-o2";

	private static final List<String> FOOTNOTE = Arrays.asList(
			"A=β-D-Altp", "B=β-L-thrHexpA4(E)en", "C=β-D-Altf");

	/** #262: the ordinal is a letter, and keeps going past Z rather than failing or repeating. */
	@Test
	public void ordinalsAreLetters() {
		assertEquals("A", SNFGFootnote.letter(1));
		assertEquals("B", SNFGFootnote.letter(2));
		assertEquals("Z", SNFGFootnote.letter(26));
		assertEquals("AA", SNFGFootnote.letter(27));
		assertEquals("AZ", SNFGFootnote.letter(52));
		assertEquals("BA", SNFGFootnote.letter(53));
		assertEquals("", SNFGFootnote.letter(0));
	}

	/** #262 on the page: the footnote names its residues A, B, C, and those letters are drawn on them. */
	@Test
	public void theFootnoteAndTheSymbolsUseLetters() throws Exception {
		Drawn drawn = paint(G63532WE, true);

		assertEquals("the footnote", FOOTNOTE, drawn.footnote());
		assertFalse("a footnote line still starts with a number: " + drawn.footnote(),
				drawn.footnoteStartsWithNumber());
		// Numbers are still drawn - "3" is a linkage position - so what is checked is that the letters
		// are, each standing alone as a symbol's text.
		for (String letter : new String[] { "A", "B", "C" })
			assertTrue(letter + " was not drawn on any symbol: " + drawn.strings(), drawn.strings().contains(letter));
	}

	/**
	 * #251: the furanose keeps its letter.
	 *
	 * <p>SNFG marks a furanose with an "f" inside its symbol, and that marker replaced the index - so the
	 * footnote named a residue 3 that no symbol carried. "C" must be drawn, and no bare "f".
	 */
	@Test
	public void aLetteredFuranoseKeepsItsLetter() throws Exception {
		Drawn drawn = paint(G63532WE, true);

		assertTrue("the furanose's letter was not drawn: " + drawn.strings(), drawn.strings().contains("C"));
		assertFalse("the furanose marker replaced the letter: " + drawn.strings(), drawn.strings().contains("f"));
	}

	/**
	 * And a furanose that <em>has</em> a symbol still gets its "f".
	 *
	 * <p>The half that says #251's fix did not simply stop drawing the marker. A galactofuranose is a
	 * yellow circle with an f in it, and must stay one.
	 */
	@Test
	public void aFuranoseWithASymbolStillGetsItsMarker() throws Exception {
		Drawn drawn = paintGws("freeEnd--?b1D-GlcNAc,p--3b1D-Gal,f$MONO,Und,0,0,freeEnd", true);

		assertTrue("the galactofuranose lost its f: " + drawn.strings(), drawn.strings().contains("f"));
		assertTrue("and has no footnote to give it a letter instead: " + drawn.footnote(),
				drawn.footnote().isEmpty());
	}

	/**
	 * #250: the footnote and the mass do not overlap.
	 *
	 * <p>Both were drawn from the structure's bottom edge. Checked as boxes: the bottom of the last
	 * footnote line must be above the top of the mass text.
	 */
	@Test
	public void theFootnoteAndTheMassDoNotOverlap() throws Exception {
		Drawn drawn = paint(G63532WE, true);

		Drawn.At lastLine = drawn.at(FOOTNOTE.get(FOOTNOTE.size() - 1));
		Drawn.At mass = drawn.atStartingWith("m/z");
		assertNotNull("the last footnote line was not drawn", lastLine);
		assertNotNull("the mass was not drawn", mass);

		assertTrue("the footnote runs into the mass: last line ends at " + lastLine.bottom()
				+ ", mass starts at " + mass.top(), lastLine.bottom() <= mass.top());
	}

	/** And without a mass shown, the footnote is still there and still in order. */
	@Test
	public void theFootnoteIsDrawnWithoutAMassToo() throws Exception {
		assertEquals(FOOTNOTE, paint(G63532WE, false).footnote());
	}

	/** Outside SNFG there is no such rule, so no letters and no footnote. */
	@Test
	public void otherNotationsHaveNoFootnote() throws Exception {
		Drawn drawn = paint(G63532WE, true, GraphicOptions.NOTATION_CFG);

		assertTrue("a footnote was drawn in CFG: " + drawn.footnote(), drawn.footnote().isEmpty());
	}

	// ------------------------------------------------------------------------------------------------

	private static Drawn paint(String wurcs, boolean showMass) throws Exception {
		return paint(wurcs, showMass, GraphicOptions.NOTATION_SNFG);
	}

	private static Drawn paint(String wurcs, boolean showMass, String notation) throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		workspace.setNotation(notation);
		GlycanDocument document = workspace.getStructures();
		document.importFromString(wurcs, "wurcs2");
		return paintIn(workspace, document.getStructures(), showMass);
	}

	private static Drawn paintGws(String gws, boolean showMass) throws Exception {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		workspace.setNotation(GraphicOptions.NOTATION_SNFG);
		GlycanDocument document = workspace.getStructures();
		document.importFromString(gws, "gws");
		return paintIn(workspace, document.getStructures(), showMass);
	}

	/** Lay out, then paint - the order every export uses - into a context that records the text. */
	private static Drawn paintIn(BuilderWorkspace workspace, List<Glycan> structures, boolean showMass) {
		GlycanRendererAWT renderer = (GlycanRendererAWT) workspace.getGlycanRenderer();
		PositionManager positions = new PositionManager();
		BBoxManager boxes = new BBoxManager();
		renderer.computeBoundingBoxes(structures, showMass, true, positions, boxes);

		Recording g2d = new Recording();
		for (Glycan structure : structures)
			renderer.paint(new DefaultPaintable(g2d), structure, null, null, showMass, true, positions, boxes);

		return new Drawn(g2d.calls);
	}

	/** A Batik graphics context that remembers every string drawn, where, and in what font. */
	private static final class Recording extends SVGGraphics2D {
		final List<Drawn.At> calls = new ArrayList<Drawn.At>();

		Recording() {
			super(GenericDOMImplementation.getDOMImplementation().createDocument(null, "svg", null));
		}

		@Override
		public void drawString(String s, float x, float y) {
			this.calls.add(new Drawn.At(s, x, y, getFontMetrics(getFont())));
			super.drawString(s, x, y);
		}

		@Override
		public void drawString(String s, int x, int y) {
			// recorded here, and not again when Batik passes it on to the float version
			drawString(s, (float) x, (float) y);
		}
	}

	/** What was drawn. */
	private static final class Drawn {
		private final List<At> calls;

		Drawn(List<At> calls) {
			this.calls = calls;
		}

		List<String> strings() {
			List<String> strings = new ArrayList<String>();
			for (At at : this.calls) strings.add(at.text);
			return strings;
		}

		/** The footnote's lines, in the order drawn: the lines naming a letter, and the marker's line. */
		List<String> footnote() {
			List<String> lines = new ArrayList<String>();
			for (At at : this.calls)
				if (at.text.matches("[A-Z]+=.*") || at.text.matches("[0-9]+=.*")
						|| at.text.equals("no glycosidic linkages"))
					lines.add(at.text);
			return lines;
		}

		boolean footnoteStartsWithNumber() {
			for (String line : footnote())
				if (line.matches("[0-9]+=.*")) return true;
			return false;
		}

		At at(String text) {
			for (At at : this.calls) if (at.text.equals(text)) return at;
			return null;
		}

		At atStartingWith(String prefix) {
			for (At at : this.calls) if (at.text.startsWith(prefix)) return at;
			return null;
		}

		static final class At {
			final String text;
			final float x;
			final float baseline;
			final int ascent;
			final int descent;

			At(String text, float x, float baseline, FontMetrics metrics) {
				this.text = text;
				this.x = x;
				this.baseline = baseline;
				this.ascent = metrics.getAscent();
				this.descent = metrics.getDescent();
			}

			float top() {
				return this.baseline - this.ascent;
			}

			float bottom() {
				return this.baseline + this.descent;
			}
		}
	}
}
