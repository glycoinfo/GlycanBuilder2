package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That a structure knows what its bonds are made of as soon as it is read, whichever format it came
 * from (#4).
 *
 * <p>Every linkage is born {@code UNVALIDATED}, and the pass that works the types out ran in one
 * place only: the WURCS <i>writer</i>. So the same glycan carried different linkage types depending
 * on the format it had arrived in, and every other reader of a type - the GlycoCT writer, the
 * renderer - was reading a placeholder. That is the shape of #62, where a structure is drawn
 * differently depending on whether it was read from WURCS or from GlycoCT.</p>
 *
 * <p>The ordinary glycosidic bond had no branch in that pass at all, only substituents and bridges
 * did. The donor gives up the OH at its anomeric centre ({@code DEOXY}) and the acceptor keeps the
 * oxygen the bond is made through ({@code H_AT_OH}) - which is what the GlycoCT writer had been
 * assuming all along when it wrote {@code 1o(4+1)2d}.</p>
 */
public class LinkageTypesAreSetOnReadingTest {

	private static final String GWS = "freeEnd--?b1D-GlcNAc,p--4b1D-Gal,p$MONO,Und,0,0,freeEnd";
	private static final String WURCS =
			"WURCS=2.0/2,2,1/[a2122h-1b_1-5_2*NCC/3=O][a2112h-1b_1-5]/1-2/a4-b1";
	private static final String GLYCOCT =
			"RES\n1b:b-dglc-HEX-1:5\n2s:n-acetyl\n3b:b-dgal-HEX-1:5\n"
			+ "LIN\n1:1d(2+1)2n\n2:1o(4+1)3d\n";

	@BeforeClass
	public static void newWorkspace() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Read from GWS, the glycosidic bond says what it is. */
	@Test
	public void gwsGivesTheGlycosidicBondItsTypes() throws Exception {
		assertGlycosidicBondIsTyped(Glycan.fromString(GWS));
	}

	/** Read from WURCS, the same. It used to stay unvalidated until the moment it was written back. */
	@Test
	public void wurcsGivesTheGlycosidicBondItsTypes() {
		assertGlycosidicBondIsTyped(readAs(WURCS, "wurcs2"));
	}

	/** And read from GlycoCT, which is the pairing #62 is about. */
	@Test
	public void glycoCtGivesTheGlycosidicBondItsTypes() {
		assertGlycosidicBondIsTyped(readAs(GLYCOCT, "glycoct_condensed"));
	}

	/** A substituent's bond is typed on every route too, not only through WURCS. */
	@Test
	public void aSubstituentIsTypedWhicheverWayItArrives() throws Exception {
		Glycan fromGws = Glycan.fromString("freeEnd--?b1D-Glc,p--2?1N--??1Ac$MONO,Und,0,0,freeEnd");

		Linkage acetyl = linkageInto(fromGws, "Ac");
		assertEquals(LinkageType.NONMONOSACCHARID, acetyl.getChildLinkageType());
		assertEquals(LinkageType.H_AT_OH, acetyl.getParentLinkageType());
	}

	/**
	 * And saying so changes nothing about what comes out.
	 *
	 * <p>The exporters had their own assumptions for an unvalidated bond; stating the type in the
	 * model has to agree with them, or every stored sequence would shift under its users.</p>
	 */
	@Test
	public void whatIsWrittenOutIsUnchanged() throws Exception {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		document.addStructure(Glycan.fromString(GWS));

		document.exportFromStructure(document.getStructures(), "wurcs2");
		assertEquals(WURCS, document.getString().get(0).strip());
		document.clearString();

		document.exportFromStructure(document.getStructures(), "glycoct_condensed");
		assertTrue(document.getString().get(0), document.getString().get(0).contains("1o(4+1)3d"));
	}

	private static void assertGlycosidicBondIsTyped(Glycan structure) {
		Linkage glycosidic = linkageInto(structure, "Gal");

		assertEquals("the acceptor's side should keep its oxygen",
				LinkageType.H_AT_OH, glycosidic.getParentLinkageType());
		assertEquals("the donor's anomeric side should give up its OH",
				LinkageType.DEOXY, glycosidic.getChildLinkageType());
	}

	/** The linkage a named residue hangs from. */
	private static Linkage linkageInto(Glycan structure, String residueName) {
		for (Residue residue : structure.getAllResidues()) {
			if (residue.getTypeName().equals(residueName)) return residue.getParentLinkage();
		}

		throw new AssertionError("no " + residueName + " in " + structure);
	}

	private static Glycan readAs(String sequence, String format) {
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		try {
			assertTrue(format + " would not import", document.importFromString(sequence, format));
		} catch (Exception thrown) {
			throw new AssertionError(format + " threw: " + thrown, thrown);
		}

		return document.getStructures().get(0);
	}
}
