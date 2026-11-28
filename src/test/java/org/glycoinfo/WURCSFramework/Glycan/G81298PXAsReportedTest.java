package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueStyle;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.SNFGFootnote;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * G81298PX, which #257 reported drawn wrong in two ways, is read, drawn and written as it is.
 *
 * <p>The two were each fixed on their own: the 6-deoxy allose drawn with "6m" beside it (#258, by #284,
 * and drawn in SNFG's white deoxyhexose triangle by #291), and the 1,? anhydro beside the oleandrose
 * whose ring is not known (#259, by #297). This holds them together, on the structure they were found on.
 */
public class G81298PXAsReportedTest {

	/** G81298PX, as #257 gives it. */
	private static final String G81298PX = "WURCS=2.0/3,3,2/[ad122m-1x_1-?_3*OC][a2222m-1b_1-5_3*OC][a2122h-1b_1-5]"
			+ "/1-2-3/a4-b1_b4-c1";

	private BuilderWorkspace workspace;
	private Glycan read;

	@Before
	public void read() throws Exception {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
		workspace.setNotation(GraphicOptions.NOTATION_SNFG);
		read = new WURCS2Parser().readGlycan(G81298PX, workspace.getDefaultMassOptions());
	}

	/** Three monosaccharides and their two methyls, and nothing else: no anhydro, no "6m". */
	@Test
	public void everyResidueAndNothingElse() {
		List<String> names = new ArrayList<String>();
		for (Residue residue : read.getAllResidues())
			names.add(residue.getTypeName());
		assertEquals(Arrays.asList("freeEnd", "Oli", "Me", "All", "Me", "Glc"), names);
	}

	/** The 6-deoxy is on the allose, which is drawn in SNFG's white triangle and named in the footnote. */
	@Test
	public void theDeoxyAlloseIsAWhiteTriangle() {
		Residue allose = null;
		for (Residue residue : read.getAllResidues())
			if ("All".equals(residue.getTypeName())) allose = residue;
		assertEquals(Arrays.asList("6"), deoxyPositions(allose));

		ResidueStyle style = workspace.getGlycanRenderer().getResidueStyleDictionary().getStyle(allose);
		assertEquals("triangle", style.getShape());
		assertEquals(Color.WHITE, style.getFillColor());
		assertEquals(Arrays.asList("A=β-6-deoxy-D-Allp"), SNFGFootnote.of(read,
				workspace.getGlycanRenderer().getResidueStyleDictionary(), workspace.getGraphicOptions()).lines());
	}

	/** It weighs what WURCSFramework says it weighs, and is written back as it was read. */
	@Test
	public void itIsWeighedAndWrittenBackAsRead() throws Exception {
		MassOptions none = new MassOptions();
		none.setDerivatization(MassOptions.NO_DERIVATIZATION);
		Glycan underivatized = new WURCS2Parser().readGlycan(G81298PX, none);
		assertEquals(WURCSMassCalculator.calcMassWURCS(new WURCSFactory(G81298PX).getArray()).doubleValue(),
				underivatized.computeMass(), 1e-3);
		assertEquals(G81298PX, new WURCS2Parser().toWURCS(read));
	}

	private static List<String> deoxyPositions(Residue residue) {
		List<String> positions = new ArrayList<String>();
		for (String deoxy : residue.getDeoxies())
			positions.add(deoxy.replaceAll("[^0-9?]", ""));
		return positions;
	}
}
