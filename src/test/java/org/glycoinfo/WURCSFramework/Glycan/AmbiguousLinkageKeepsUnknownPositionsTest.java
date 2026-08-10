package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SVGUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That an antenna written the other way round still says its position is unknown (#150).
 *
 * <p>An antenna names the residues it may hang from, and WURCS writes the two sides in whichever
 * order puts the residue linking through its anomeric carbon on the donor side. A sialic acid
 * leaves from C2, so G00955WX is written {@code l2-a?|…|k?} - antenna first, as the donor. G42735RP
 * says {@code m1-f?|i?|k?} instead, position 1 on a residue whose anomeric carbon is 2, and is
 * parsed the other way round: the antenna as the acceptor, its candidates as donors.</p>
 *
 * <p>Reading it as though the sides meant the usual thing took the antenna's own 1 for the position
 * on each candidate. The structure came out drawn as 1-linked to the galactoses and written back as
 * {@code m2-f1|i1|k1} - a definite position where the sequence said it was unknown. The same shape
 * on G00955WX did not read at all.</p>
 */
public class AmbiguousLinkageKeepsUnknownPositionsTest {

	/** G42735RP as registered: the antenna written on the acceptor side, at position 1. */
	private static final String THE_OTHER_WAY_ROUND =
			"WURCS=2.0/7,13,12/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]"
			+ "[a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a1221m-1a_1-5]"
			+ "[Aad21122h-2a_2-6_5*NCC/3=O]/1-2-3-4-2-5-4-2-5-2-5-6-7"
			+ "/a4-b1_a6-l1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_g6-j1_h4-i1_j4-k1"
			+ "_m1-f?|i?|k?}";

	/** The same structure with the sialic acid's own anomeric carbon, which was always read right. */
	private static final String THE_USUAL_WAY_ROUND =
			THE_OTHER_WAY_ROUND.replace("_m1-f?|i?|k?}", "_m2-f?|i?|k?}");

	/** G00955WX, whose antenna is written the usual way round. */
	private static final String G00955WX =
			"WURCS=2.0/5,12,11/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]"
			+ "[a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-1-4-3-1-4-5"
			+ "/a4-b1_b4-c1_c3-d1_c6-i1_d2-e1_d4-g1_e4-f1_g4-h1_i2-j1_j4-k1"
			+ "_l2-a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?}";

	/** And the same, written the other way round - the shape that would not read at all. */
	private static final String G00955WX_THE_OTHER_WAY_ROUND =
			G00955WX.replace("_l2-a?", "_l1-a?");

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The position on the residues the antenna may hang from is unknown, and stays unknown. */
	@Test
	public void theAntennaDoesNotClaimAPositionOnItsCandidates() throws Exception {
		Residue antenna = read(THE_OTHER_WAY_ROUND).getBracket().getChildAt(0);

		assertEquals("the antenna's own position was taken for the position on its candidates",
				"?", antenna.getParentLinkage().getParentPositionsString());
	}

	/** So it is written back saying what it was given: f, i and k at an unknown position. */
	@Test
	public void theUnknownPositionsSurviveTheRoundTrip() throws Exception {
		String written = writeAs(read(THE_OTHER_WAY_ROUND), "wurcs2");

		assertTrue("a definite position was invented on the candidates: " + written,
				written.endsWith("_m2-f?|i?|k?}"));
	}

	/**
	 * And it is drawn as the same glycan either way round, which is the point.
	 *
	 * <p>The two sequences say the same thing about where the sialic acid may go; only the position
	 * written beside the sialic acid itself differs, and that is not what the picture shows.</p>
	 */
	@Test
	public void bothWaysRoundDrawTheSamePicture() throws Exception {
		assertEquals("the same antenna is drawn differently depending on how it was written",
				draw(read(THE_USUAL_WAY_ROUND)), draw(read(THE_OTHER_WAY_ROUND)));
	}

	/**
	 * The other way round reads at all, and keeps every residue.
	 *
	 * <p>G00955WX written that way used to fail to import - a null acceptor in LinkageConnector -
	 * and the reading that replaced the crash dropped the last galactose, because the antenna was
	 * taken for that residue's parent as well.</p>
	 */
	@Test
	public void nothingIsLostWhenTheAntennaIsWrittenTheOtherWayRound() throws Exception {
		assertEquals("residues went missing when the antenna was written the other way round",
				read(G00955WX).getAllResidues().size(),
				read(G00955WX_THE_OTHER_WAY_ROUND).getAllResidues().size());
	}

	private static String draw(Glycan structure) throws Exception {
		return SVGUtils.getVectorGraphics(
				new GlycanRendererAWT(), Collections.singletonList(structure), false, false);
	}

	private static Glycan read(String sequence) throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue("the WURCS would not import", document.importFromString(sequence, "wurcs2"));

		return document.getStructures().get(0);
	}

	private static String writeAs(Glycan structure, String format) {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		document.addStructure(structure);
		document.exportFromStructure(document.getStructures(), format);
		assertTrue("nothing came out as " + format, !document.getString().isEmpty());

		return document.getString().get(0);
	}
}
