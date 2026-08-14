package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * That an acyl at the position of a residue's own amine substitutes the nitrogen rather than claiming
 * the carbon (#211).
 *
 * <p>{@code GlcN}'s 2 is left out of its linkage positions because the amine is there, and 1.37.0
 * began enforcing that list against every child — so acylating the amine, which is how a GlcNAc is
 * built up a step at a time, was refused. The exporter had always read it the other way and wrote
 * exactly GlcNAc's WURCS for it.
 *
 * <p><i>"Glc + 2N -&gt; GlcN + 2Ac -&gt; GlcNAc となり、Glc の C2 位の OH が、NHAc に置換され、GlcNAc
 * となります"</i> — I. Yamada, 2026-08-15. The position names the site; what sits there is the group
 * being modified.
 *
 * <p><b>Both halves of the rule are read from the dictionary</b>, not derived: the type's IUPAC name
 * says where the nitrogen is ({@code Glc$2N}, {@code Glc$2NAc}), and the type offers {@code N} among
 * its positions when that nitrogen has room. This corner has now taught twice over that chemistry
 * which is reasoned about rather than declared comes out wrong — the bridge exemption was the first
 * time.
 */
public class AcylatingTheAmineIsNotClaimingTheCarbonTest {

	/** What a GlcNAc weighs as a sequence, whichever way it was spelled. */
	private static final String GLCNAC = "WURCS=2.0/1,1,0/[a2122h-1x_1-5_2*NCC/3=O]/1/";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * The case that regressed: an acetyl on a glucosamine's amine, written as position 2.
	 *
	 * <p>Refused on 1.37.0 and 1.38.0; attached and correct on every version before them.
	 */
	@Test
	public void anAcetylOnAGlucosaminesAmineAttaches() throws Exception {
		Residue glcN = ResidueDictionary.newResidue("GlcN");

		assertTrue("acylating the amine at 2 was refused",
				glcN.addChild(ResidueDictionary.newResidue("Ac"), '2'));
	}

	/**
	 * And it is a GlcNAc, not merely attached.
	 *
	 * <p>The assertion that matters: the exporter combines the residue's own {@code 2*N} with the
	 * acetyl into {@code 2*NCC/3=O}, so the stepwise spelling and the named residue write the same
	 * sequence. Attaching without that would be a worse outcome than refusing.
	 */
	@Test
	public void andItWritesTheSameWURCSAsTheNamedResidue() throws Exception {
		Residue glcN = ResidueDictionary.newResidue("GlcN");
		glcN.addChild(ResidueDictionary.newResidue("Ac"), '2');

		assertEquals("the named residue does not write what was expected",
				GLCNAC, write(ResidueDictionary.newResidue("GlcNAc")));
		assertEquals("a GlcN acylated at 2 should write the same sequence as a GlcNAc",
				GLCNAC, write(glcN));
	}

	/**
	 * A nitrogen that is already acylated keeps its position closed.
	 *
	 * <p>GlcNAc omits 2 <em>and</em> offers no {@code N}, which is the dictionary saying the nitrogen
	 * is spoken for. This is the half that stops the fix from becoming "substituents may go anywhere".
	 */
	@Test
	public void anAmideNitrogenStaysClosed() throws Exception {
		assertFalse("GlcNAc's 2 carries the N-acetyl and should stay closed",
				ResidueDictionary.newResidue("GlcNAc").addChild(
						ResidueDictionary.newResidue("Me"), '2'));
	}

	/**
	 * Only a substituent qualifies. A monosaccharide there would be a glycosidic bond to something
	 * that is not a hydroxyl, and nothing measured supports it.
	 */
	@Test
	public void aSugarAtTheNitrogenIsStillRefused() throws Exception {
		assertFalse("a Gal at the amine's position should be refused",
				ResidueDictionary.newResidue("GlcN").addChild(
						ResidueDictionary.newResidue("Gal"), '2'));
	}

	/**
	 * The exemption is for the nitrogen's own position and nothing else: a position the sugar does not
	 * have is still refused, and so is a second child at the nitrogen.
	 */
	@Test
	public void theExemptionReachesNoFurther() throws Exception {
		assertFalse("a pentose has no 6",
				ResidueDictionary.newResidue("Xyl").addChild(
						ResidueDictionary.newResidue("Me"), '6'));

		Residue glcN = ResidueDictionary.newResidue("GlcN");
		assertTrue(glcN.addChild(ResidueDictionary.newResidue("Ac"), '2'));
		assertFalse("the amine already carries one, and a carbon carries one bond",
				glcN.addChild(ResidueDictionary.newResidue("Me"), '2'));
	}

	private static String write(Residue residue) throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		root.addChild(residue);

		return new WURCS2Parser().writeGlycan(new Glycan(root, false, MassOptions.empty()));
	}
}
