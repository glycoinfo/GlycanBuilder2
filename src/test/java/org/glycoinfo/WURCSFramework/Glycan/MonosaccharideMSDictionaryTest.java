package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.LinkageTypeOptimizer;
import org.glycoinfo.application.glycanbuilder.dataset.MonosaccharideMSDictionary;
import org.glycoinfo.application.glycanbuilder.util.exchange.exporter.GlycanToWURCSGraph;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Reading a WURCS monosaccharide description back to the residue it names, using this builder's own
 * dictionary. The names in the old path came from a converter that does not cover everything this
 * builder can draw, and a residue it did not recognise took the whole structure down with it.
 */
public class MonosaccharideMSDictionaryTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	@Test
	public void namesAResidueFromItsSkeletonAlone() {
		assertResidue("Glc", "a2122h-1x_1-5");
		assertResidue("Gal", "a2112h-1b_1-5");
		assertResidue("Man", "a1122h-1a_1-5");
	}

	/** The substituents a residue owns are part of what names it. */
	@Test
	public void namesAResidueTogetherWithItsOwnSubstituents() {
		assertResidue("GlcNAc", "a2122h-1b_1-5_2*NCC/3=O");
		assertResidue("NeuAc", "Aad21122h-2a_2-6_5*NCC/3=O");
	}

	/** Whatever is left over is reported separately, rather than confusing the name. */
	@Test
	public void separatesAttachedGroupsFromTheResidue() {
		MonosaccharideMSDictionary.Match match =
				MonosaccharideMSDictionary.match("a2122h-1x_1-5_2*NCC/3=O_6*OSO/3=O/3=O");

		assertNotNull(match);
		assertEquals("GlcNAc", match.getResidueType().getName());
		assertEquals(1, match.getAttachedGroups().size());
		assertEquals("6*OSO/3=O/3=O", match.getAttachedGroups().get(0));
	}

	/**
	 * These three used to bring the import down, each for its own reason in the converter that named
	 * residues: a substituent it did not know, a skeleton it could not read, and a bridge across the
	 * anomeric position, which makes a second ring it refuses to model.
	 */
	@Test
	public void readsWhatTheOldNamingCouldNot() {
		MonosaccharideMSDictionary.Match ethyl = MonosaccharideMSDictionary.match("a2122h-1x_1-5_3*OCC");
		assertNotNull(ethyl);
		assertEquals("Glc", ethyl.getResidueType().getName());
		assertEquals("3*OCC", ethyl.getAttachedGroups().get(0));

		assertResidue("ddHex", "adxxxm-1x_1-5");

		MonosaccharideMSDictionary.Match anhydro = MonosaccharideMSDictionary.match("a2122h-1x_1-5_1-6");
		assertNotNull(anhydro);
		assertEquals("Glc", anhydro.getResidueType().getName());
		assertEquals("1-6", anhydro.getAttachedGroups().get(0));
	}

	/** A description this builder writes no residue for is reported as such, not guessed at. */
	@Test
	public void reportsAnUnknownDescriptionRatherThanGuessing() {
		assertNull(MonosaccharideMSDictionary.match("zzzzzz-1x_1-5"));
	}

	/**
	 * The index is built from what the exporter writes, so every residue that can be written has to
	 * be readable back as itself. This is the property that keeps the two directions in step.
	 */
	@Test
	public void everyResidueThatCanBeWrittenIsReadBackAsItself() {
		List<String> broken = new ArrayList<String>();
		int checked = 0;

		for (ResidueType type : ResidueDictionary.allResidues()) {
			if (!type.isSaccharide() || type.isBridge()) continue;

			String ms = describeAlone(type);
			if (ms == null) continue;

			checked++;
			MonosaccharideMSDictionary.Match match = MonosaccharideMSDictionary.match(ms);
			if (match == null || !match.getResidueType().getName().equals(type.getName()))
				broken.add(type.getName() + " (" + ms + ") -> "
						+ (match == null ? "null" : match.getResidueType().getName()));
		}

		assertTrue("residues not read back as themselves: " + broken, broken.isEmpty());
		assertTrue("expected the index to cover most residues, checked " + checked, checked > 70);
	}

	private void assertResidue(String _expectedName, String _ms) {
		MonosaccharideMSDictionary.Match match = MonosaccharideMSDictionary.match(_ms);
		assertNotNull("no residue matched " + _ms, match);
		assertEquals(_ms, _expectedName, match.getResidueType().getName());
	}

	/** How the exporter writes this residue standing on its own, or null when it cannot write it. */
	private String describeAlone(ResidueType _type) {
		try {
			Residue root = ResidueDictionary.createReducingEnd("freeEnd");
			root.addChild(ResidueDictionary.newResidue(_type.getName()));

			Glycan glycan = new Glycan(root, false, new MassOptions());
			new LinkageTypeOptimizer().start(glycan);

			GlycanToWURCSGraph toGraph = new GlycanToWURCSGraph();
			toGraph.start(glycan);
			String wurcs = new WURCSFactory(toGraph.getGraph()).getWURCS();

			return wurcs.substring(wurcs.indexOf('[') + 1, wurcs.indexOf(']'));
		} catch (Throwable cannotBeWritten) {
			return null;
		}
	}
}
