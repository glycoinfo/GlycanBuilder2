package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidationReport;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.CrossLinkedSubstituentDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * WURCS2 conversion of bridge substituents.
 *
 * <p>A bridge between two monosaccharides ("inter") is written from the glycosidic linkage it sits
 * on, and always worked. A bridge within one monosaccharide ("intra" - 4,6-pyruvate, 1,6-anhydro,
 * ...) is a leaf hanging off a single residue, and used to be dropped by the exporter without any
 * error, so a drawn structure silently exported as a different, perfectly valid structure.
 *
 * <p>These tests pin the three failures that produced:
 * <ul>
 *   <li>an intra bridge vanishing from the output entirely,</li>
 *   <li>(S)/(R)/(X)-pyruvate all collapsing to the same stereo notation,</li>
 *   <li>the star indices of an inter bridge coming out swapped.</li>
 * </ul>
 */
public class BridgeSubstituentWURCSTest {

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	// ---------------------------------------------------------------- round trips

	/**
	 * An ether bridge has no chemistry of its own, so WURCS writes it as the two linkage positions
	 * alone. "*O*" is redundant and is not part of the canonical form.
	 */
	@Test
	public void anhydroBridgeIsWrittenAsBarePositions() throws Exception {
		assertRoundTrip("WURCS=2.0/1,1,0/[a2122h-1x_1-5_4-6]/1/");
	}

	/** From this project's own GlyContainer2GlycanTester: a 4,6-pyruvate alongside a 3-O-methyl. */
	@Test
	public void pyruvateBridgeSurvivesExport() throws Exception {
		assertRoundTrip("WURCS=2.0/3,3,2/[a2221m-1x_1-5][a2211m-1a_1-5]"
				+ "[a2122h-1b_1-5_4-6*OC^XO*/3CO/6=O/3C_3*OC]/1-2-3/a2-b1_b3-c1");
	}

	@Test
	public void interPhosphateBridgeIsUnchanged() throws Exception {
		assertRoundTrip("WURCS=2.0/1,2,1/[a2122h-1x_1-5]/1-1/a6-b1*OPO*/3O/3=O");
	}

	/** The "n2"/"n1" say which end of the MAP each side attaches to; they must not swap. */
	@Test
	public void interPhosphoethanolamineKeepsItsStarIndices() throws Exception {
		assertRoundTrip("WURCS=2.0/1,2,1/[a2122h-1x_1-5]/1-1/a6n2-b1n1*1NCCOP^XO*2/6O/6=O");
	}

	/** A structure with no bridge at all, as a guard on the surrounding conversion. */
	@Test
	public void nGlycanCoreIsUnchanged() throws Exception {
		assertRoundTrip("WURCS=2.0/3,5,4/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]"
				+ "/1-1-2-3-3/a4-b1_b4-c1_c3-d1_c6-e1");
	}

	// ---------------------------------------------------------------- built structures

	@Test
	public void anhydroBuiltOnACanvasExportsAsBarePositions() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1x_1-5_4-6]/1/", intraBridgeOnGlc("Anhydro"));
	}

	/**
	 * The three pyruvates share the glycoCT notation "pyruvate", so re-deriving their MAP from that
	 * name returned ^X for all of them (and the table's S entry carries ^R). Their MAP now comes
	 * from the cross-linked template, which keeps them apart.
	 */
	@Test
	public void pyruvateStereochemistryIsDistinct() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1x_1-5_4-6*OC^XO*/3CO/6=O/3C]/1/", intraBridgeOnGlc("Py"));
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1x_1-5_4-6*OC^SO*/3CO/6=O/3C]/1/", intraBridgeOnGlc("(S)Py"));
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1x_1-5_4-6*OC^RO*/3CO/6=O/3C]/1/", intraBridgeOnGlc("(R)Py"));
	}

	/** "Both" types are reachable as intra bridges too, and keep their own linking atoms. */
	@Test
	public void bothTypeBridgesKeepTheirLinkingAtoms() throws Exception {
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1x_1-5_4-6*N*]/1/", intraBridgeOnGlc("N"));
		assertEquals("WURCS=2.0/1,1,0/[a2122h-1x_1-5_4-6*OCCCCO*/6=O/3=O]/1/", intraBridgeOnGlc("Suc"));
	}

	/**
	 * Whatever each bridge type produces, it has to be WURCS the validator accepts and the importer
	 * can read back as the same bridge - the previous output could not be re-imported at all.
	 */
	@Test
	public void everyBridgeTypeExportsToReadableWurcs() throws Exception {
		List<String> broken = new ArrayList<String>();

		for (ResidueType type : CrossLinkedSubstituentDictionary.getCrossLinkedSubstituents()) {
			String name = type.getName();
			String wurcs = intraBridgeOnGlc(name);

			WURCSValidator validator = new WURCSValidator();
			validator.start(wurcs);
			WURCSValidationReport report = validator.getReport();
			if (report.hasError()) {
				broken.add(name + ": " + report.getErrors());
				continue;
			}

			Glycan reread = new WURCS2Parser().readGlycan(wurcs, new MassOptions());
			String reexported = new WURCS2Parser().writeGlycan(reread);
			if (!wurcs.equals(reexported))
				broken.add(name + ": " + wurcs + " -> " + reexported);
		}

		assertTrue("bridge types that do not survive a WURCS round trip: " + broken, broken.isEmpty());
	}

	// ---------------------------------------------------------------- helpers

	private void assertRoundTrip(String _wurcs) throws Exception {
		WURCS2Parser parser = new WURCS2Parser();
		Glycan glycan = parser.readGlycan(_wurcs, new MassOptions());
		assertEquals(_wurcs, parser.writeGlycan(glycan));

		WURCSValidator validator = new WURCSValidator();
		validator.start(_wurcs);
		assertFalse("validator rejected " + _wurcs, validator.getReport().hasError());
	}

	/** freeEnd - Glc, with the named bridge spanning positions 4 and 6 of that Glc. */
	private String intraBridgeOnGlc(String _bridgeName) throws Exception {
		Residue root = ResidueDictionary.createReducingEnd("freeEnd");
		Residue glc = ResidueDictionary.newResidue("Glc");
		root.addChild(glc);

		Residue bridge = new Residue(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituent(_bridgeName));
		List<Bond> bonds = new ArrayList<Bond>();
		bonds.add(new Bond('4', '1'));
		bonds.add(new Bond('6', '1'));
		bridge.setParentLinkage(new Linkage(glc, bridge, bonds));
		glc.addChild(bridge, bonds);

		return new WURCS2Parser().writeGlycan(new Glycan(root, false, new MassOptions()));
	}
}
