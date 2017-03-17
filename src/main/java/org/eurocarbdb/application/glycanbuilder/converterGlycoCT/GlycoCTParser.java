/*
 *   EuroCarbDB, a framework for carbohydrate bioinformatics
 *
 *   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
 *   indicated by the @author tags or express copyright attribution
 *   statements applied by the authors.  
 *
 *   This copyrighted material is made available to anyone wishing to use, modify,
 *   copy, or redistribute it subject to the terms and conditions of the GNU
 *   Lesser General Public License, as published by the Free Software Foundation.
 *   A copy of this license accompanies this distribution in the file LICENSE.txt.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *   for more details.
 *
 *   Last commit: $Rev$ by $Author$ on $Date::             $  
 */

package org.eurocarbdb.application.glycanbuilder.converterGlycoCT;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;
import org.eurocarbdb.resourcesdb.*;
import org.eurocarbdb.resourcesdb.io.*;
import org.eurocarbdb.resourcesdb.glycoconjugate_derived.*;
import org.eurocarbdb.MolecularFramework.sugar.*;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.*;
import org.eurocarbdb.MolecularFramework.io.namespace.*;
import org.eurocarbdb.MolecularFramework.sugar.NonMonosaccharides.HistoricalEntity;

import java.awt.Rectangle;
import java.util.*;
import java.util.regex.*;

