package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.dataset.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.CoreType;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That GlycoCT gives a substituent's own side of its bond as position 1 (#45).
 *
 * <p>A substituent attaches through its one position, so its side of the bond is 1 by definition -
 * and the validators insist: "for this substituent sulfate linkage pos must be 1". The exporter
 * wrote whatever the bond recorded, which for the GAG templates was unknown, so gagheparin's
 * GlycoCT carried lines like {@code 1:1d(2+-1)2n} and GlyTouCan's graphic search rejected the
 * whole structure.</p>
 */
public class GlycoCTSubstituentPositionTest {

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void newWorkspace() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * The GAG templates write no "-1" attachments - the case GlyTouCan rejected.
	 *
	 * <p>Every core the dictionary offers is held, not only heparin: a template that exports
	 * invalid GlycoCT fails silently for whoever picks it from the menu.</p>
	 */
	@Test
	public void everyCoreTemplateWritesValidSubstituentAttachments() throws Exception {
		List<String> wrong = new ArrayList<String>();
		int cores = 0;

		for (CoreType core : CoreDictionary.getCores()) {
			Glycan structure = Glycan.fromString(core.getStructure());
			if (structure == null) continue;
			cores++;

			String glycoct = glycoctOf(structure);
			for (String line : glycoct.split("\n")) {
				if (line.contains("+-1)") && line.strip().endsWith("n")) {
					wrong.add(core.getName() + ": " + line.strip());
				}
			}
		}

		assertEquals("substituents attached at -1: " + wrong, List.of(), wrong);
		assertTrue("no cores were tested at all", cores > 10);
	}

	/** gagheparin in particular - the structure the report came in on - names its positions. */
	@Test
	public void gagheparinAttachesItsSulfatesAtOne() throws Exception {
		CoreType heparin = CoreDictionary.getCoreType("gagheparin");
		String glycoct = glycoctOf(Glycan.fromString(heparin.getStructure()));

		assertTrue("no n-sulfate in the export at all", glycoct.contains("n-sulfate"));
		assertTrue("still attaching at -1:\n" + glycoct, !glycoct.contains("+-1)"));
	}

	/** A substituent whose position was written down is left exactly as written. */
	@Test
	public void aKnownPositionIsLeftAsItIs() throws Exception {
		String glycoct = glycoctOf(Glycan.fromString("freeEnd--?b1D-Glc,p--6b1S$MONO,Und,0,0,freeEnd"));

		assertTrue(glycoct, glycoct.contains("(6+1)2n"));
	}

	/**
	 * And an unknown position between two sugars stays unknown.
	 *
	 * <p>The repair is for substituents only: a sugar's attachment really can be unknown, and
	 * writing 1 there would be inventing data.</p>
	 */
	@Test
	public void anUnknownPositionBetweenSugarsStaysUnknown() throws Exception {
		String glycoct = glycoctOf(
				Glycan.fromString("freeEnd--?b1D-Glc,p--??1D-Gal,p$MONO,Und,0,0,freeEnd"));

		assertTrue("a sugar-sugar unknown was invented away:\n" + glycoct,
				glycoct.contains("(-1+1)"));
	}

	private static String glycoctOf(Glycan structure) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		document.addStructure(structure);

		return document.toString("glycoct_condensed");
	}
}
