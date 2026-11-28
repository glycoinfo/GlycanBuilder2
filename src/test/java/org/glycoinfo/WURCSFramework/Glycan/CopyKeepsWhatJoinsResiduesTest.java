package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A copy keeps what joins its residues other than through the tree (#340): the ends of a repeat and of
 * a cycle, and an antenna's candidate parents.
 *
 * <p>Copied residue by residue and bond by bond, a copy left them behind. A repeat's or a cycle's copy
 * - and a structure whose two residues are joined by two bonds is read as a cycle - could not be
 * written, and an N-glycan's antenna of uncertain parent was written with every residue as a candidate
 * parent, another structure, without a word. Of the test resources' structures, 24 copies wrote
 * something other than their original.
 */
public class CopyKeepsWhatJoinsResiduesTest {

	private static final String REPEAT =
			"WURCS=2.0/2,2,2/[a2122h-1b_1-5_2*NCC/3=O][a2122A-1b_1-5]/1-2/a4-b1_a1-b3~n";
	private static final String CYCLE =
			"WURCS=2.0/1,7,7/[a2122h-1a_1-5_2*OC_3*OC_6*N]/1-1-1-1-1-1-1/a1-g4_a4-b1_b4-c1_c4-d1_d4-e1_e4-f1_f4-g1";
	private static final String TWO_BONDS =
			"WURCS=2.0/2,2,2/[h4344h_2*NCC/3=O][a2112h-1b_1-5]/1-2/a4-b1_a6-b2";
	private static final String ANTENNA = "WURCS=2.0/6,11,10/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]"
			+ "[a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a1221m-1a_1-5]/1-2-3-4-2-5-4-2-6-2-5/"
			+ "a4-b1_a6-i1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_j4-k1_j1-d4|d6|g4|g6}";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	private static Glycan read(String _wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(_wurcs, new MassOptions());
	}

	private static String write(Glycan _structure) throws Exception {
		return new WURCS2Parser().toWURCS(_structure);
	}

	/** A repeat, a cycle, two residues joined by two bonds, and an antenna: each copy writes its original. */
	@Test
	public void eachCopyWritesItsOriginal() throws Exception {
		for (String sequence : new String[] { REPEAT, CYCLE, TWO_BONDS, ANTENNA }) {
			Glycan original = read(sequence);
			assertEquals(sequence, write(original), write(original.clone()));
		}
	}

	/** The antenna's candidate parents are the copies of its original's, not every residue. */
	@Test
	public void anAntennasParentsAreTheCopiesOfItsOwn() throws Exception {
		Glycan original = read(ANTENNA);
		Glycan copy = original.clone();

		Residue antenna = original.getBracket().firstChild();
		Residue copied = copy.getBracket().firstChild();
		assertEquals(antenna.getParentsOfFragment().size(), copied.getParentsOfFragment().size());
		Set<Residue> inTheCopy = identitySet(copy.getAllResidues());
		for (Residue parent : copied.getParentsOfFragment())
			assertTrue("a candidate parent outside the copy", inTheCopy.contains(parent));
	}

	/** A repeat's copy refers to the copy's own ends, never to the original's. */
	@Test
	public void aRepeatsEndsAreTheCopysOwn() throws Exception {
		Glycan original = read(REPEAT);
		Glycan copy = original.clone();
		Set<Residue> inTheCopy = identitySet(copy.getAllResidues());

		for (Residue residue : copy.getAllResidues())
			for (Residue end : new Residue[] { residue.getEndRepitionResidue(), residue.getStartRepetitionResidue(),
					residue.getStartCyclicResidue(), residue.getEndCyclicResidue() })
				if (end != null) assertTrue(residue.getTypeName() + " refers outside its copy", inTheCopy.contains(end));
	}

	/** What the canvas copies, the whole structure selected, writes what the structure writes. */
	@Test
	public void aCopiedSelectionWritesItsOriginal() throws Exception {
		for (String sequence : new String[] { REPEAT, TWO_BONDS, ANTENNA }) {
			GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
			document.setStructures(java.util.Collections.singletonList(read(sequence)));
			Glycan original = document.getStructures().iterator().next();

			Glycan copied = document.extractView(new HashSet<Residue>(original.getAllResidues())).getFirst();
			assertNotSame(original, copied);
			assertEquals(sequence, write(original), write(copied));
		}
	}

	/**
	 * Every structure in the test resources whose original is written: its copy writes the same. One
	 * whose original refers to a residue outside its own tree - a cycle around a repeat whose end markers
	 * the reader never attaches - is a fault of the reading, not of the copy, and is left out.
	 */
	@Test
	public void everyCopyWritesWhatItsOriginalWrites() throws Exception {
		List<String> differ = new ArrayList<String>();
		int compared = 0;
		for (String wurcs : resourceWurcs()) {
			Glycan original;
			String written;
			try {
				original = read(wurcs);
				if (original == null || original.getRoot() == null || refersOutside(original)) continue;
				written = write(original);
			} catch (Throwable notReadOrNotWritten) {
				continue;
			}
			compared++;
			String copied;
			try {
				copied = write(original.clone());
			} catch (Throwable copyNotWritten) {
				copied = "cannot be written: " + copyNotWritten.getMessage();
			}
			if (!written.equals(copied)) differ.add(wurcs + " -> " + copied);
		}

		assertTrue("too few structures compared to mean anything: " + compared, compared > 200);
		assertEquals(differ.toString(), 0, differ.size());
	}

	/** Whether a residue of the structure refers to one that is not in it. */
	private static boolean refersOutside(Glycan _structure) {
		Set<Residue> inIt = identitySet(_structure.getAllResidues());
		for (Residue residue : _structure.getAllResidues())
			for (Residue end : new Residue[] { residue.getEndRepitionResidue(), residue.getStartRepetitionResidue(),
					residue.getStartCyclicResidue(), residue.getEndCyclicResidue() })
				if (end != null && !inIt.contains(end)) return true;
		return false;
	}

	private static Set<Residue> identitySet(java.util.Collection<Residue> _residues) {
		Set<Residue> set = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Residue, Boolean>());
		set.addAll(_residues);
		return set;
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
