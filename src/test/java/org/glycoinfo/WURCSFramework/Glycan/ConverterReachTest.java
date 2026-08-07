package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;
import org.glycoinfo.application.glycanbuilder.dataset.MonosaccharideMSDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * How far a structure can be read without asking outside this project.
 *
 * <p>A monosaccharide is normally recognised from this builder's own index. What that index does not
 * hold falls through to a naming converter in glycanformatconverter, and that fallback used to carry
 * one residue in five - it held only the ring form of each residue, so an undetermined anomeric
 * carbon, an alditol or an open chain all missed. Over this corpus it now carries 7 residues of 127.
 *
 * <p>Keeping it there is a decision rather than an accident, so it is held by this test. The symptom
 * of losing it is quiet: a residue gets named by an outside table instead of ours, and nothing looks
 * wrong until that table meets something it cannot name. Both numbers are asserted, and so are the
 * descriptions that legitimately miss - a change in which ones miss should be as visible as a change
 * in how many.
 */
public class ConverterReachTest {

	/**
	 * The descriptions the index does not hold. Four are residues this builder has no entry for at
	 * all, rather than forms it failed to recognise. The fifth, a2221m, is one it does hold - 6dTal
	 * drawn as an L - and it misses because the index records each residue in its own configuration
	 * only. Closing that means carrying the configuration in the index rather than taking it from the
	 * residue type, which is tracked separately.
	 */
	private static final List<String> NOT_OURS = Arrays.asList(
			"a21EEA-1a_1-5_2*OSO/3=O/3=O",   // a hexuronate with an unsaturation
			"a21d2h-1a_1-5",                 // a 4-deoxy hexose
			"a2221m-1x_1-5",                 // 6dTal drawn as an L
			"hxh",                           // glycerol
			"o2h");                          // glyceraldehyde

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	@Test
	public void theNamingFallbackCarriesNoMoreThanItDoesToday() throws Exception {
		int residues = 0;
		int unmatched = 0;
		TreeSet<String> descriptions = new TreeSet<String>();

		for (String wurcs : corpus()) {
			for (GRES gres : new WURCSFactory(wurcs).getSequence().getGRESs()) {
				residues++;
				String ms = gres.getMS().getString();
				if (MonosaccharideMSDictionary.match(ms) != null) continue;

				unmatched++;
				descriptions.add(ms);
			}
		}

		assertEquals("residues read from the corpus", 127, residues);
		assertEquals("residues the index could not name, which go to the converter", 7, unmatched);
		assertEquals("which descriptions they were", NOT_OURS, new ArrayList<String>(descriptions));
	}

	/** A guard on the corpus itself, so a truncated resource cannot make the counts above pass. */
	@Test
	public void theCorpusIsWhatItWas() throws Exception {
		assertEquals(17, corpus().size());
	}

	private List<String> corpus() throws Exception {
		List<String> wurcs = new ArrayList<String>();
		InputStream resource = getClass().getResourceAsStream("/converter-reach-corpus.txt");
		assertTrue("corpus resource is missing", resource != null);

		BufferedReader reader = new BufferedReader(new InputStreamReader(resource, "UTF-8"));
		for (String line; (line = reader.readLine()) != null; ) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#")) continue;

			wurcs.add(line);
		}
		reader.close();

		return wurcs;
	}
}
