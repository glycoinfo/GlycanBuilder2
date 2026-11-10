package org.eurocarbdb.application.glycanbuilder.renderutil;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyleDictionary;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;

/**
 * The footnote under an SNFG drawing: which residues are drawn as a white symbol carrying a letter,
 * which letter each gets, and the lines that name them.
 *
 * <p>SNFG rule/note 11: a residue with no symbol of its own "may be indicated using a single
 * non-italicized letter (A..Z) within an SNFG white symbol", named in a footnote. Three things went
 * wrong because this was worked out in several places that did not agree:
 *
 * <ul>
 * <li><b>#262</b> - the letter was a number, 1, 2, 3.
 * <li><b>#250</b> - the footnote was drawn from the bottom of the structure, and so was the mass. The
 * space reserved beneath the structure was counted per residue whose superclass is "Assigned", which
 * is not the set that gets a footnote line: a residue with no symbol whose superclass is "Unknown" or
 * "Hexuronic acid" got a line and no room for it, and the line was drawn over the mass.
 * <li><b>#251</b> - not a disagreement over the number, as first thought. The symbol was given its
 * index, and then the ring and configuration markers SNFG draws inside a symbol ("f" for a furanose)
 * replaced it. Fixed in the residue renderer; noted here because it is the same rule: a lettered
 * residue is identified by its letter.
 * </ul>
 *
 * <p>Built once per structure, from one walk, and read by everything that needs it - the residues
 * for their letter, the legend for its lines, the layout for its height. Nothing computes any of it a
 * second time.
 */
public final class SNFGFootnote {

	/** The footnote's type size, unscaled, as it has always been drawn. */
	public static final int FONT_SIZE = 10;

	private final Map<String, Integer> ordinalByLegend = new LinkedHashMap<String, Integer>();
	private final boolean saysNoGlycosidicLinkages;
	private final String fontFace;

	private SNFGFootnote(Glycan structure, ResidueStyleDictionary styles, GraphicOptions options) {
		this.fontFace = options.MASS_TEXT_FONT_FACE;

		boolean noLinkages = false;
		if (structure != null && GraphicOptions.NOTATION_SNFG.equals(options.NOTATION)) {
			for (Residue residue : structure.getAllResidues()) {
				if (residue.isCompositionMarker()) {
					noLinkages = true;
					continue;
				}
				if (!isLettered(residue, styles, options)) continue;

				String legend = residue.getLegend();
				if (!this.ordinalByLegend.containsKey(legend))
					this.ordinalByLegend.put(legend, this.ordinalByLegend.size() + 1);
			}
		}
		this.saysNoGlycosidicLinkages = noLinkages;
	}

	/** The footnote of this structure as it would be drawn under these options. */
	public static SNFGFootnote of(Glycan structure, ResidueStyleDictionary styles, GraphicOptions options) {
		return new SNFGFootnote(structure, styles, options);
	}

	/**
	 * Whether this residue is drawn as a white symbol with a letter.
	 *
	 * <p>The one predicate. A saccharide, not a bridge, not the marker a composition carries, with no
	 * symbol of its own in this notation - and only in SNFG, since the other notations have no such
	 * rule.
	 */
	public static boolean isLettered(Residue residue, ResidueStyleDictionary styles, GraphicOptions options) {
		if (residue == null || residue.getType() == null) return false;
		if (!GraphicOptions.NOTATION_SNFG.equals(options.NOTATION)) return false;
		if (!residue.isSaccharide()) return false;
		if ("Bridge".equals(residue.getType().getSuperclass())) return false;
		if (residue.isCompositionMarker()) return false;

		return !styles.containsResidue(residue);
	}

	/**
	 * The ordinal of this residue's letter, 1 for A, or 0 when it has none.
	 *
	 * <p>Residues with the same footnote name share a letter: two β-D-Altp are one footnote line, and a
	 * β-D-Altf beside them is another.
	 */
	public int ordinalOf(Residue residue) {
		if (residue == null) return 0;
		Integer ordinal = this.ordinalByLegend.get(residue.getLegend());
		return (ordinal == null) ? 0 : ordinal.intValue();
	}

	/**
	 * The footnote's lines, top to bottom: the composition marker's line first when there is one, as it
	 * always has been, then one line per letter - "A=β-D-Altp".
	 */
	public List<String> lines() {
		List<String> lines = new ArrayList<String>();
		if (this.saysNoGlycosidicLinkages) lines.add(Residue.NO_GLYCOSIDIC_LINKAGES);
		for (Map.Entry<String, Integer> entry : this.ordinalByLegend.entrySet())
			lines.add(letter(entry.getValue()) + "=" + entry.getKey());

		return lines;
	}

	public boolean isEmpty() {
		return lines().isEmpty();
	}

	/** The font the footnote is drawn in. */
	public Font font() {
		return new Font(this.fontFace, Font.PLAIN, FONT_SIZE);
	}

	/**
	 * The distance from one line's baseline to the next.
	 *
	 * <p>Measured here and nowhere else, so that the layout reserves exactly the height the legend is
	 * drawn in. Measuring it from whatever graphics context happens to be painting - an SVG one, a
	 * canvas - is how the two came apart.
	 */
	public int lineHeight() {
		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = scratch.createGraphics();
		try {
			FontMetrics metrics = g2d.getFontMetrics(font());
			return metrics.getHeight();
		} finally {
			g2d.dispose();
		}
	}

	/** How far below the structure the footnote reaches. Zero when there is nothing to say. */
	public int height() {
		List<String> lines = lines();
		if (lines.isEmpty()) return 0;

		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = scratch.createGraphics();
		try {
			// the last baseline, and the descent below it
			return lines.size() * lineHeight() + g2d.getFontMetrics(font()).getDescent();
		} finally {
			g2d.dispose();
		}
	}

	/** The width of the widest line. */
	public int width() {
		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = scratch.createGraphics();
		try {
			FontMetrics metrics = g2d.getFontMetrics(font());
			int widest = 0;
			for (String line : lines())
				widest = Math.max(widest, metrics.stringWidth(line));
			return widest;
		} finally {
			g2d.dispose();
		}
	}

	/**
	 * The letter for an ordinal: 1 is A, 26 is Z.
	 *
	 * <p>SNFG asks for a single letter and stops at Z. A structure with more than 26 distinct residues
	 * without a symbol has outrun the rule, and continuing as spreadsheet columns do - AA, AB - keeps
	 * every residue identifiable rather than failing or reusing a letter.
	 */
	public static String letter(int ordinal) {
		if (ordinal <= 0) return "";

		StringBuilder letters = new StringBuilder();
		int n = ordinal;
		while (n > 0) {
			n--;
			letters.insert(0, (char) ('A' + (n % 26)));
			n /= 26;
		}
		return letters.toString();
	}
}
