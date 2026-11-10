package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A WURCS that is not valid, or that cannot be read whole, is refused with a reason, not opened in
 * part (#236).
 *
 * <p>The conversion did not check what it was given. The sequence of #236 has a GlcNAc whose carbon 2
 * carries both its N-acetyl and a linkage, a typing slip for {@code a4-b1}; it came in as two of its
 * twelve residues, with no word said. Two guards now: WURCSFramework's validator, before the
 * conversion; and after it, a count of the monosaccharides read against those the WURCS has.
 */
public class InvalidWurcsIsRefusedTest {

	/** #236, as reported. */
	private static final String ISSUE_236 = "WURCS=2.0/6,12,11/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]"
			+ "[a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][a1221m-1a_1-5]/1-1-2-3-1-4-5-3-1-4-5-6/a2-b1_a6-l1_b4-c1"
			+ "_c3-d1_c6-h1_d2-e1_e4-f1_f6-g2_h2-i1_i4-j1_j6-k2";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** #236 is refused, saying what the validator found. */
	@Test
	public void theReportedSequenceIsRefused() throws Exception {
		String message = refusal(ISSUE_236);
		assertTrue(message, message.contains("this WURCS is not valid"));
		assertTrue(message, message.contains("Only one modification is allowed to be connected to the CarbonDescriptor \"2\""));
	}

	/** And with a4-b1, as it was meant, it reads whole. */
	@Test
	public void asMeantItReadsWhole() throws Exception {
		Glycan read = read(ISSUE_236.replace("/a2-b1_", "/a4-b1_"));
		assertNotNull(read);
		assertEquals(12, sugars(read));
	}

	/**
	 * A valid sequence the conversion still drops part of is refused too. A sucrose-type bond, two
	 * anomeric carbons, lost its glucose: five of six read, weighing 828.27 where WURCSFramework says
	 * 990.33.
	 */
	@Test
	public void aShortReadIsRefused() throws Exception {
		String message = refusal("WURCS=2.0/2,6,5/[a2122h-1a_1-5][ha122h-2b_2-5]/1-2-2-2-2-2/a1-b2_b6-c2_c1-d2_c6-e2_e6-f2");
		assertTrue(message, message.contains("only 5 of the 6 monosaccharides"));
	}

	/** A residue the reader made up a name for is named in the refusal (#282). */
	@Test
	public void theRefusalNamesWhatCouldNotBeRead() throws Exception {
		String message = refusal("WURCS=2.0/1,1,0/[ad1221h-1a_1-5]/1/");
		assertTrue(message, message.contains("galHep is no residue this application knows"));
	}

	/**
	 * Across every WURCS this repository keeps for tests, nothing reads in part: each is read whole,
	 * or refused.
	 */
	@Test
	public void nothingReadsInPart() throws Exception {
		TreeSet<String> sequences = new TreeSet<String>();
		Pattern wurcs = Pattern.compile("WURCS=2\\.0/[^\"\\s\\\\]+");
		for (String root : new String[] { "src/test/resources", "src/test/java" })
			try (Stream<Path> files = Files.walk(Paths.get(root))) {
				for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
					Matcher m = wurcs.matcher(new String(Files.readAllBytes(file), "UTF-8"));
					while (m.find()) sequences.add(m.group());
				}
			}
		assertTrue("expected the corpora, found " + sequences.size(), sequences.size() > 100);

		List<String> partial = new ArrayList<String>();
		for (String sequence : sequences) {
			Glycan read;
			try {
				read = read(sequence);
			} catch (Exception refused) {
				continue;
			}
			if (read == null || read.isComposition()) continue;
			int expected = Integer.parseInt(sequence.split("/")[1].split(",")[1]);
			if (sugars(read) < expected) partial.add(sugars(read) + " of " + expected + ": " + sequence);
		}
		assertTrue(partial.size() + " read in part:\n" + String.join("\n", partial), partial.isEmpty());
	}

	// ------------------------------------------------------------------------------------------------

	private static Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, new MassOptions());
	}

	private static String refusal(String wurcs) throws Exception {
		try {
			read(wurcs);
		} catch (WURCSToGlycanException refused) {
			return refused.getMessage();
		}
		fail("it was read: " + wurcs);
		return null;
	}

	private static int sugars(Glycan glycan) {
		int n = 0;
		for (Residue r : glycan.getAllResidues())
			if (r.isSaccharide() && !r.isBridge()) n++;
		return n;
	}
}
