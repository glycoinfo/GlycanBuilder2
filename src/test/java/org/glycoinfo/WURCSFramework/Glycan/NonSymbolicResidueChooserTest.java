package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.OfferedTypes;
import org.glycoinfo.application.glycanbuilder.dialog.ResidueDesignDialog;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The monosaccharides with no symbol of their own can be chosen, and a placed "Assigned"
 * placeholder can be told which one it is (#252).
 *
 * <p>The palette offers the white pentagon, "Assigned", for a residue with no symbol of its own, and
 * there was no way to say which residue it was. The dialog that lists them existed but was on no menu,
 * and listed 36 of them by hand. The SNFG table it now also shows is {@link SnfgTableInTheChooserTest}'s.
 */
public class NonSymbolicResidueChooserTest {

	private static final String GLUCOSE = "freeEnd--?b1D-Glc,p$MONO,Und,0,0,freeEnd";

	private static BuilderWorkspace workspace;

	@BeforeClass
	public static void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
	}

	/**
	 * Every residue in the file is offered, once - but Pse, Leg and 4eLeg, names the main dictionary
	 * also has. A file reads those names as the residue with the SNFG symbol, so the copy without one
	 * would not come back as itself; the SNFG table offers them.
	 */
	@Test
	public void everyResidueIsListedOnce() {
		TreeSet<String> inFile = new TreeSet<String>();
		TreeSet<String> inBoth = new TreeSet<String>();
		for (ResidueType type : NonSymbolicResidueDictionary.getResidues()) {
			if (type.getName().startsWith("#")) continue;
			if (ResidueDictionary.hasResidueType(type.getName())) inBoth.add(type.getName());
			else inFile.add(type.getName());
		}
		assertEquals(new TreeSet<String>(Arrays.asList("4eLeg", "Leg", "Pse")), inBoth);

		List<String> listed = new ArrayList<String>();
		for (List<ResidueType> section : ResidueDesignDialog.sections().values())
			for (ResidueType type : section) listed.add(type.getName());

		// what cannot be written once drawn is not offered (#315); OfferedTypesTest names them
		TreeSet<String> writable = new TreeSet<String>();
		for (ResidueType type : NonSymbolicResidueDictionary.getResidues())
			if (inFile.contains(type.getName()) && OfferedTypes.residue(type)) writable.add(type.getName());

		assertEquals("listed twice: " + listed, new TreeSet<String>(listed).size(), listed.size());
		assertEquals(writable, new TreeSet<String>(listed));
		assertEquals(27, listed.size());
	}

	/** Grouped by backbone, in order; a residue the dialog's own list does not anticipate is still shown. */
	@Test
	public void theResiduesAreGroupedByBackbone() {
		Map<String, List<ResidueType>> sections = ResidueDesignDialog.sections(NonSymbolicResidueDictionary.getResidues());
		assertEquals(Arrays.asList("Triose", "Tetrose", "Pentose", "Hexose", "Heptose", "Octose", "Nonose", "Decose", "Other"),
				new ArrayList<String>(sections.keySet()));
		assertTrue(names(sections.get("Hexose")).containsAll(Arrays.asList("araHex", "4uGlcA", "GlcNA", "Rho")));
		assertTrue(names(sections.get("Pentose")).containsAll(Arrays.asList("dRib", "4dPenN", "lyxPen")));

		// what the chooser shows, which is only what can be written (#315): glycerol's section stays
		// for glycerol, which is offered apart, as Gro made an alditol
		Map<String, List<ResidueType>> offered = ResidueDesignDialog.sections();
		assertEquals(Arrays.asList("Triose", "Tetrose", "Pentose", "Hexose", "Heptose", "Nonose"),
				new ArrayList<String>(offered.keySet()));
		assertTrue(offered.get("Triose").isEmpty());
		assertTrue(names(offered.get("Hexose")).containsAll(Arrays.asList("araHex", "thrHexA")));
		assertFalse(names(offered.get("Hexose")).contains("4uGlcA"));
	}

	/** A placeholder becomes the residue chosen, where it stands. */
	@Test
	public void aPlaceholderBecomesTheResidueChosen() throws Exception {
		GlycanDocument doc = document();
		Residue glucose = sugarOf(doc);
		Residue placeholder = ResidueDictionary.newResidue("Assigned");
		assertNotNull(doc.addResidue(glucose, placeholder));
		assertTrue(GlycanDocument.isPlaceholder(placeholder));

		ResidueType araHex = NonSymbolicResidueDictionary.getResidueType("araHex");
		assertTrue(doc.assignPlaceholder(placeholder, null, araHex));

		assertEquals("araHex", placeholder.getTypeName());
		assertSame("it stays where it was", glucose, placeholder.getParent());
		assertFalse(GlycanDocument.isPlaceholder(placeholder));
		// the placeholder said nothing of the ring; the type does
		assertEquals(araHex.getRingSize(), placeholder.getRingSize());
		assertEquals(araHex.getChirality(), placeholder.getChirality());
	}

	/** What was set on the placeholder is kept. */
	@Test
	public void whatWasSetOnThePlaceholderIsKept() throws Exception {
		GlycanDocument doc = document();
		Residue placeholder = ResidueDictionary.newResidue("Assigned");
		doc.addResidue(sugarOf(doc), placeholder);
		placeholder.setAnomericState('a');
		placeholder.setRingSize('f');

		assertTrue(doc.assignPlaceholder(placeholder, null, NonSymbolicResidueDictionary.getResidueType("araHex")));
		assertEquals('a', placeholder.getAnomericState());
		assertEquals('f', placeholder.getRingSize());
	}

	/** Only a placeholder: any other residue is left alone. */
	@Test
	public void anyOtherResidueIsLeftAlone() throws Exception {
		GlycanDocument doc = document();
		Residue glucose = sugarOf(doc);
		assertFalse(doc.assignPlaceholder(glucose, null, NonSymbolicResidueDictionary.getResidueType("araHex")));
		assertEquals("Glc", glucose.getTypeName());
	}

	/**
	 * The five rows #273 records as contradicting themselves - an anomeric carbon of "-", a chirality
	 * of "/", a name opening with "?" - are written to GWS as text no reader can take back. They are
	 * data to correct, and are listed here so that a change to them is noticed either way.
	 */
	private static final TreeSet<String> UNREADABLE_273 = new TreeSet<String>(Arrays.asList(
			"?-gro-D-glcHep", "?-gro-D-manHep", "araOct", "manOct", "L-gro-D-glcOct"));

	/**
	 * Every residue a placeholder can be made is saved to GWS under its name, and read back as the same
	 * residue and the same text - but the five of #273.
	 *
	 * <p>The same type, and not only the same text: 24 residues whose names begin with a configuration,
	 * D-gro-D-galHep and the like, were written and read back as the same text and as a different
	 * residue, an unknown "gro-D-galHep" of no mass.
	 */
	@Test
	public void everyChoiceIsSavedToGwsAndReadBack() throws Exception {
		TreeSet<String> failed = new TreeSet<String>();
		List<String> failures = new ArrayList<String>();
		// every residue of the file, offered or not: a file can hold one the chooser no longer offers
		for (List<ResidueType> section : ResidueDesignDialog.sections(NonSymbolicResidueDictionary.getResidues()).values()) {
			for (ResidueType type : section) {
				GlycanDocument doc = document();
				Residue placeholder = ResidueDictionary.newResidue("Assigned");
				doc.addResidue(sugarOf(doc), placeholder);
				if (!doc.assignPlaceholder(placeholder, null, type)) {
					failures.add(type.getName() + ": not assigned");
					continue;
				}
				String gws = doc.getFirstStructure().toString();
				Glycan read = Glycan.fromString(gws);
				if (read == null || !gws.equals(read.toString()) || !hasResidue(read, type.getName())) {
					failed.add(type.getName());
					failures.add(type.getName() + ": " + gws + " -> " + (read == null ? null : read.toString()));
				}
			}
		}
		assertEquals(String.join("\n", failures), UNREADABLE_273, failed);
	}

	// ------------------------------------------------------------------------------------------------

	private static GlycanDocument document() throws Exception {
		GlycanDocument doc = new GlycanDocument(workspace);
		doc.addStructures(Arrays.asList(Glycan.fromString(GLUCOSE)));
		return doc;
	}

	private static Residue sugarOf(GlycanDocument doc) {
		for (Residue r : doc.getFirstStructure().getAllResidues())
			if (r.isSaccharide()) return r;
		throw new AssertionError("no monosaccharide");
	}

	private static boolean hasResidue(Glycan glycan, String typeName) {
		for (Residue r : glycan.getAllResidues())
			if (typeName.equals(r.getTypeName())) return true;
		return false;
	}

	private static List<String> names(List<ResidueType> types) {
		List<String> names = new ArrayList<String>();
		for (ResidueType type : types) names.add(type.getName());
		return names;
	}
}
