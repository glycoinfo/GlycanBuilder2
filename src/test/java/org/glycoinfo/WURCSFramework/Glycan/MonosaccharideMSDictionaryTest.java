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
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;
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

	/**
	 * A residue's stereo is one recorded string, written for the D form, and the other configurations
	 * follow from it: L swaps every digit, because an enantiomer inverts every centre, and an unknown
	 * configuration maps 1 to 3 and 2 to 4, the relative form that says how the carbons stand in
	 * relation to each other without saying which way round the molecule is.
	 *
	 * <p>D-glucose is 2122, so L-glucose is 1211 and a glucose of unspecified configuration is 4344.
	 */
	@Test
	public void theOtherConfigurationsFollowFromTheRecordedOne() {
		NativeMonosaccharideDictionary.Entry glc = NativeMonosaccharideDictionary.forResidueName("Glc");

		assertEquals("2122", glc.getStereo('D'));
		assertEquals("1211", glc.getStereo('L'));
		assertEquals("4344", glc.getStereo('?'));
	}

	/**
	 * A name that fixes both of its stereo blocks has nothing left for a configuration to change. The
	 * nonulosonates are named for two of them - Neu is D-glycero-D-galacto - and so are the heptoses.
	 */
	@Test
	public void aNameThatFixesBothBlocksIgnoresTheConfiguration() {
		NativeMonosaccharideDictionary.Entry neu = NativeMonosaccharideDictionary.forResidueName("Neu");

		assertEquals("21122", neu.getStereo('D'));
		assertEquals("21122", neu.getStereo('L'));
		assertEquals("21122", neu.getStereo('?'));
	}

	/**
	 * The index holds each residue in the configuration residue_types makes its default, and SNFG
	 * Note 4 states what those defaults are: L for Ara, and D for the other three pentoses. Lyx was
	 * declared L and so answered to a221h, its mirror image, while the symbol drawn for it is the one
	 * SNFG defines for D-Lyx. That 112 is D-lyxo is confirmed from the other direction by Tag, which
	 * is D-lyxo-hex-2-ulose and writes ha112h.
	 */
	@Test
	public void thePentosesAreIndexedInTheConfigurationSnfgMakesDefault() {
		assertResidue("Ara", "a211h-1x_1-5");   // L
		assertResidue("Xyl", "a212h-1x_1-5");   // D
		assertResidue("Rib", "a222h-1x_1-5");   // D
		assertResidue("Lyx", "a112h-1x_1-5");   // D
	}

	/**
	 * A residue is written in more than one form, and all of them have to be recognised. The index
	 * used to hold only the first - a residue with a ring and a determined anomeric carbon - and the
	 * others went to a name lookup elsewhere. They are not rare: in a corpus of two hundred structures
	 * one residue in five arrived in one of these forms.
	 */
	@Test
	public void everyFormOfAResidueIsRecognised() {
		assertResidue("Glc", "a2122h-1x_1-5");   // ring
		assertResidue("Glc", "u2122h");          // anomeric carbon undetermined
		assertResidue("Glc", "h2122h");          // alditol
		assertResidue("Glc", "o2122h");          // open chain
	}

	/**
	 * An MS with a ring reads {@code a2122h-1x_1-5_2*NCC/3=O}, and the {@code 1-5} is the one segment
	 * that is not a group. A residue written without a ring has no such segment, so its groups begin
	 * one place earlier - and reading them from the wrong place lost the substituents of every residue
	 * written that way, which turned a GlcNAc into a Glc.
	 */
	@Test
	public void theGroupsOfAResidueWithNoRingAreStillFound() {
		assertResidue("GlcNAc", "u2122h_2*NCC/3=O");
		assertResidue("NeuAc", "AUd21122h_5*NCC/3=O");

		MonosaccharideMSDictionary.Match sulfated =
				MonosaccharideMSDictionary.match("u2122h_2*NCC/3=O_6*OSO/3=O/3=O");
		assertNotNull(sulfated);
		assertEquals("GlcNAc", sulfated.getResidueType().getName());
		assertEquals("6*OSO/3=O/3=O", sulfated.getAttachedGroups().get(0));
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
