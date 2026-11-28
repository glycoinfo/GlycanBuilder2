package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.CompositionOptions;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.renderutil.PositionManager;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.junit.Before;
import org.junit.Test;

/**
 * A composition is drawn in one order, however it was made (#235).
 *
 * <p>Its members were laid out in the order they were attached. Hex3HexNAc2 built in the composition
 * dialog was drawn with its HexNAc above its Hex, and the same composition written to WURCS and read
 * back - WURCS lists HexNAc before Hex - with its Hex above its HexNAc.
 */
public class CompositionIsDrawnInOneOrderTest {

	private BuilderWorkspace workspace;

	@Before
	public void loadDictionaries() {
		workspace = new BuilderWorkspace(new GlycanRendererAWT());
		workspace.setAutoSave(false);
	}

	/** Built in the dialog, and read back from the WURCS it writes: the same order, top to bottom. */
	@Test
	public void builtAndReadBackAreDrawnAlike() throws Exception {
		CompositionOptions counts = new CompositionOptions();
		counts.HEX = 3;
		counts.HEXNAC = 2;
		counts.NEU5AC = 1;
		counts.DHEX = 1;
		Glycan built = counts.getCompositionAsGlycan(neutral());
		Glycan readBack = read(new WURCS2Parser().toWURCS(built));

		assertEquals(drawnTopToBottom(built), drawnTopToBottom(readBack));
	}

	/** The members a WURCS gives in another order are drawn as the dialog orders them. */
	@Test
	public void theDialogsOrderIsTheOrder() throws Exception {
		Glycan readBack = read("WURCS=2.0/2,5,0/[axxxxh-1x_1-5_2*NCC/3=O][axxxxh-1x_1-5]/1-1-2-2-2/");
		List<String> drawn = drawnTopToBottom(readBack);
		List<String> dialog = drawnTopToBottom(composition("HEX", 3, "HEXNAC", 2));
		assertEquals(dialog, drawn);
	}

	/** The order the drawing follows is the order the dialog adds its residues in. */
	@Test
	public void theOrderIsTheDialogs() throws Exception {
		CompositionOptions every = new CompositionOptions();
		for (Field field : CompositionOptions.class.getFields())
			if (field.getType() == int.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
				field.setInt(every, 1);
		List<String> added = new ArrayList<String>();
		for (Linkage link : every.getCompositionAsGlycan(neutral()).getBracket().getChildrenLinkages()) {
			String name = link.getChildResidue().getTypeName();
			if (!name.startsWith("Or")) added.add(name);
		}
		assertEquals(CompositionOptions.MEMBER_ORDER, added);
	}

	/** The type names of a composition's members, from the top of the drawing down. */
	private static List<String> drawnTopToBottom(Glycan composition) throws Exception {
		final BBoxManager boxes = new BBoxManager();
		new GlycanRendererAWT().computeBoundingBoxes(Collections.singletonList(composition), false, false,
				new PositionManager(), boxes);
		List<Residue> members = new ArrayList<Residue>();
		for (Linkage link : composition.getBracket().getChildrenLinkages())
			if (boxes.getCurrent(link.getChildResidue()) != null) members.add(link.getChildResidue());
		Collections.sort(members, new Comparator<Residue>() {
			@Override
			public int compare(Residue a, Residue b) {
				return Integer.compare(boxes.getCurrent(a).y, boxes.getCurrent(b).y);
			}
		});
		List<String> names = new ArrayList<String>();
		for (Residue member : members)
			if (names.isEmpty() || !names.get(names.size() - 1).equals(member.getTypeName()))
				names.add(member.getTypeName());
		return names;
	}

	private Glycan read(String wurcs) throws Exception {
		GlycanDocument document = workspace.getStructures();
		document.setStructures(new ArrayList<Glycan>());
		document.importFromString(wurcs, "wurcs2");
		return document.getStructures().get(0);
	}

	private static Glycan composition(Object... fieldsAndCounts) throws Exception {
		CompositionOptions counts = new CompositionOptions();
		for (int i = 0; i < fieldsAndCounts.length; i += 2)
			CompositionOptions.class.getField((String) fieldsAndCounts[i]).setInt(counts, (Integer) fieldsAndCounts[i + 1]);
		return counts.getCompositionAsGlycan(neutral());
	}

	private static MassOptions neutral() {
		MassOptions neutral = new MassOptions();
		neutral.DERIVATIZATION = MassOptions.NO_DERIVATIZATION;
		neutral.ION_CLOUD = new IonCloud();
		return neutral;
	}
}
