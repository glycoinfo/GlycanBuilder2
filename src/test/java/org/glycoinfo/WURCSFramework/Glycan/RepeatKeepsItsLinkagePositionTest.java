package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That the position a repeat closes on survives a round trip (#204).
 *
 * <p>It did not: {@code l1-m3~n} was read in and written back as {@code l?-m3~n}. Every other linkage
 * in the sequence round-tripped. {@code ?} is not a rounding of {@code 1} — it is the sequence saying
 * the position is unknown, about a position the input had stated, so the export said less than the
 * import and said it in a form that looks deliberate.
 *
 * <p><b>Where it went.</b> {@code addChild} rebuilds a linkage from its bonds and finishes by taking
 * the child residue's own anomeric carbon. The closing marker is created fresh and has none, so the
 * position {@code GLINToLinkage} had just worked out from the donor side was read back over with
 * {@code '?'}. The opening marker never had the problem, because
 * {@code makeEdgeWithStartBracket} sets its anomeric carbon before adding it; only the closing side
 * was missing that line.
 */
public class RepeatKeepsItsLinkagePositionTest {

	/**
	 * G03246MZ, the structure the fault was found on while measuring #57 — a repeating unit inside an
	 * N-glycan antenna.
	 */
	private static final String G03246MZ = "WURCS=2.0/7,15,15/[h2122h_2*NCC/3=O]"
			+ "[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5]"
			+ "[Aad21122h-2a_2-6_5*NCC/3=O][a1221m-1a_1-5]/1-2-3-4-2-5-6-2-5-6-4-2-5-6-7/"
			+ "a4-b1_a6-o1_b4-c1_c3-d1_c6-k1_d2-e1_d4-h1_e4-f1_f3-g2_h4-i1_i3-j2_k2-l1_l4-m1_m3-n2"
			+ "_l1-m3~n";

	/**
	 * A repeat and nothing else, so a failure here cannot be blamed on anything around it: GlcNAc
	 * and GlcA repeating through {@code a1-b3}.
	 */
	private static final String REPEAT_ALONE =
			"WURCS=2.0/2,2,2/[a2122h-1b_1-5_2*NCC/3=O][a2122A-1b_1-5]/1-2/a4-b1_a1-b3~n";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * The smallest case: the repeat's own position comes back as it went in.
	 *
	 * <p>Without the fix this reads {@code a?-b3~n}.
	 */
	@Test
	public void aRepeatAloneKeepsItsPosition() throws Exception {
		assertRoundTrip(REPEAT_ALONE);
	}

	/** And in the structure it was reported on, where the repeat sits inside an antenna. */
	@Test
	public void aRepeatInsideAnAntennaKeepsItsPosition() throws Exception {
		assertRoundTrip(G03246MZ);
	}

	private void assertRoundTrip(String wurcs) throws Exception {
		WURCS2Parser parser = new WURCS2Parser();
		Glycan glycan = parser.readGlycan(wurcs, new MassOptions());
		assertEquals("the position the repeat closes on did not survive", wurcs,
				parser.writeGlycan(glycan));
	}
}
