package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converterGWS.GWSParser;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A file written before the double dash opens as the structure it describes (#274).
 *
 * <p>GWS linkages were once written {@code -4} where today's files say {@code --4}. The reader kept a
 * branch for the old form, and the name grammar was narrowed specifically so that {@code -4} stays a
 * linkage rather than becoming part of a residue name. That branch parsed correctly and then never
 * told the child residue about its linkage. The modern branch always did. The first thing to ask the
 * child for its parent linkage, {@code LinkageTypeOptimizer} on every load, threw a
 * {@code NullPointerException}, and {@code fromString} swallowed it. The file opened as empty with no
 * message.
 *
 * <p>Each case below is compared against the same structure written the modern way, not against a
 * hand-written expectation, so what is asserted is "means the same thing" rather than "matches what
 * the author of this test thought it meant".
 */
public class PreDoubleDashLinkagesLoadTest {

	private static final String TAIL = "$MONO,Und,0,0,freeEnd";

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** The reporter's case: one residue on a free end. */
	@Test
	public void aSingleOldStyleLinkageLoads() throws Exception {
		assertMeansTheSame("freeEnd-1b1D-GlcNAc,p", "freeEnd--1b1D-GlcNAc,p");
	}

	/** Every position the old form allowed: a digit, N and ?. */
	@Test
	public void everyPositionTheOldFormAllowedLoads() throws Exception {
		assertMeansTheSame("freeEnd--??1D-GlcNAc,p-4b1D-Gal,p", "freeEnd--??1D-GlcNAc,p--4b1D-Gal,p");
		assertMeansTheSame("freeEnd--??1D-GlcNAc,p-Nb1D-Gal,p", "freeEnd--??1D-GlcNAc,p--Nb1D-Gal,p");
		assertMeansTheSame("freeEnd--??1D-GlcNAc,p-?b1D-Gal,p", "freeEnd--??1D-GlcNAc,p--?b1D-Gal,p");
	}

	/** Branches, and an N-glycan core written entirely in the old form. */
	@Test
	public void branchesAndAWholeCoreLoad() throws Exception {
		assertMeansTheSame("freeEnd--?b1D-Man,p(-3a1D-Man,p)-6a1D-Man,p",
				"freeEnd--?b1D-Man,p(--3a1D-Man,p)--6a1D-Man,p");
		assertMeansTheSame("freeEnd-?b1D-GlcNAc,p-4b1D-GlcNAc,p-4b1D-Man,p(-3a1D-Man,p)-6a1D-Man,p",
				"freeEnd--?b1D-GlcNAc,p--4b1D-GlcNAc,p--4b1D-Man,p(--3a1D-Man,p)--6a1D-Man,p");
	}

	/**
	 * The cause itself: the child knows its linkage.
	 *
	 * <p>Pinned directly so that a future change to the optimizer, which might stop asking, cannot make
	 * this pass while leaving the model wrong for whatever asks next.
	 */
	@Test
	public void theChildKnowsItsLinkage() throws Exception {
		Residue root = GWSParser.readSubtree("freeEnd-1b1D-GlcNAc,p", false);
		Linkage linkage = root.getChildrenLinkages().get(0);

		assertSame(linkage, linkage.getChildResidue().getParentLinkage());
	}

	/** Read the old form and the modern form, and require the same structure out of both. */
	private static void assertMeansTheSame(String _old, String _modern) throws Exception {
		Glycan old = Glycan.fromString(_old + TAIL);
		Glycan modern = Glycan.fromString(_modern + TAIL);

		assertNotNull("the old form does not load: " + _old, old);
		assertNotNull("the control does not load: " + _modern, modern);
		assertEquals("the old form reads as a different structure", modern.toString(), old.toString());
		assertEquals("and weighs differently", modern.computeMass(), old.computeMass(), 1e-6);
	}
}
