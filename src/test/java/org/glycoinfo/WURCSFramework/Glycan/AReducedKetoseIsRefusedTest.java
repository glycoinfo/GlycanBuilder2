package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.GlycanToWURCSException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A reduced ketose is not written to WURCS, and says why (#364).
 *
 * <p>Reducing a ketose's carbonyl makes that carbon a stereocentre, both ways round: D-fructose
 * gives D-mannitol and D-glucitol. GlycanBuilder2 has no residue for the pair, and the maintainer
 * decided on 2026-11-29 to treat it as a structure it does not support. It was written as the ketose
 * itself, the open-chain sugar - reduced D-Fru as hO122h - and read back as that, two hydrogens
 * lighter; with a residue on it, it failed with a NullPointerException.
 */
public class AReducedKetoseIsRefusedTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
	}

	/** Fru, Tag, Sor, Sed, Kdo and NeuAc reduced, alone or with a residue on them: refused, naming them. */
	@Test
	public void aReducedKetoseIsRefused() throws Exception {
		String[][] reduced = {
				{ "redEnd--?D-Fru,o$MONO,Und,0,0,redEnd", "D-Fru" },
				{ "redEnd--?D-Tag,o$MONO,Und,0,0,redEnd", "D-Tag" },
				{ "redEnd--?L-Sor,o$MONO,Und,0,0,redEnd", "L-Sor" },
				{ "redEnd--?D-Sed,o$MONO,Und,0,0,redEnd", "D-Sed" },
				{ "redEnd--?D-KDO,o$MONO,Und,0,0,redEnd", "D-Kdo" },
				{ "redEnd--?D-NeuAc,o$MONO,Und,0,0,redEnd", "D-NeuAc" },
				{ "redEnd--?D-Fru,o--1b2D-Glc,p$MONO,Und,0,0,redEnd", "D-Fru" } };
		for (String[] each : reduced) {
			Glycan structure = Glycan.fromString(each[0]);
			try {
				String written = new WURCS2Parser().toWURCS(structure);
				fail(each[0] + " was written: " + written);
			} catch (GlycanToWURCSException refused) {
				String why = String.valueOf(refused.getMessage());
				assertTrue(why, why.startsWith(each[1] + " is drawn reduced, and a reduced ketose is not one compound"));
			}
		}
	}

	/** A reduced aldose, and a ketose in its ring, are written as they were. */
	@Test
	public void anAldoseAndAKetoseRingAreWritten() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[h2122h]/1/",
				new WURCS2Parser().toWURCS(Glycan.fromString("redEnd--?D-Glc,o$MONO,Und,0,0,redEnd")));
		assertEquals("WURCS=2.0/1,1,0/[ha122h-2b_2-5]/1/",
				new WURCS2Parser().toWURCS(Glycan.fromString("freeEnd--?b2D-Fru,f$MONO,Und,0,0,freeEnd")));
	}
}
