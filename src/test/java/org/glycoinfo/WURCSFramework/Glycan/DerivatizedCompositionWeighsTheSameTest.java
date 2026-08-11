package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A derivatized composition weighs what the same glycan written with its linkages weighs.
 *
 * <p>It did not. Underivatized the two agreed (1.34.1), but derivatized the composition gained one
 * group per implicit glycosidic bond less one: permethylated Hex3HexNAc2 read 1190.6408 against
 * 1148.5938, which is 3 x CH2. A glycosidic bond consumes a hydroxyl that would otherwise be
 * derivatized, and the members - attached to the bracket rather than to one another - were
 * derivatized as though they were free.
 *
 * <p>The linked figures are checkable without this code: permethylated Man3GlcNAc2 as [M+Na]+ is
 * 1171.58 in the literature, and the permethylated Hex residue increment is 204.0998. The second is
 * the sharper test, and is asserted below - adding one Hex to a glycan must add exactly one
 * permethylated Hex residue, whichever way the glycan is written.
 */
public class DerivatizedCompositionWeighsTheSameTest {

	/** Published permethylated residue increments. */
	private static final double PERMETHYLATED_HEX = 204.0998;

	/**
	 * The residue dictionary and its mass tables are filled in when a workspace is built. Without
	 * one every residue type weighs zero, and the sums come out negative.
	 */
	@BeforeClass
	public static void loadTheResidueMasses() {
		BuilderWorkspace workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	@Test
	public void underEveryDerivatization() throws Exception {
		for (String derivatization : new String[] { MassOptions.NO_DERIVATIZATION,
				MassOptions.PERMETHYLATED, MassOptions.HEAVYPERMETHYLATION,
				MassOptions.PERDMETHYLATED, MassOptions.PERACETYLATED, MassOptions.PERDACETYLATED }) {
			assertEquals(derivatization,
					linkedMan3GlcNAc2(derivatization), composition(3, 2, derivatization), 1e-6);
		}
	}

	/** Permethylated Man3GlcNAc2, the figure a table of permethylated masses gives. */
	@Test
	public void andAgreesWithThePublishedFigure() throws Exception {
		assertEquals(1148.5938, composition(3, 2, MassOptions.PERMETHYLATED), 1e-3);
	}

	/**
	 * One more Hex adds one permethylated Hex residue, and nothing else.
	 *
	 * <p>The test that does not depend on remembering a total: whatever the absolute masses are, the
	 * step between consecutive sizes is the residue increment. It was 218.1154 - one CH2 too many
	 * every time.
	 */
	@Test
	public void andOneMoreHexAddsOneHexResidue() throws Exception {
		for (int hexoses = 1; hexoses < 6; hexoses++) {
			assertEquals("Hex" + hexoses + " -> Hex" + (hexoses + 1),
					PERMETHYLATED_HEX,
					composition(hexoses + 1, 2, MassOptions.PERMETHYLATED)
							- composition(hexoses, 2, MassOptions.PERMETHYLATED),
					1e-3);
		}
	}

	private static double composition(int hexoses, int hexNAcs, String derivatization) throws Exception {
		CompositionOptions counted = new CompositionOptions();
		counted.HEX = hexoses;
		counted.HEXNAC = hexNAcs;

		return counted.getCompositionAsGlycan(massOptions(derivatization)).computeMass();
	}

	/** The N-glycan core, written with its linkages: Hex3HexNAc2's structure. */
	private static double linkedMan3GlcNAc2(String derivatization) throws Exception {
		Glycan linked = Glycan.fromString("freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p"
				+ "(--3a1D-Man,p)--6a1D-Man,p$MONO,Und,0,0,freeEnd");
		linked.setMassOptions(massOptions(derivatization));

		return linked.computeMass();
	}

	private static MassOptions massOptions(String derivatization) {
		MassOptions options = new MassOptions();
		options.DERIVATIZATION = derivatization;
		options.ION_CLOUD = new IonCloud();

		return options;
	}
}
