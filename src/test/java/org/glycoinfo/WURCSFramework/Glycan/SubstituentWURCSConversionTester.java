package org.glycoinfo.WURCSFramework.Glycan;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.LinkageTypeOptimizer;
import org.glycoinfo.application.glycanbuilder.util.exchange.exporter.GlycanToWURCSGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Attaches every Substituent residue type (see conf/residue_types) to a Glc
 * residue and reports whether the resulting structure converts to WURCS2.
 * Tested both with a single bond at position 6, and with a second bridging
 * bond at position 4. The Residue model itself does not reject a second bond
 * on a non-Bridge substituent (that guard lives at the GlycoCT/WURCS importer
 * call sites instead, since those are the only reachable paths for such data);
 * this test builds the structure directly via the raw API to document which
 * substituents' WURCS notation is/isn't reliable if such a bond ever reaches
 * export.
 */
public class SubstituentWURCSConversionTester {

	// Py, (S)Py, (R)Py and Anhydro are intentionally excluded: they are Bridge-only
	// now (see cross_linked_substituent_types) and no longer resolvable via the
	// plain ResidueDictionary used by this single/synthetic-double-bond test.
	private static final String[] SUBSTITUENTS = {
		"Me", "Ac", "Et", "NAc", "P", "S", "Pyr", "PMe", "PCho", "PPEtn", "PEtn", "N",
		"Gc", "NGc", "NS", "NFo", "NAm", "NMe", "NSuc", "Fo", "Am", "Suc", "NDiMe",
		"PyrP", "Tri-P", "F", "I", "Br", "Cl", "SH",
		"(S)Lac", "(R)Lac", "(X)Lac", "?", "Ino"
	};

	public static void main(String[] args) throws Exception {
		BuilderWorkspace ws = new BuilderWorkspace(new GlycanRendererAWT());
		ws.initData();

		System.out.println("=== single bond (position 6) ===");
		System.out.println(runAll(false));

		System.out.println("=== two bonds (position 6 + bridging second bond at position 4) ===");
		System.out.println(runAll(true));
	}

	private static String runAll(boolean withSecondBond) {
		StringBuilder report = new StringBuilder();
		report.append(String.format("%-10s %-8s %s%n", "Substituent", "Result", "WURCS / Error"));

		for (String name : SUBSTITUENTS) {
			try {
				Residue glc = ResidueDictionary.newResidue("Glc");
				glc.setAnomericState('a');

				Residue subst = ResidueDictionary.newResidue(name);

				if (withSecondBond) {
					List<Bond> bonds = new ArrayList<>();
					bonds.add(new Bond('4', '2'));                          // bridging second bond
					bonds.add(new Bond('6', subst.getAnomericCarbon()));    // main glycosidic bond (must be last)
					glc.addChild(subst, bonds);
				} else {
					glc.addChild(subst, '6');
				}

				Glycan glycan = new Glycan(glc, true, new MassOptions());

				LinkageTypeOptimizer linkOpt = new LinkageTypeOptimizer();
				linkOpt.start(glycan);

				GlycanToWURCSGraph glycan2graph = new GlycanToWURCSGraph();
				glycan2graph.start(glycan);

				WURCSFactory wf = new WURCSFactory(glycan2graph.getGraph());
				String wurcs = wf.getWURCS();

				report.append(String.format("%-10s %-8s %s%n", name, "OK", wurcs));
			} catch (Exception e) {
				report.append(String.format("%-10s %-8s %s: %s%n", name, "FAIL", e.getClass().getSimpleName(), e.getMessage()));
			}
		}

		return report.toString();
	}
}
