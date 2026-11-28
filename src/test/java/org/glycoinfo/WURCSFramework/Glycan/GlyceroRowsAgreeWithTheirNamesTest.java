package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A residue named by two configurational prefixes - D-gro-D-altHep, L-gro-D-manNon - is described as
 * the sugar its name says (#273).
 *
 * <p>Three rows were not. D-gro-D-altHep was described as D-glycero-D-<em>talo</em>-heptose, a
 * different sugar; D-gro-D-galOct and D-gro-D-talOct as D-glycero-<em>L</em>-galacto- and -talo-
 * octoses. The name is the one the WURCS reader gives each skeleton - a12222h is D-gro-D-altHep,
 * ha11122h D-gro-D-talOct - and the one the original tests used for D-glycero-D-talo, Ko's; the
 * descriptions were corrected to it (the maintainer, 2026-11-28).
 *
 * <p>The two octoses are oct-2-uloses, as their anomeric carbon of 2 says: D-glycero-D-galacto- and
 * D-glycero-D-talo-oct-2-ulose. Their rows also recorded 5 methylation and 3 acetylation sites; an
 * oct-2-ulopyranose has seven free hydroxyls, at 1, 2, 3, 4, 5, 7 and 8. And their positions began at
 * C2, the anomeric carbon, and left out C1: a ketose lists every carbon but its anomeric one, as Fru
 * (1, 3, 4, 5, 6) and Kdo (1, 4, 5, 6, 7, 8) do, so 1, 3, 4, 5, 6, 7 and 8.
 */
public class GlyceroRowsAgreeWithTheirNamesTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	private static final Pattern GLYCERO_NAME = Pattern.compile("^([DL])-gro-([DL])-([a-z]{3})(Hep|Oct|Non)$");

	/** How each configuration is spelled in the descriptions, including the abbreviated spellings they use. */
	private static final Map<String, List<String>> SPELLED = new HashMap<String, List<String>>();
	static {
		SPELLED.put("man", Arrays.asList("manno"));
		SPELLED.put("gal", Arrays.asList("galacto"));
		SPELLED.put("glc", Arrays.asList("gluco", "glco"));
		SPELLED.put("tal", Arrays.asList("talo"));
		SPELLED.put("alt", Arrays.asList("altro", "alto"));
		SPELLED.put("ido", Arrays.asList("ido"));
	}

	@Test
	public void eachIsDescribedAsTheSugarItsNameSays() {
		List<ResidueType> rows = new ArrayList<ResidueType>(ResidueDictionary.allResidues());
		rows.addAll(NonSymbolicResidueDictionary.getResidues());

		List<String> wrong = new ArrayList<String>();
		int checked = 0;
		for (ResidueType row : rows) {
			Matcher name = GLYCERO_NAME.matcher(row.getName());
			if (!name.matches()) continue;
			checked++;
			String prefix = name.group(1) + "-glycero-" + name.group(2) + "-";
			String description = row.getDescription();
			boolean agrees = description.startsWith(prefix);
			if (agrees) {
				String configuration = description.substring(prefix.length()).toLowerCase();
				agrees = false;
				for (String spelling : SPELLED.get(name.group(3)))
					if (configuration.startsWith(spelling)) agrees = true;
			}
			if (!agrees) wrong.add(row.getName() + ": " + description);
		}

		assertTrue("too few rows found: " + checked, checked >= 20);
		assertEquals(wrong.toString(), 0, wrong.size());
	}

	@Test
	public void theOctulosesHaveSevenSitesAndThePositionsOfAKetose() {
		for (String name : Arrays.asList("D-gro-D-galOct", "D-gro-D-talOct")) {
			ResidueType row = NonSymbolicResidueDictionary.findResidueType(name);
			assertEquals(name, '2', row.getAnomericCarbon());
			assertEquals(name, 7, row.getNoMethyls());
			assertEquals(name, 7, row.getNoAcetyls());
			assertEquals(name, "1345678", new String(row.getLinkagePositions()));
		}
	}
}
