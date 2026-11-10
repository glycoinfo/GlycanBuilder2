package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.RelativeStereoInGWS;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A monosaccharide read in relative stereo is written back the way round it was read (#286).
 *
 * <p>Relative stereo - 3 and 4, no 1 or 2 - can be written either way round, ad3dh or ad4dh, and
 * WURCSFramework keeps each as a normal form of its own; the normal form is the canonical WURCS. A
 * residue has D, L and unknown and nothing more, so the writer wrote one representative, and for half
 * of what it read that was the other one: ad3dh came back as ad4dh, with nothing to say so.
 */
public class RelativeStereoIsKeptTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Both ways round, alone and on a glucose, each as WURCSFramework normalizes it. */
	@Test
	public void eachIsWrittenBackAsRead() throws Exception {
		for (String wurcs : new String[] {
				"WURCS=2.0/1,1,0/[ad3dh-1b_1-5_3*OC]/1/", "WURCS=2.0/1,1,0/[ad4dh-1b_1-5_3*OC]/1/",
				"WURCS=2.0/1,1,0/[a3433h-1b_1-5]/1/", "WURCS=2.0/1,1,0/[a4344h-1b_1-5]/1/",
				"WURCS=2.0/1,1,0/[ad433h-1x_1-5]/1/", "WURCS=2.0/1,1,0/[ad344h-1x_1-5]/1/",
				"WURCS=2.0/2,2,1/[a2122h-1b_1-5][ad3dh-1b_1-5_3*OC]/1-2/a4-b1",
				"WURCS=2.0/2,2,1/[a2122h-1b_1-5][a3433h-1b_1-5]/1-2/a4-b1" }) {
			assertEquals(wurcs, new WURCSFactory(wurcs).getWURCS(), write(read(wurcs)));
		}
	}

	/** A copy keeps it, as it keeps a deoxy. */
	@Test
	public void aCopyKeepsIt() throws Exception {
		Glycan read = read("WURCS=2.0/1,1,0/[ad3dh-1b_1-5_3*OC]/1/");
		Residue sugar = sugarOf(read);
		assertEquals("ad3dh", sugar.getRelativeSkeleton());
		assertEquals("ad3dh", sugar.cloneResidue().getRelativeSkeleton());
	}

	/** A residue changed since it was read is written as it now is, not as it was read. */
	@Test
	public void aResidueChangedSinceIsWrittenAsItIs() throws Exception {
		Glycan read = read("WURCS=2.0/1,1,0/[a3433h-1b_1-5]/1/");
		sugarOf(read).setChirality('D');
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1b_1-5]/1/", write(read));
	}

	/** Nothing is recorded for a residue with absolute stereo. */
	@Test
	public void absoluteStereoRecordsNothing() throws Exception {
		assertNull(sugarOf(read("WURCS=2.0/1,1,0/[a2122h-1b_1-5]/1/")).getRelativeSkeleton());
	}

	/**
	 * GWS has no place for it: reopened, a residue is written the writer's way. What the save loses is
	 * measured, so that it can be said at the save - and only where it changes the WURCS.
	 */
	@Test
	public void whatASaveLosesIsSaid() throws Exception {
		Glycan threeWay = read("WURCS=2.0/1,1,0/[ad3dh-1b_1-5_3*OC]/1/");
		Glycan reopened = Glycan.fromString(threeWay.toString());
		assertNull(sugarOf(reopened).getRelativeSkeleton());
		assertEquals("WURCS=2.0/1,1,0/[ad4dh-1b_1-5_3*OC]/1/", write(reopened));

		assertEquals(Arrays.asList(sugarOf(threeWay).getTypeName() + ": ad3dh -> ad4dh"),
				RelativeStereoInGWS.lostOnSave(Collections.singletonList(threeWay)));
		assertEquals(Arrays.asList("Glc: a3433h -> a4344h"),
				RelativeStereoInGWS.lostOnSave(Collections.singletonList(read("WURCS=2.0/1,1,0/[a3433h-1b_1-5]/1/"))));

		// the way round the writer writes anyway loses nothing, and nothing is said
		assertTrue(RelativeStereoInGWS.lostOnSave(Collections.singletonList(
				read("WURCS=2.0/1,1,0/[ad4dh-1b_1-5_3*OC]/1/"))).isEmpty());
		assertTrue(RelativeStereoInGWS.lostOnSave(Collections.singletonList(
				read("WURCS=2.0/1,1,0/[a2122h-1b_1-5]/1/"))).isEmpty());
	}

	// ------------------------------------------------------------------------------------------------

	private static Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, new MassOptions());
	}

	private static String write(Glycan glycan) throws Exception {
		return new WURCS2Parser().writeGlycan(glycan);
	}

	private static Residue sugarOf(Glycan glycan) {
		Residue last = null;
		for (Residue r : glycan.getAllResidues())
			if (r.isSaccharide()) last = r;
		if (last == null) throw new AssertionError("no monosaccharide in " + glycan);
		return last;
	}
}
