package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Every composition count, through the road the application actually takes (#219).
 *
 * <p>The encoder's own tests ask for a {@code CompositionResidue} <b>by name</b>, so only names the
 * encoder already knows can appear in one. That is how a composition containing sialic acid came to
 * export as an empty file and stay that way through four releases: the residue type is called
 * {@code NeuAc}, the composition calls it {@code Neu5Ac}, and nothing on the road between them
 * translated. The single assertion that did go through the application's path used Hex and HexNAc —
 * the two residues whose names happen to agree.
 *
 * <p>So this walks every count {@link CompositionOptions} has, sets it, builds the composition the way
 * the dialog does, and writes it. **A test that constructs its input the way the code under test likes
 * it will not find a fault on the way in.**
 */
public class EveryCompositionTheDialogOffersWritesTest {

	/**
	 * The counts that should produce a WURCS.
	 *
	 * <p>Thirteen: the twelve the web application's dialog offers, and Hep, which the desktop dialog
	 * offers as well.
	 */
	private static final List<String> SHOULD_WRITE = Arrays.asList(
			"PEN", "HEX", "HEP", "HEXN", "HEXNAC", "DHEX", "DDHEX", "HEXA",
			"NEU5AC", "NEU5GC", "KDO", "KDN", "MUR");

	/**
	 * The counts that cannot be written, and are known not to be.
	 *
	 * <p>Held here so the list is a statement rather than an absence. {@code 4dPen}, {@code MeH},
	 * {@code dHexA} and the two lactonised sialic acids have no composition residue to be counted as —
	 * and, measured, cannot be written as ordinary structures either, so they are not a composition
	 * fault. The rest are substituents, user-defined residues and the two placeholders. All of it is
	 * #220.
	 */
	private static final List<String> KNOWN_NOT_TO = Arrays.asList(
			"DPEN", "MEHEX", "DHEXA", "NEU5ACLAC", "NEU5GCLAC",
			"OR1", "OR2", "OR3", "S", "P", "AC", "PYR", "PC", "KETOSE", "UNKNOWN");

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	/** Each of the thirteen writes something. */
	@Test
	public void everyOfferedResidueWrites() throws Exception {
		List<String> silent = new ArrayList<String>();
		for (String count : SHOULD_WRITE)
			if (writes(count) == null) silent.add(count + " (" + residueMadeBy(count) + ")");

		assertTrue("these composition counts exported as an empty file: " + silent, silent.isEmpty());
	}

	/** Sialic acid in particular, since that is the one that was refused and the one users reach for. */
	@Test
	public void aSialylatedCompositionWrites() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.HEX = 5;
		counts.HEXNAC = 4;
		counts.NEU5AC = 2;
		counts.DHEX = 1;

		String written = new WURCS2Parser().writeGlycan(counts.getCompositionAsGlycan(neutral()));

		assertFalse("a sialylated composition exported as an empty file", written.isEmpty());
		assertTrue("the residues are not the ones expected: " + written,
				written.startsWith("WURCS=2.0/4,12,11/[Aad21122h-2x_2-6_5*NCC/3=O][axxxxm-1x_1-5]"
						+ "[axxxxh-1x_1-5_2*NCC/3=O][axxxxh-1x_1-5]/1-1-2-3-3-3-3-4-4-4-4-4/"));
	}

	/**
	 * And the ones that cannot be written are still the ones we think they are.
	 *
	 * <p>Not an assertion that they should stay broken — it is #220's list, and this fails if one starts
	 * working, which is the moment to move it into the list above.
	 */
	@Test
	public void theOnesThatCannotAreStillTheOnesWeThinkTheyAre() throws Exception {
		List<String> nowWriting = new ArrayList<String>();
		for (String count : KNOWN_NOT_TO)
			if (writes(count) != null) nowWriting.add(count);

		assertEquals("these now write, and belong in the offered list instead: " + nowWriting,
				0, nowWriting.size());
	}

	/** @return Returns what the count wrote, or null where it wrote nothing. */
	private static String writes(String count) throws Exception {
		Glycan composition = compositionOf(count);
		if (composition == null) return null;

		String written = new WURCS2Parser().writeGlycan(composition);
		return written.isEmpty() ? null : written;
	}

	private static String residueMadeBy(String count) throws Exception {
		StringBuilder made = new StringBuilder();
		for (Residue residue : compositionOf(count).getAllResidues())
			if (!residue.isReducingEnd() && !residue.isBracket() && residue.getType() != null)
				made.append(residue.getType().getName());

		return made.toString();
	}

	private static Glycan compositionOf(String count) throws Exception {
		CompositionOptions counts = new CompositionOptions();
		Field field = CompositionOptions.class.getField(count);
		field.setInt(counts, 1);

		return counts.getCompositionAsGlycan(neutral());
	}

	private static MassOptions neutral() {
		MassOptions neutral = new MassOptions();
		neutral.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		neutral.ION_CLOUD = new IonCloud();

		return neutral;
	}
}
