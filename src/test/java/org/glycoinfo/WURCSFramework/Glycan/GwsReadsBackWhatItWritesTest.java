package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.InputSource;

/**
 * What GlycanBuilder2 writes as GWS it reads back, and what it cannot read it names (#394).
 *
 * <p>A cycle was written with its ends as {@code >} and {@code <}, which nothing read: the root was
 * taken for an aglycon called "&gt;", and the end failed the parse. A repeat around nested repeats
 * was written whole and failed on reading, its outer end finding no start. Saved to a .gws file, any of
 * them failed the whole file with "Invalid format for string: &lt;", and held in a workspace, the
 * structure was dropped without a word.
 */
public class GwsReadsBackWhatItWritesTest {

	/** The five that could be saved and not opened again. */
	private static final String[] FIVE = {
			// a cyclodextrin
			"WURCS=2.0/1,7,7/[a2122h-1a_1-5_2*OC_3*OC_6*N]/1-1-1-1-1-1-1/a1-g4_a4-b1_b4-c1_c4-d1_d4-e1_e4-f1_f4-g1",
			// two residues joined by two bonds, read as a cycle
			"WURCS=2.0/2,2,2/[h4344h_2*NCC/3=O][a2112h-1b_1-5]/1-2/a4-b1_a6-b2",
			// a cycle around a repeat, and a cycle with repeats
			"WURCS=2.0/2,3,4/[a2122h-1a_1-5_2*N][a2122h-1a_1-5]/1-2-2/a1-c4_a4-b1_b4-c1_c1-c4~n",
			"WURCS=2.0/2,4,6/[a2122h-1a_1-5][a2122A-1a_1-5]/1-2-1-2/a1-d4_a4-b1_b4-c1_c4-d1_a1-a4~n_c1-c4~n",
			// a repeat around nested repeats
			"WURCS=2.0/4,13,17/[a122h-1x_1-4][a2112h-1x_1-4][a2112h-1x_1-5][a1122h-1x_1-5]/1-1-2-3-1-1-1-1-1-1-1-4-4/"
					+ "a5-b1_c5-d1_c6-l1_d4-e1_e5-f1_f5-j1_g2-h1_h5-i1_j5-k1_l2-m1_g1-f2|f3_b?-c1_a1-k5~n_e1-e5~n_h1-h5~n_j1-j5~n_b1-b?~n" };

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private BuilderWorkspace workspace;

	@Before
	public void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Each is read back from its own GWS as it was. */
	@Test
	public void eachIsReadBackFromItsGws() throws Exception {
		for (String wurcs : FIVE) {
			Glycan original = read(wurcs);
			List<Glycan> back = workspace.getStructures().parseString(original.toString());
			assertEquals(wurcs, 1, back.size());
			assertEquals(wurcs, new WURCS2Parser().toWURCS(original), new WURCS2Parser().toWURCS(back.get(0)));
			assertEquals(wurcs, original.computeMass(), back.get(0).computeMass(), 1e-6);
		}
	}

	/** Saved together to a .gws file, they open again, all five, as they were. */
	@Test
	public void aFileOfThemOpensAgain() throws Exception {
		List<Glycan> originals = new ArrayList<Glycan>();
		List<String> written = new ArrayList<String>();
		for (String wurcs : FIVE) {
			Glycan original = read(wurcs);
			originals.add(original);
			written.add(new WURCS2Parser().toWURCS(original));
		}
		File file = folder.newFile("five.gws");
		GlycanDocument saving = workspace.getStructures();
		saving.setStructures(originals);
		assertTrue(saving.save(file.getAbsolutePath()));

		GlycanDocument opening = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		assertTrue("the file did not open", opening.open(file, false, false));
		List<String> opened = new ArrayList<String>();
		for (Glycan structure : opening.getStructures())
			opened.add(new WURCS2Parser().toWURCS(structure));
		assertEquals(written, opened);
	}

	/** A structure that still cannot be read is named: its place and its beginning, with the reason. */
	@Test
	public void anUnreadableStructureIsNamed() throws Exception {
		String good = read("WURCS=2.0/2,2,1/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5]/1-2/a4-b1").toString();
		String bad = "freeEnd--?b1D-GlcNAc,p--4b1D-Man,p((--3a1D-Man,p$MONO,Und,0,0,freeEnd";
		try {
			workspace.getStructures().parseString(good + ";" + bad + ";" + good);
			fail("an unreadable structure was read");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(), refused.getMessage().startsWith("Structure 2 of 3 could not be read ("));
			assertTrue(refused.getMessage(), refused.getMessage().contains("freeEnd--?b1D-GlcNAc,p--4b1D-Man,p((--3a1D-Man"));
		}
	}

	/** In a workspace, it is no longer held as nothing: the structures are refused, naming it. */
	@Test
	public void aWorkspaceDoesNotDropIt() throws Exception {
		String good = read("WURCS=2.0/2,2,1/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5]/1-2/a4-b1").toString();
		String bad = "freeEnd--?b1D-GlcNAc,p--4b1D-Man,p((--3a1D-Man,p$MONO,Und,0,0,freeEnd";
		String xml = "<Structures><Glycan structure=\"" + good + "\"/><Glycan structure=\"" + bad + "\"/></Structures>";
		org.w3c.dom.Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		GlycanDocument document = new BuilderWorkspace(new GlycanRendererAWT()).getStructures();
		try {
			document.fromXML(dom.getDocumentElement(), false);
			fail("an unreadable structure was read");
		} catch (Exception refused) {
			assertTrue(refused.getMessage(), refused.getMessage().startsWith("Structure 2 of 2 could not be read ("));
		}
		for (Glycan held : document.getStructures())
			assertTrue("a structure held as nothing", held != null);
	}

	private Glycan read(String wurcs) throws Exception {
		return new WURCS2Parser().readGlycan(wurcs, workspace.getDefaultMassOptions());
	}
}
