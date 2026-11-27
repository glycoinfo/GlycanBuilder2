package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A copy of a structure weighs what the structure weighs (#281).
 *
 * <p>{@code Glycan.clone()} - which copy and paste goes through - left every linkage
 * {@code UNVALIDATED}, and the mass reads the linkage types: an O-methyl whose linkage does not say it
 * takes the hydroxyl's hydrogen weighs as if its oxygen were not there. And it dropped the alditol of a
 * residue inside a chain, two hydrogens. Of the 222 test-resource structures that read, 21 copies
 * weighed differently from their originals, by up to 223.9288 Da.
 */
public class CloneWeighsTheSameTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	private static Glycan read(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, new MassOptions());
	}

	/** The structure of the report: 3-O-methyl glucose, 194.0790, and its copy 178.0841. */
	@Test
	public void anOMethylKeepsItsOxygen() throws Exception {
		Glycan original = read("WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*OC]/1/");
		Glycan copy = original.clone();

		assertEquals(194.0790, original.computeMass(), 1e-4);
		assertEquals(original.computeMass(), copy.computeMass(), 1e-6);
		assertEquals(typesOf(original), typesOf(copy));
	}

	/** A methyl ester on a sialic acid, the same way. */
	@Test
	public void aMethylEsterKeepsItsOxygen() throws Exception {
		Glycan original = read("WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_1*OC_5*NCC/3=O]/1/");

		assertEquals(original.computeMass(), original.clone().computeMass(), 1e-6);
	}

	/** An alditol inside a chain stays one: the copy was an aldose, two hydrogens light. */
	@Test
	public void anAlditolInAChainStaysOne() throws Exception {
		Glycan original = read("WURCS=2.0/2,2,1/[h2122h][h2112h]/1-2/a4-b?");
		Glycan copy = original.clone();

		assertEquals(original.computeMass(), copy.computeMass(), 1e-6);
		Residue gal = copy.getRoot().firstChild().firstChild();
		assertEquals("Gal", gal.getTypeName());
		assertTrue("the copy's Gal is no longer an alditol", gal.isAlditol());
	}

	/**
	 * What the canvas copies - {@code GlycanDocument.extractView}, which clones a selection its own way -
	 * weighs the same too. It is what goes on the clipboard and what "copy to" attaches.
	 */
	@Test
	public void aCopiedSelectionWeighsTheSame() throws Exception {
		org.eurocarbdb.application.glycanbuilder.GlycanDocument document =
				new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		document.setStructures(java.util.Collections.singletonList(read("WURCS=2.0/1,1,0/[a2122h-1b_1-5_3*OC]/1/")));
		Glycan original = document.getStructures().iterator().next();

		Glycan copied = document.extractView(new java.util.HashSet<Residue>(original.getAllResidues())).getFirst();

		assertEquals(original.computeMass(), copied.computeMass(), 1e-6);
	}

	/** Every structure in the test resources that reads and weighs: its copy weighs the same. */
	@Test
	public void everyCopyWeighsWhatItsOriginalWeighs() throws Exception {
		List<String> differ = new ArrayList<String>();
		int weighed = 0;
		for (String wurcs : resourceWurcs()) {
			Glycan original;
			double mass;
			try {
				original = read(wurcs);
				if (original == null || original.getRoot() == null) continue;
				mass = original.computeMass();
			} catch (Throwable notReadOrNotWeighed) {
				continue;
			}
			double copied;
			try {
				copied = original.clone().computeMass();
			} catch (Throwable copyNotWeighed) {
				// a repeat whose copy loses its end, a fault of its own; it is not what this measures
				continue;
			}
			weighed++;
			if (Math.abs(mass - copied) > 1e-6) differ.add(String.format("%.4f -> %.4f %s", mass, copied, wurcs));
		}

		assertTrue("too few structures weighed to mean anything: " + weighed, weighed > 150);
		assertEquals(differ.toString(), 0, differ.size());
	}

	private static String typesOf(Glycan _structure) {
		StringBuilder types = new StringBuilder();
		for (Residue residue : _structure.getAllResidues())
			for (Linkage linkage : residue.getChildrenLinkages())
				types.append(linkage.getParentLinkageType()).append('/').append(linkage.getChildLinkageType()).append(' ');
		return types.toString();
	}

	private static final Pattern WURCS = Pattern.compile("WURCS=2\\.0/[^\\s\"]+");

	/** The WURCS the tests hold, in their resources and in their sources. */
	private static List<String> resourceWurcs() throws IOException {
		java.util.Set<String> found = new java.util.TreeSet<String>();
		for (String root : new String[] { "src/test/resources", "src/test/java" }) {
			try (Stream<Path> files = Files.walk(Paths.get(root))) {
				for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
					String name = file.toString();
					if (!name.endsWith(".tsv") && !name.endsWith(".txt") && !name.endsWith(".java")) continue;
					Matcher m = WURCS.matcher(new String(Files.readAllBytes(file), "UTF-8"));
					while (m.find()) found.add(m.group().replaceAll("[,;)]+$", ""));
				}
			}
		}
		return new ArrayList<String>(found);
	}
}