/**
 * Read and write glycan structure in the GlycoCT XML format using the
 * MolecularFramework and ResourceDB libraries.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GlycoCTParser implements GlycanParser {

	static private Pattern iupac_sub_pattern;
	static {
		iupac_sub_pattern = Pattern.compile("^(.+)(\\-?[1-9][a-zA-Z]+)$");
	}

	private SugarImporterGlycoCT importer = null;
	private SugarExporterGlycoCT exporter = null;

	private MonosaccharideConverter converter = null;
	private GlycoVisitorFromGlycoCT visitor_import = null;
	private GlycoVisitorToGlycoCT visitor_export = null;

	private boolean tolerate_unknown_residues = false;

	/**
	 * Default constructor. Initialize the MolecularFramework and ResourceDB
	 * objects.
	 */

	public GlycoCTParser(boolean tolerate) {

		try {
			tolerate_unknown_residues = tolerate;

			importer = new SugarImporterGlycoCT();
			exporter = new SugarExporterGlycoCT();

			converter = new MonosaccharideConverter(new Config());

			visitor_import = new GlycoVisitorFromGlycoCT(converter);
			// visitor_import.setNameScheme(GlycanNamescheme.GLYCOSCIENCES);
			visitor_import.setNameScheme(GlycanNamescheme.GWB);

			visitor_export = new GlycoVisitorToGlycoCT(converter);
			// visitor_export.setNameScheme(GlycanNamescheme.GLYCOSCIENCES);
			visitor_export.setNameScheme(GlycanNamescheme.GWB);
			visitor_export.setUseFusion(true);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	public void setTolerateUnknown(boolean f) {
		tolerate_unknown_residues = f;
	}

	public String writeGlycan(Glycan structure) {
		return toGlycoCT(structure);
	}

	public Glycan readGlycan(String buffer, MassOptions default_mass_options)
			throws Exception {
		return fromGlycoCT(buffer, default_mass_options);
	}

	/**
	 * Get the MolecularFramework object used to parse the GlycoCT string.
	 */
	public SugarImporterGlycoCT getImporter() {
		return importer;
	}

	/**
	 * Get the MolecularFramework object used to produce the GlycoCT string.
	 */
	public SugarExporterGlycoCT getExporter() {
		return exporter;
	}

	/**
	 * Get the ResourceDB object used to translate between the monosaccharide
	 * namespaces.
	 */
	public MonosaccharideConverter getConverter() {
		return converter;
	}

	/**
	 * Get the MolecularFramework object used to translate between a
	 * MolecularFramework and a GlycoWorkbench glycan structure.
	 */
	public GlycoVisitorFromGlycoCT getVisitorImport() {
		return visitor_import;
	}

	/**
	 * Get the MolecularFramework object used to translate between a
	 * GlycoWorkbench and a MolecularFramework glycan structure.
	 */
	public GlycoVisitorToGlycoCT getVisitorExport() {
		return visitor_export;
	}

	public String toGlycoCT(Glycan structure) {
		return toGlycoCT(structure,null);
	}
	
	/**
	 * Return a GlycoCT representation of a glycan structure. Equivalent to a
	 * call to {@link #writeGlycan}
	 */
	public String toGlycoCT(Glycan structure, BBoxManager manager) {
		try {
			exporter.start(toSugar(structure, manager));
			return exporter.getXMLCode();
		} catch (Exception e) {
			LogUtils.report(e);
			return "";
		}
	}

	/**
	 * Return a representation of a glycan structure as a MolecularFramework
	 * object.
	 * 
	 * @throws Exception
	 *             if the conversion cannot be made
	 */
	public Sugar toSugar(Glycan structure, BBoxManager manager) throws Exception {
		// init
		Sugar sugar = new Sugar();
		if (structure == null)
			return sugar;

		// create the sugar
		if (structure.isFragment())
			throw new Exception("fragments not supported for the moment");
		if (structure.getRoot() != null) {
			Residue root = structure.getRoot();
			if (!root.isSaccharide()) {
				if (root.getTypeName().equals("freeEnd"))
					toSugar(root.firstChild(), sugar, false, null, null, manager);
				else if (root.getTypeName().equals("redEnd"))
					toSugar(root.firstChild(), sugar, true, null, null, manager);
				else
					toSugar(root.firstChild(), sugar, false, null, null, manager);
			} else
				toSugar(root, sugar, false, null, null, manager);
		}

		// add antennae
		if (structure.getBracket() != null) {
			ArrayList<GlycoNode> parents = (ArrayList<GlycoNode>) sugar.getNodes().clone();
			for (Linkage link : structure.getBracket().getChildrenLinkages()) {
				Residue antenna = link.getChildResidue();

				// create subtree
				UnderdeterminedSubTree nm_antenna = new UnderdeterminedSubTree();
				toSugar(antenna, nm_antenna, false, null, null, manager);

				// set connection
				nm_antenna.setConnection(toSugar(link));

				// add subtree
				sugar.addUndeterminedSubTree(nm_antenna);
				for (GlycoNode n : parents)
					sugar.addUndeterminedSubTreeParent(nm_antenna, n);

			}
		}

		// Normalize the sugar
		visitor_export.start(sugar);
		return visitor_export.getNormalizedSugar();
	}

	private GlycoNode toSugar(Residue current, GlycoGraph nm_graph,
			boolean alditol, Residue stop_at, HashMap<Residue, GlycoNode> map, BBoxManager bboxManager)
			throws Exception {
		if (nm_graph == null)
			return null;
		if (current == stop_at)
			return null;

		// create new node
		Residue parent = null;
		GlycoNode nm_current = null;
		if (current.isStartRepetition()) {
			// create repeat unit
			nm_current = toSugarUnitRepeat(current, bboxManager);
			parent = current.findEndRepetition();
		} else {
			// translate current residue
			UnvalidatedGlycoNode toadd = new UnvalidatedGlycoNode();
			toadd.setName(getIupacName(current, alditol));
			
			//if
			if(bboxManager!=null && bboxManager.border_bboxes.containsKey(current)){
				//Konchan commented out toadd.setCenterPosition(bboxManager.border_bboxes.get(current));
			}
			
			nm_current = toadd;
			parent = current;
		}

		// add node
		nm_graph.addNode(nm_current);
		if (map != null)
			map.put(current, nm_current);

		// translate children
		for (Linkage link : parent.getChildrenLinkages()) {
			GlycoNode nm_child = toSugar(link.getChildResidue(), nm_graph, false, stop_at, map, bboxManager);
			
			if (nm_child != null)
				nm_graph.addEdge(nm_current, nm_child, toSugar(link));
		}

		return nm_current;
	}

	private SugarUnitRepeat toSugarUnitRepeat(Residue start, BBoxManager manager) throws Exception {
		// init
		SugarUnitRepeat unit = new SugarUnitRepeat();

		// add nodes
		Residue root = start.getChildAt(0);
		Residue end = start.findEndRepetition();
		HashMap<Residue, GlycoNode> map = new HashMap<Residue, GlycoNode>();

		toSugar(root, unit, false, end, map, manager);

		// set min and max
		unit.setMinRepeatCount(end.getMinRepetitions());
		unit.setMaxRepeatCount(end.getMaxRepetitions());

		// set linkage
		unit.setRepeatLinkage(toSugar(root.getParentLinkage()),
				map.get(end.getParent()), map.get(root));

		return unit;
	}

	/**
	 * Create a glycan structure from its GlycoCT representation. Equivalent to
	 * a call to {@link #readGlycan}.
	 * 
	 * @param default_mass_opt
	 *            the mass options to use for the new structure if they are not
	 *            specified in the string representation
	 * @throws Exception
	 *             if the string cannot be parsed
	 */
	public Glycan fromGlycoCT(String str, MassOptions default_mass_opt)
			throws Exception {
		return fromSugar(importer.parse(str), default_mass_opt);
	}

	/**
	 * Create a glycan structure from its representation as a MolecularFramework
	 * object.
	 * 
	 * @param default_mass_opt
	 *            the mass options to use for the new structure if they are not
	 *            specified in the string representation
	 * @throws Exception
	 *             if the string cannot be parsed
	 */
	public Glycan fromSugar(Sugar sugar, MassOptions default_mass_opt)
			throws Exception {
		return fromSugar(sugar, converter, visitor_import, default_mass_opt,
				tolerate_unknown_residues);
	}

	static public Glycan fromSugar(Sugar sugar,
			MonosaccharideConversion converter,
			GlycoVisitorFromGlycoCT visitor, MassOptions default_mass_opt,
			boolean tolerate_unknown_residues) throws Exception {

		if (sugar == null)
			return null;

		// remove protein
		if (sugar.getRootNodes().size() == 1) {
			GlycoNode gn_root = sugar.getRootNodes().iterator().next();
			if (gn_root instanceof HistoricalEntity)
				sugar.removeNode(gn_root);
		}

		// "Denormalize" the sugar
		visitor.start(sugar);
		sugar = visitor.getNormalizedSugar();

		// create the sugar
		if (sugar.getRootNodes().size() > 1)
			throw new Exception("Multiple roots are not currently supported");
		if (sugar.getRootNodes().size() == 0)
			return new Glycan(null, false, default_mass_opt);

		// parse from the root
		GlycoNode gn_root = sugar.getRootNodes().iterator().next();
		Residue root = fromSugar(gn_root, converter, tolerate_unknown_residues,
				null);

		if (root != null && !root.isReducingEnd()) {
			if (root.isAlditol()) {
				Residue redend = ResidueDictionary.newResidue("redEnd");
				redend.addChild(root);
				root = redend;
			} else {
				Residue redend = ResidueDictionary.newResidue("freeEnd");
				redend.addChild(root);
				root = redend;
			}
		}

		Glycan ret = new Glycan(root, false, default_mass_opt);

		// parse antennae
		for (UnderdeterminedSubTree antenna : sugar.getUndeterminedSubTrees()) {
			if (antenna.getRootNodes().size() > 1)
				throw new Exception(
						"Multiple roots in antenna are not currently supported");
			if (antenna.getRootNodes().size() == 0)
				continue;

			GlycoNode antenna_root = antenna.getRootNodes().iterator().next();
			Residue toadd = fromSugar(antenna_root, converter,
					tolerate_unknown_residues, null);
			Vector<Bond> bonds = fromSugar(antenna.getConnection());
			ret.addAntenna(toadd, bonds);
		}

		return ret;
	}

	static private Residue fromSugar(GlycoNode nm_current,
			MonosaccharideConversion converter,
			boolean tolerate_unknown_residues, HashMap<GlycoNode, Residue> map)
			throws Exception {
		if (nm_current == null)
			return null;

		Residue ret = null;
		Residue parent = null;
		if (nm_current instanceof SugarUnitRepeat) {
			ret = fromSugarUnitRepeat((SugarUnitRepeat) nm_current, converter, tolerate_unknown_residues);
			parent = ret.findEndRepetition();
			if (map != null)
				map.put(nm_current, ret);
		} else {
			String buf = "";
			// transform the node into a residue
			String iupac_name = ((UnvalidatedGlycoNode) nm_current).getName();
			Residue current = fromIupacName(iupac_name, converter, converter == null);
			
			if (current == null) {
				// try to remove the substitutions
				Vector<String> subs = new Vector<String>();
				iupac_name = removeSubstitutions(subs, iupac_name);
		
				// parse again
				current = fromIupacName(iupac_name, converter, tolerate_unknown_residues);
				if (current == null)
					throw new Exception("Unrecognized residue type: " + iupac_name + " " + tolerate_unknown_residues);

				// add substitutions
				addModifications(current, subs, converter, tolerate_unknown_residues);
			}

			ret = parent = current;
			
			if (map != null)
				map.put(nm_current, current);
		}

		// System.out.println(nm_current.getChildEdges().size() + " children");

		// parse the children
		for (GlycoEdge edge : nm_current.getChildEdges()) {
			Residue child = fromSugar(edge.getChild(), converter,
					tolerate_unknown_residues, map);

			Vector<Bond> bonds = fromSugar(edge);
			parent.addChild(child, bonds);
			child.setAnomericCarbon(bonds.lastElement().getChildPosition());
		}

		return ret;
	}

	static private Residue fromSugarUnitRepeat(SugarUnitRepeat unit,
			MonosaccharideConversion converter,
			boolean tolerate_unknown_residues) throws Exception {

		if (unit.getRootNodes().size() > 1)
			throw new Exception(
					"Multiple roots are not currently supported in repeat units");

		// convert nodes
		GlycoNode gn_root = unit.getRootNodes().iterator().next();
		HashMap<GlycoNode, Residue> map = new HashMap<GlycoNode, Residue>();
		Residue root = fromSugar(gn_root, converter, tolerate_unknown_residues, map);

		// add start repetition
		Residue start = ResidueDictionary.createStartRepetition();
		start.addChild(root, fromSugar(unit.getRepeatLinkage()));

		// add end repetition
		Residue end = ResidueDictionary.createEndRepetition();
		end.setMinRepetitions("" + unit.getMinRepeatCount());
		end.setMaxRepetitions("" + unit.getMaxRepeatCount());

		start.setEndRepitionResidue(end);

		// connect end repetition to the last residue of the repeating unit
		Residue last = map.get(unit.getRepeatLinkage().getParent());
		last.addChild(end);

		return start;
	}

	static private String removeSubstitutions(Vector<String> subs,
			String iupac_name) throws Exception {
		// System.out.println("removing substitutions from " + iupac_name);
		while (iupac_name.length() > 0) {
			Matcher m = iupac_sub_pattern.matcher(iupac_name);
			if (!m.matches())
				break;

			iupac_name = m.group(1);
			subs.add(m.group(2));
		}
		return iupac_name;
	}

	static private void addModifications(Residue current,
			Vector<String> children_iupac_names,
			MonosaccharideConversion converter,
			boolean tolerate_unknown_residues) throws Exception {
		for (String s : children_iupac_names) {
			char pos = s.charAt(0);
			String type = s.substring(1);
			Residue child = fromIupacName(type, converter,
					tolerate_unknown_residues);
			if (child == null)
				throw new Exception("Unrecognized residue type: " + type);
			current.addChild(child, pos);
		}
	}

	/**
	 * Get the IUPAC representation of the type of a residue.
	 * 
	 * @param current
	 *            the residue
	 * @param alditol
	 *            <code>true</code> if the residue represent an alditol
	 */
	public String getIupacName(Residue current, boolean alditol)
			throws Exception {
		if (!current.getType().hasIupacName())
			throw new Exception("Unsupported IUPAC name for type "
					+ current.getTypeName());

		return getIupacName(current.getType(), alditol,
				current.getAnomericCarbon(), current.getAnomericState(),
				current.getChirality(), current.getRingSize());
	}

	static private String getIupacName(ResidueType type, boolean alditol,
			char anomeric_carbon, char anomeric_state, char chirality,
			char ring_size) {
		String iupac_name = type.getIupacName();
		if (type.isSaccharide()) {
			// add chirality
			if (type.hasChirality())
				iupac_name = chirality + "-" + iupac_name;

			// get name
			if (alditol)
				return TextUtils.delete(iupac_name, '$') + "-ol";

			// add stereochemistry
			iupac_name = anomeric_state + "-" + iupac_name;

			// open ring
			if (ring_size == 'o') {
				if (anomeric_carbon == '2')
					return "keto-" + TextUtils.delete(iupac_name, '$');
				return "aldehydo-" + TextUtils.delete(iupac_name, '$');
			}

			// add ring size;
			if (ring_size == '?')
				return TextUtils.delete(iupac_name, '$');

			return iupac_name.replace('$', ring_size);
		}
		return iupac_name;
	}

	static private Residue fromIupacName(String iupac_name,
			MonosaccharideConversion converter,
			boolean tolerate_unknown_residues) throws Exception {

		if (converter == null) {
			if (tolerate_unknown_residues)
				return new Residue(ResidueType.createSaccharide(iupac_name));
			else
				throw new Exception("Cannot convert iupac name to residue");
		}

		EcdbMonosaccharide type = null;
		try {
			MonosaccharideExchangeObject data = converter.convertResidue(
					iupac_name, GlycanNamescheme.GWB, GlycanNamescheme.GLYCOCT);
			type = data.getBasetype();
		} catch (Exception e) {
			if (tolerate_unknown_residues)
				return new Residue(ResidueType.createSaccharide(iupac_name));
			else
				return null;
		}

		if (type != null) {
			// saccharide

			// get anomeric state
			char anomeric_state = type.getAnomer().getSymbol().charAt(0);
			if (anomeric_state == 'x')
				anomeric_state = '?';

			// get ring size
			char ring_size = '?';
			if ((type.getRingEnd() - type.getRingStart()) == 4)
				ring_size = 'p';
			else if ((type.getRingEnd() - type.getRingStart()) == 3)
				ring_size = 'f';
			else if (type.getRingStart() == EcdbMonosaccharide.OPEN_CHAIN) {
				ring_size = 'o';
				anomeric_state = '?';
			}

			// get chirality
			char chirality = '?';
			if (type.getBaseTypeCount() > 0) {
				chirality = Character.toUpperCase(type.getBaseType(0).getName()
						.charAt(0));
				if (chirality == 'X')
					chirality = '?';
			}

			// find type
			for (ResidueType t : ResidueDictionary.allResidues()) {
				if (getIupacName(t, false, t.getAnomericCarbon(),
						anomeric_state, chirality, ring_size)
						.compareToIgnoreCase(iupac_name) == 0) {
					Residue ret = new Residue(t);
					ret.setAnomericState(anomeric_state);
					ret.setChirality(chirality);
					ret.setRingSize(ring_size);
					ret.setAlditol(false);
					return ret;
				} else if (getIupacName(t, true, t.getAnomericCarbon(),
						anomeric_state, chirality, ring_size)
						.compareToIgnoreCase(iupac_name) == 0) {
					Residue ret = new Residue(t);
					ret.setAnomericState(anomeric_state);
					ret.setChirality(chirality);
					ret.setRingSize(ring_size);
					ret.setAlditol(true);
					return ret;
				}
			}
			if (tolerate_unknown_residues) {
				Residue ret = new Residue(
						ResidueType.createSaccharide(iupac_name));
				ret.setAnomericState(anomeric_state);
				ret.setChirality(chirality);
				ret.setRingSize(ring_size);
				ret.setAlditol(true);
				return ret;
			}
		} else {
			// substituent
			for (ResidueType t : ResidueDictionary.allResidues()) {
				if (getIupacName(t, false, '?', '?', '?', '?')
						.compareToIgnoreCase(iupac_name) == 0)
					return new Residue(t);
			}

			if (tolerate_unknown_residues)
				return new Residue(ResidueType.createSubstituent(iupac_name));
		}

		return null;
	}

	private GlycoEdge toSugar(Linkage link) throws Exception {

		GlycoEdge nm_edge = new GlycoEdge();
		for (Bond b : link.getBonds()) {
			org.eurocarbdb.MolecularFramework.sugar.Linkage nm_link = new org.eurocarbdb.MolecularFramework.sugar.Linkage();

			char[] p_poss = b.getParentPositions();
			for (int i = 0; i < p_poss.length; i++)
				nm_link.addParentLinkage(toIntPosition(p_poss[i]));
			nm_link.addChildLinkage(toIntPosition(b.getChildPosition()));

			nm_edge.addGlycosidicLinkage(nm_link);
		}

		return nm_edge;
	}

	private static Vector<Bond> fromSugar(GlycoEdge nm_edge) {
		if (nm_edge.getGlycosidicLinkages().size() == 0)
			return Bond.single();

		Vector<Bond> ret = new Vector<Bond>();
		for (org.eurocarbdb.MolecularFramework.sugar.Linkage nm_link : nm_edge
				.getGlycosidicLinkages()) {

			// get parent linkages
			ArrayList<Integer> nm_ppos = nm_link.getParentLinkages();
			char[] p_poss = new char[nm_ppos.size()];
			for (int i = 0; i < nm_ppos.size(); i++)
				p_poss[i] = fromIntPosition(nm_ppos.get(i));

			// get child linkage
			char c_pos = (nm_link.getChildLinkages().size() == 1) ? fromIntPosition(nm_link
					.getChildLinkages().get(0)) : '?';

			// create bond
			ret.add(new Bond(p_poss, c_pos));
		}
		return ret;
	}

	static private char fromIntPosition(int pos) {
		if (pos == org.eurocarbdb.MolecularFramework.sugar.Linkage.UNKNOWN_POSITION)
			return '?';
		else
			return (char) (pos + '0');
	}

	static private int toIntPosition(char pos) {
		if (pos == 'N')
			return 2;
		if (pos == '?' || pos == 'N')
			return org.eurocarbdb.MolecularFramework.sugar.Linkage.UNKNOWN_POSITION;
		return (int) (pos - '0');
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		return toGlycoCT(structure, bboxManager);
	}

	public Sugar toSugar(Glycan glycan) throws Exception {
		return toSugar(glycan, null);
	}
}
