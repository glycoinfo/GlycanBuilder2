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

package org.eurocarbdb.application.glycanbuilder;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParserFactory;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTCondensedParser;
import org.eurocarbdb.application.glycanbuilder.converterGlycoCT.GlycoCTParser;
import org.eurocarbdb.application.glycanbuilder.converterGWS.GWSParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.fileutil.ExtensionFileFilter;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.SAXUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.helpers.AttributesImpl;

/**
 * Document object containing a set of glycan structures.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class GlycanDocument extends BaseDocument implements SAXUtils.SAXWriter {


	// workspace
	private BaseWorkspace theWorkspace = null;

	// glycan structure
	private LinkedList<Glycan> structures = new LinkedList<Glycan>();
	private LinkedList<String> lst_encode = new LinkedList<String>();

	// ----------------

	/**
	 * Default constructor.
	 *
	 * @param workspace the workspace from which to derive the mass settings for new
	 *                  structures
	 */

	public GlycanDocument(BaseWorkspace workspace) {
		super();

		theWorkspace = workspace;
	}

	// ---------------- DATA ACCESS -----------------

	public LinkedList<String> getString() {
		return this.lst_encode;
	}

	public void clearString() {
		this.lst_encode.clear();
	}

	public String getName() {
		return "Structures";
	}

	public javax.swing.ImageIcon getIcon() {
		return FileUtils.themeManager.getImageIcon("glycandoc");
	}

	public Collection<javax.swing.filechooser.FileFilter> getFileFormats() {
		LinkedList<javax.swing.filechooser.FileFilter> filters = new LinkedList<javax.swing.filechooser.FileFilter>();

		filters.addLast(new ExtensionFileFilter("gws",
				"GlycoWorkbench structure file"));

		return filters;
	}

	public javax.swing.filechooser.FileFilter getAllFileFormats() {
		return new ExtensionFileFilter("gws", "Structure files");
	}

	public boolean isEmpty() {
		return structures.size() == 0;
	}

	/**
	 * Return the number of structures in the document.
	 */
	public int size() {
		return structures.size();
	}

	/**
	 * Return the number of structures in the document.
	 */
	public int getNoStructures() {
		return structures.size();
	}

	/**
	 * Return the first structure in the list or <code>null</code> if empty.
	 */
	public Glycan getFirstStructure() {
		return (structures.isEmpty() ? null : structures.getFirst());
	}

	/**
	 * Return the last structure in the list or <code>null</code> if empty.
	 */
	public Glycan getLastStructure() {
		return (structures.isEmpty() ? null : structures.getLast());
	}

	/**
	 * Return the structure at position <code>ind</code> in the list.
	 */
	public Glycan getStructure(int ind) {
		return structures.get(ind);
	}

	/**
	 * Return the position of structure in the list.
	 */
	public int indexOf(Glycan structure) {
		return structures.indexOf(structure);
	}

	/**
	 * Return the list of structures.
	 */
	public LinkedList<Glycan> getStructures() {
		return structures;
	}

	/**
	 * Return the set of structures with specified indexes.
	 */
	public LinkedList<Glycan> getStructures(int[] inds) {
		LinkedList<Glycan> ret = new LinkedList<Glycan>();
		for (int i = 0; i < inds.length; i++)
			ret.add(structures.get(inds[i]));
		return ret;
	}

	/**
	 * Return an iterator over the structures.
	 */
	public Iterator<Glycan> iterator() {
		return structures.iterator();
	}

	/**
	 * Return the structure containing the specified residue or
	 * <code>null</code> if there are no matches.
	 */
	public Glycan findStructureWith(Residue node) {
		if (node == null)
			return null;
		for (Iterator<Glycan> i = structures.iterator(); i.hasNext(); ) {
			Glycan structure = i.next();
			if (structure.contains(node))
				return structure;
		}
		return null;
	}

	/**
	 * Return <code>true</code> if <code>node</code> is in a glycan structure
	 * representing a composition.
	 */
	public boolean isInComposition(Residue node) {
		Glycan s = findStructureWith(node);
		return (s != null && s.isComposition());
	}

	/**
	 * Return the list structure containing the specified residues and linkages.
	 */
	public Collection<Glycan> findStructuresWith(Collection<Residue> nodes,
												 Collection<Linkage> links) {

		LinkedList<Glycan> ret = new LinkedList<Glycan>();
		if (nodes != null) {
			for (Residue r : nodes) {
				Glycan toadd = findStructureWith(r);
				if (!ret.contains(toadd)) ret.addLast(toadd);
			}
		}
		if (links != null) {
			for (Linkage l : links) {
				Glycan toadd = findStructureWith(l.getChildResidue());
				if (!ret.contains(toadd)) ret.addLast(toadd);
			}
		}
		return ret;
	}

	/**
	 * Return <code>true</code> if at least one structure contains the residue.
	 */
	public boolean contains(Residue node) {
		return (findStructureWith(node) != null);
	}

	// ------------------
	// document operations

	/**
	 * Order the structures by mass-to-charge ratio.
	 */
	public void orderStructures(boolean descending) {
		if (structures.size() <= 1)
			return;

		// create map
		TreeMap<Double, LinkedList<Glycan>> sorted_structures = new TreeMap<Double, LinkedList<Glycan>>();
		if (descending)
			sorted_structures = new TreeMap<Double, LinkedList<Glycan>>(
					new DescendingDoubleComparator());

		// sort glycans
		for (Glycan s : structures) {
			double mz = s.computeMZ();

			LinkedList<Glycan> vec = sorted_structures.get(mz);
			if (vec == null) {
				vec = new LinkedList<Glycan>();
				sorted_structures.put(mz, vec);
			}

			vec.addLast(s);
		}

		// change document
		structures.clear();
		for (LinkedList<Glycan> vec : sorted_structures.values())
			structures.addAll(vec);
		fireDocumentChanged();
	}

	/**
	 * Reset the document to contain only this structure.
	 */
	public void setStructure(Glycan _structure) {
		setStructure(_structure, true);
	}

	private void setStructure(Glycan _structure, boolean fire) {
		structures.clear();
		addStructure(_structure, false);
		if (fire) fireDocumentChanged();
	}

	/**
	 * Reset the document to contain only this list of structure.
	 */
	public void setStructures(Collection<Glycan> _structures) {
		setStructures(_structures, true);
	}

	private void setStructures(Collection<Glycan> _structures, boolean fire) {
		structures.clear();
		addStructures(_structures, false);
		if (fire) fireDocumentChanged();
	}

	/**
	 * Add a new glycan structure creating it from a residue tree.
	 */
	public Residue addStructure(Residue root) {
		return addStructure(root, true);
	}

	private Residue addStructure(Residue root, boolean fire) {
		if (root == null) return null;
		if (root.isReducingEnd() && !root.hasChildren()) return null;

		// add a structure
		Glycan new_structure = new Glycan(root, true,
				theWorkspace.getDefaultMassOptions());
		structures.add(new_structure);

		// update views
		if (fire) fireDocumentChanged();
		return new_structure.getRoot();
	}

	/**
	 * Add a new glycan structure.
	 */
	public void addStructure(Glycan _structure) {
		addStructure(_structure, true);
	}

	private void addStructure(Glycan _structure, boolean fire) {
		if (_structure != null && _structure.getRoot() != null) {
			structures.add(_structure/*.clone(true)*/);
			if (fire) fireDocumentChanged();
		}
	}

	/**
	 * Add a list of glycan structures.
	 */
	public void addStructures(Collection<Glycan> _structures) {
		addStructures(_structures, true);
	}

	private void addStructures(Collection<Glycan> _structures, boolean fire) {
		if (_structures != null && _structures.size() > 0) {
			for (Glycan _structure : _structures) {
				addStructure(_structure, false);
			}
			if (fire) fireDocumentChanged();
		}
	}

	/**
	 * Remove the structure at the specified position.
	 */
	public void removeStructure(int ind) {
		removeStructure(ind, true);
	}

	public void removeStructure(int ind, boolean fire) {
		structures.remove(ind);
		if (fire) fireDocumentChanged();
	}

	/**
	 * Remove the structures at the specified positions.
	 */
	public void removeStructures(int[] inds) {
		removeStructures(inds, true);
	}

	public void removeStructures(int[] inds, boolean fire) {

		boolean removed = false;

		LinkedList<Glycan> toremove = getStructures(inds);
		for (Iterator<Glycan> i = structures.iterator(); i.hasNext(); ) {
			if (toremove.contains(i.next())) {
				i.remove();
				removed = true;
			}
		}

		if (removed && fire)
			fireDocumentChanged();
	}

	/**
	 * Set the mass settings for the specified structures.
	 */
	public boolean setMassOptions(Collection<Glycan> structures,
								  MassOptions common_options) {
		// set options for all structures
		boolean changed = false;
		for (Glycan structure : structures)
			changed = changed | structure.setMassOptions(common_options);

		if (changed) {
			fireDocumentChanged();
		}
		return changed;
	}

	// --------------
	// structure operations

	/**
	 * Add <code>toadd</code> to the <code>current</code> residue.
	 *
	 * @see Residue#addChild
	 */
	public Residue addResidue(Residue current, Residue toadd) {
		if (toadd == null || isInComposition(current))
			return null;

		// add a structure
		if (isEmpty() || current == null || current.isReducingEnd())
			return addStructure(toadd);

		// append node to current selection
		if (!current.addChild(toadd))
			return null;

		// update views
		fireDocumentChanged();
		return toadd;
	}

	/**
	 * Add <code>toadd</code> to the <code>current</code> residue and to all the
	 * residues that are drawn at the same position.
	 *
	 * @see Residue#addChild
	 * @see GlycanRendererAWT
	 * @see BBoxManager
	 */
	public Residue addResidue(Residue current, ArrayList<Residue> linked, Residue toadd) {
		if (toadd == null || isInComposition(current))
			return null;

		// add a structure
		if (isEmpty() || current == null || current.isReducingEnd()) {
			return addStructure(toadd);
		}

		// append node to current selection
		if (!current.addChild(toadd))
			return null;

		// do the same with linked residues
		if (linked != null) {
			for (Residue r : linked)
				r.addChild(toadd.cloneSubtree());
		}

		if (current.isSaccharide() || current.isBracket()) {
			if (toadd.isSaccharide()) {
				toadd = modifyLinkageType(toadd, LinkageType.DEOXY, LinkageType.H_AT_OH);
			}
			if (toadd.isSubstituent()) {
				LinkageType lTypeOnChild = getSubstituentLinkageType(toadd);
				toadd = modifyLinkageType(toadd, LinkageType.NONMONOSACCHARID, lTypeOnChild);
			}
		}

		// update views
		fireDocumentChanged();
		return toadd;
	}

	private LinkageType getSubstituentLinkageType(Residue toadd) {
		switch(toadd.getType().getCompositionClass()) {
			case "O-type":
			case "Organic":
				return LinkageType.H_AT_OH;
		}
		switch(toadd.getType().getName()) {
			case "P":
			case "S":
				return LinkageType.H_AT_OH;
		}
		return LinkageType.DEOXY;
	}

	public Residue modifyLinkageType(Residue _toAdd, LinkageType _donorSide, LinkageType _acceptorSide) {
		try {
			_toAdd.getParentLinkage().setParentLinkageType(_acceptorSide);
			_toAdd.getParentLinkage().setChildLinkageType(_donorSide);
		} catch (Exception e) {

		}
		return _toAdd;
	}

	/**
	 * Insert <code>toinsert</code> between the <code>current</code> residue and
	 * its parent.
	 *
	 * @see Residue#addChild
	 */
	public Residue insertResidueBefore(Residue current, Residue toinsert) {
		if (toinsert == null || current == null || current.getParent() == null
				|| isInComposition(current))
			return null;

		// insert node before current selection
		if (!current.insertParent(toinsert))
			return null;

		// update views
		fireDocumentChanged();
		return toinsert;
	}

	/**
	 * Insert <code>toinsert</code> between the <code>current</code> residue and
	 * its parent. Do the same to all the residues that are drawn at the same
	 * position.
	 *
	 * @see Residue#addChild
	 * @see GlycanRendererAWT
	 * @see BBoxManager
	 */
	public Residue insertResidueBefore(Residue current, ArrayList<Residue> linked,
									   Residue toinsert) {
		if (toinsert == null || current == null || current.getParent() == null
				|| isInComposition(current))
			return null;

		// insert node before current selection
		if (!current.insertParent(toinsert))
			return null;

		// do the same with linked residues
		if (linked != null) {
			for (Residue r : linked)
				r.insertParent(toinsert.cloneResidue());
		}

		// update views
		fireDocumentChanged();
		return toinsert;
	}

	/**
	 * Add a bracket residue to the structure containing <code>current</code>
	 *
	 * @see Glycan#addBracket
	 */
	public Residue addBracket(Residue current) {

		Glycan structure = findStructureWith(current);
		if (structure != null && !structure.isComposition()) {
			Residue bracket = structure.addBracket();
			if (bracket != null) {
				fireDocumentChanged();
				return bracket;
			}
		}
		return null;
	}

	/**
	 * Change the residue type of <code>current</code>
	 *
	 * @see Residue#setType
	 */

	public boolean changeResidueType(Residue current, ResidueType new_type) {
		if (current == null
				|| (current.hasParent() && !new_type.canHaveParent())
				|| (!current.hasParent() && !new_type.canBeReducingEnd())
				|| (current.hasChildren() && !new_type.canHaveChildren())
				|| isInComposition(current))
			return false;

		current.setType(new_type);
		fireDocumentChanged();
		return true;
	}

	/**
	 * Change the residue type of <code>current</code> and of all the residues
	 * that are drawn at the same position.
	 *
	 * @see Residue#setType
	 * @see GlycanRendererAWT
	 * @see BBoxManager
	 */
	public boolean changeResidueType(Residue current, ArrayList<Residue> linked,
									 ResidueType new_type) {
		if (current == null
				|| (current.hasParent() && !new_type.canHaveParent())
				|| (!current.hasParent() && !new_type.canBeReducingEnd())
				|| (current.hasChildren() && !new_type.canHaveChildren())
				|| isInComposition(current))
			return false;

		// change type
		current.setType(new_type);

		// do the same with linked residues
		if (linked != null) {
			for (Residue r : linked)
				r.setType(new_type);
		}

		fireDocumentChanged();
		return true;
	}

	private boolean changeReducingEndTypePVT(Glycan structure,
											 ResidueType new_type) {
		if (structure != null)
			return structure.setReducingEndType(new_type);
		return false;
	}

	/**
	 * Change the reducing end marker for all the specified structures.
	 *
	 * @see Glycan#setReducingEndType
	 */
	public boolean changeReducingEndType(Collection<Glycan> structures,
										 ResidueType new_type) {
		boolean changed = false;
		for (Glycan s : structures)
			changed |= changeReducingEndTypePVT(s, new_type);
		if (changed)
			fireDocumentChanged();
		return changed;
	}

	/**
	 * Change the reducing end marker for the structure containing
	 * <code>current</code>
	 *
	 * @see Glycan#setReducingEndType
	 */
	public boolean changeReducingEndType(Residue current, ResidueType new_type) {
		Glycan structure = findStructureWith(current);
		if (changeReducingEndTypePVT(structure, new_type)) {
			fireDocumentChanged();
			return true;
		}
		return false;
	}

	private boolean addStructuresPVT(Residue current, Collection<Glycan> toadd) {
		if (toadd == null || toadd.isEmpty())
			return false;

		if (isEmpty() || current == null || current.isReducingEnd()) {
			// add new structures
			addStructures(toadd, false);
		} else {
			// append roots as child of the current selection
			for (Iterator<Glycan> i = toadd.iterator(); i.hasNext(); )
				current.addChild(i.next().getRoot());

			// find non-overlapping set of antennae
			LinkedList<Residue> brackets = new LinkedList<Residue>();
			for (Iterator<Glycan> i = toadd.iterator(); i.hasNext(); ) {
				Glycan structure = i.next();
				if (structure.getBracket() != null) {
					boolean found = false;
					for (Iterator<Residue> l = brackets.iterator(); l.hasNext(); ) {
						if (structure.getBracket().subtreeEquals(l.next()))
							found = true;
					}
					if (!found)
						brackets.add(structure.getBracket());
				}
			}

			// add antennae to current structure
			Glycan cur_structure = findStructureWith(current);
			for (Residue b : brackets) {
				for (Linkage link : b.getChildrenLinkages())
					cur_structure.addAntenna(link.getChildResidue(), link.getBonds());
			}
		}
		return true;
	}

	/**
	 * Merge the specified structures with the glycan object containing
	 * <code>current</code>. The root of each structure is added as a child of
	 * <code>current</code>, while the uncertain antennae are added to the
	 * bracket.
	 *
	 * @see Residue#addChild
	 * @see Glycan#addAntenna
	 */
	public void addStructures(Residue current, Collection<Glycan> toadd) {
		if (canAddStructures(current, toadd)) {
			if (addStructuresPVT(current, toadd))
				fireDocumentChanged();
		}
	}

	/**
	 * Return <code>true</code> if the structure formed by the residues in
	 * <code>toadd</code> can be merged with the structure containg
	 * <code>current</code>.
	 *
	 * @see #addStructures
	 * @see #extractView
	 */
	public boolean canAddStructures(Residue current, HashSet<Residue> toadd) {
		return canAddStructures(current, extractView(toadd));
	}

	/**
	 * Return <code>true</code> if the structures can be merged with the glycan
	 * object containing <code>current</code>.
	 *
	 * @see #addStructures
	 */
	public boolean canAddStructures(Residue current, Collection<Glycan> toadd) {
		if (current == null)
			return true;
		if (isInComposition(current))
			return false;

		if (current.isAntenna() || current.isBracket()) {
			for (Glycan s : toadd) {
				if (s.isFuzzy())
					return false;
			}
		}

		if (current.isInRepetition()) {
			for (Glycan s : toadd) {
				if (s.hasRepetition())
					return false;
			}
		}

		return true;
	}

	/**
	 * Merge the structures formed by the residues in <code>tocopy</code> with
	 * the glycan object containing <code>current</code>.
	 *
	 * @see #addStructures
	 * @see #extractView
	 */
	public void copyResidues(Residue current, HashSet<Residue> tocopy) {
		// copy structures
		LinkedList<Glycan> cloned_structures = extractView(tocopy);

		// paste structures
		if (canAddStructures(current, cloned_structures)
				&& addStructuresPVT(current, cloned_structures))
			fireDocumentChanged();
	}

	/**
	 * Merge the structures formed by the residues in <code>tocopy</code> with
	 * the glycan object containing <code>current</code>. Do the same with all
	 * linked residues.
	 *
	 * @see #addStructures
	 * @see #extractView
	 */
	public void copyResidues(Residue current, ArrayList<Residue> linked,
							 HashSet<Residue> tocopy) {
		// copy structures
		LinkedList<Glycan> cloned_structures = extractView(tocopy);

		// paste structures
		if (canAddStructures(current, cloned_structures)
				&& addStructuresPVT(current, cloned_structures)) {
			if (linked != null) {
				for (Residue r : linked)
					addStructuresPVT(r, extractView(tocopy));
			}
			fireDocumentChanged();
		}
	}

	/**
	 * Merge the structures formed by the residues in <code>tocopy</code> with
	 * the glycan object containing <code>current</code>. Remove the residues
	 * from their containing structures after that.
	 *
	 * @see #addStructures
	 * @see #extractView
	 */
	public void moveResidues(Residue current, HashSet<Residue> tomove) {
		// copy structures
		LinkedList<Glycan> cloned_structures = extractView(tomove);

		// paste structures
		if (canAddStructures(current, cloned_structures)
				&& addStructuresPVT(current, cloned_structures)) {
			// remove residues
			removeResiduesPVT(tomove);

			fireDocumentChanged();
		}
	}

	/**
	 * Merge the structures formed by the residues in <code>tocopy</code> with
	 * the glycan object containing <code>current</code>. Do the same for all
	 * linked residues. Remove the residues from their containing structures
	 * after that.
	 *
	 * @see #addStructures
	 * @see #extractView
	 */
	public void moveResidues(Residue current, ArrayList<Residue> linked,
							 HashSet<Residue> tomove) {
		// copy structures
		LinkedList<Glycan> cloned_structures = extractView(tomove);

		// paste structures
		if (canAddStructures(current, cloned_structures)
				&& addStructuresPVT(current, cloned_structures)) {
			if (linked != null) {
				for (Residue r : linked) addStructuresPVT(r, extractView(tomove));
			}

			// remove residues
			removeResiduesPVT(tomove);

			fireDocumentChanged();
		}
	}

	private boolean removeResiduePVT(Residue toremove) {
		if (toremove == null) return false;

		for (Glycan a_objGlycan : this.structures) {
			if (a_objGlycan.removeResidue(toremove)) {
				if (a_objGlycan.isEmpty()) {
					this.structures.remove(this.structures.indexOf(a_objGlycan));
				} else {
					for (Glycan obj_tmp : a_objGlycan.splitMultipleRoots()) {
						this.structures.addLast(obj_tmp);
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove a residue from is containing structure.
	 */
	public boolean removeResidue(Residue toremove) {
		if (removeResiduePVT(toremove)) {
			fireDocumentChanged();
			return true;
		}
		return false;
	}

	private boolean removeResiduesPVT(Collection<Residue> toremove) {
		if (toremove == null) return false;

		boolean removed = false;
		for (Glycan elm_Glycan : (LinkedList<Glycan>) this.structures.clone()) {
			int index = this.structures.indexOf(elm_Glycan);
			if (elm_Glycan.removeResidues(toremove)) {
				if (elm_Glycan.isEmpty()) {
					this.structures.remove(index);
				} else {
					for (Glycan obj_tmp : elm_Glycan.splitMultipleRoots())
						this.structures.addLast(obj_tmp);
				}
				removed = true;
			}
		}
		return removed;
	}

	/**
	 * Remove the residues from their containing structure.
	 */
	public void removeResidues(Collection<Residue> toremove) {
		if (removeResiduesPVT(toremove))
			fireDocumentChanged();
	}

	protected boolean swap(Residue node1, Residue node2) {
		if (node1 == null || node2 == null)
			return false;
		if (node1.getParent() != node2.getParent())
			return false;

		Residue parent = node1.getParent();
		if (parent.swapChildren(node1, node2)) {
			fireDocumentChanged();
			return true;
		}
		return false;
	}

	/**
	 * Create a repeat block containing the selected nodes.
	 *
	 * @param selected_last the last residue in the repeat block
	 * @param nodes         the residues in the repeat block
	 * @throws Exception if the reapeat unit cannot be created
	 */
	public boolean createRepetition(Residue selected_last,
									Collection<Residue> nodes) throws Exception {
		if (nodes == null || nodes.size() == 0)
			return false;
		// select first and last residue of the repetition,
		// check if the selected residues form a connected component and contain
		// no repetitions

		Residue first = null;
		Residue last = selected_last;
		for (Residue r : nodes) {
			// check content of the repeating unit
			if (r.isReducingEnd())
				throw new Exception(
						"The repeating unit cannot contain the reducing end");
			if (r.isBracket())
				throw new Exception(
						"The repeating unit cannot contain the bracket");
			if (r.isCleavage())
				throw new Exception(
						"The repeating unit cannot contain a cleavage");
			//if (r.isRepetition())
			//	throw new Exception("Repeating units cannot be nested");

			// find the starting point of the repetition and check that all the
			// components are connected
			if (r.getParent() != null && !nodes.contains(r.getParent())) {
				if (first == null) first = r;
				else
					throw new Exception(
							"The residue forming the repeating unit must be all linked together.");
			}

			if (selected_last == null) {
				// first attempt to find the end point of the repetition looking
				// for residues with outbound connections, if more than one
				// throw an exception
				if (r.isSaccharide() && r.getNoChildren() > 0) {
					int links_out = 0;
					for (Linkage l : r.getChildrenLinkages()) {
						if (!nodes.contains(l.getChildResidue())) links_out++;
					}
					if (links_out > 0) {
						if (last == null) last = r;
						else
							throw new Exception(
									"There are more than one residue in the repeating units with children not in the repeating unit. Check that all the residues in the repeating unit have been selected.");
					}
				}
			}
		}

		if (first == null)
			throw new Exception(
					"No available start point for the repeating unit.");
		//if (first.isInRepetition())
		//	throw new Exception("Repeating units cannot be nested");
		if (first.isAntenna())
			throw new Exception("Repeating units cannot be in an antenna");

		if (last == null) {
			if (nodes.size() == 1) last = first;
			else return false;
		}

		// create the repetition
		Residue start = ResidueDictionary.createStartRepetition();
		//start.setAnomericState(first.getAnomericState());
		start.setAnomericCarbon(first.getAnomericCarbon());
		start.setParentLinkage(first.getParentLinkage());

		first.setStartRepetiionResidue(start);
		first.insertParent(start, first.getParentLinkage().getBonds());
		Residue end = ResidueDictionary.createEndRepetition();

		end.setStartResidue(first);
		end.setParentLinkage(new Linkage(last, end, new char[]{first.getParentLinkage().getParentPositionsSingle()}));

		last.setEndRepitionResidue(end);
		last.addChild(end, end.getParentLinkage().getBonds());
		// 20211217, S.TSUCHIYA removed comment out
		for (Iterator<Linkage> il = last.iterator(); il.hasNext(); ) {
			Linkage child_link = il.next();
			if (child_link.getChildResidue() != end
					&& !nodes.contains(child_link.getChildResidue())) {
				end.getChildrenLinkages().add(child_link);
				child_link.setParentResidue(end);
				il.remove();
			}
		}

		fireDocumentChanged();
		return true;
	}

	public boolean createCyclic(Residue selected_last,
								Collection<Residue> nodes) throws Exception {
		Residue first = null;
		Residue last = selected_last;

		for (Residue r : nodes) {
			if (r.isReducingEnd())
				throw new Exception("The cyclic unit cannot contain the reducing end");
			if (r.isBracket())
				throw new Exception("The cyclic unit cannot contain the bracket");
			if (r.isCleavage())
				throw new Exception("The cyclic unit cannot contain a cleavage");
			if (r.getParent() != null && !nodes.contains(r.getParent())) {
				if (first == null) first = r;
				else
					throw new Exception("The residue forming the cyclic unit must be all linked together.");
			}

			if (selected_last == null) {
				if (r.isSaccharide() && r.getNoChildren() > 0) {
					int links_out = 0;
					for (Linkage l : r.getChildrenLinkages()) {
						if (!nodes.contains(l.getChildResidue())) links_out++;
					}
					if (links_out > 0) {
						if (last == null) last = r;
						else
							throw new Exception(
									"There are more than one residue in the cyclic units with children not in the cyclic unit. Check that all the residues in the cyclic unit have been selected.");
					}
				}
			}
		}

		if (first == null)
			throw new Exception("No available start point for the cyclic unit.");
		if (first.isAntenna())
			throw new Exception("Cyclic units cannot be in an antenna");
		if (last == null) {
			if (nodes.size() == 1) last = first;
			else return false;
		}

		if (!first.getParent().canBeReducingEnd())
			throw new Exception("Cyclic structure is should contain a reducing end");
		if (first.isStartCyclic())
			throw new Exception("Cyclic structure can not be nested");

		Residue a_oStartCyclic = ResidueDictionary.createStartCyclic();

		first.setStartCyclicResidue(a_oStartCyclic);
		first.setParentLinkage(new Linkage(a_oStartCyclic, first, first.getParentLinkage().getParentPositionsSingle()));
		a_oStartCyclic.addChild(first, first.getParentLinkage().getBonds());

		Residue a_oEndCyclic = ResidueDictionary.createEndCyclic();

		a_oEndCyclic.setParentLinkage(new Linkage(last, a_oEndCyclic, new char[]{first.getParentLinkage().getParentPositionsSingle()}));
		a_oEndCyclic.setStartResidue(first);

		last.setEndCyclicResidue(a_oEndCyclic);
		last.addChild(a_oEndCyclic);
		// 20211227, S.TSUCHIYA comment out
		/*
		for (Iterator<Linkage> il = last.iterator(); il.hasNext();) {
			Linkage child_link = il.next();
			if (child_link.getChildResidue() != a_oEndCyclic
					&& !nodes.contains(child_link.getChildResidue())) {
				a_oEndCyclic.getChildrenLinkages().add(child_link);
				child_link.setParentResidue(a_oEndCyclic);
				il.remove();
			}
		}
		 */

		fireDocumentChanged();
		return true;
	}

	// ---------------
	// initialization

	public void initData() {
		structures = new LinkedList<Glycan>();
	}

	private Residue cloneStructure(Residue root, HashSet<Residue> nodes,
								   boolean add_attachment) {
		if (root == null) return null;

		if (add_attachment
				&& !root.isReducingEnd()
				&& (root.getParent() == null || !root.getParent()
				.isReducingEnd())) {
			Residue cloned_root = ResidueDictionary.createAttachPoint();
			cloned_root.addChild(cloneStructure(root, nodes, false), root
					.getParentLinkage().getBonds());
			return cloned_root;
		} else {
			// clone this
			Residue cloned_root = root.cloneResidue();

			// clone children
			for (Linkage link : root.getChildrenLinkages()) {
				Residue child = link.getChildResidue();
				if (nodes.contains(child))
					cloned_root.addChild(cloneStructure(child, nodes, false),
							link.getBonds());
			}
			return cloned_root;
		}
	}

	/**
	 * Create a list of structures from the selected residues. If two residues
	 * in the list are linked they will be so in the new structures as well.
	 * Disconnected components will constitute separate structures. Residues in
	 * uncertain antenna will be added to all structures created by the residues
	 * in the same original glycan object.
	 *
	 * @see Glycan
	 * @see Glycan#addAntenna
	 */
	public LinkedList<Glycan> extractView(HashSet<Residue> nodes) {

		// search roots and antennae
		LinkedList<Residue> roots = new LinkedList<Residue>();
		LinkedList<Residue> antennae = new LinkedList<Residue>();
		for (Iterator<Residue> i = nodes.iterator(); i.hasNext(); ) {
			Residue cur = i.next();
			Residue par = cur.getParent();
			if (par == null || !nodes.contains(par) || par.isBracket()) {
				if (cur.isAntenna()) antennae.addLast(cur);
				else if (!cur.isBracket()) roots.addLast(cur);
			}
		}

		// clone roots
		LinkedList<Residue> cloned_roots = new LinkedList<Residue>();
		HashMap<Residue, Glycan> roots_map = new HashMap<Residue, Glycan>();
		for (Residue root : roots) {
			Residue cloned_root = cloneStructure(root, nodes, true);
			if (!cloned_root.isReducingEnd() || cloned_root.hasChildren()) {
				cloned_roots.addLast(cloned_root);
				roots_map.put(cloned_root, findStructureWith(root));
			}
		}

		// map antennae
		HashMap<Residue, Glycan> antennae_map = new HashMap<Residue, Glycan>();
		for (Residue antenna : antennae)
			antennae_map.put(antenna, findStructureWith(antenna));

		// create structures
		LinkedList<Residue> assigned_antennae = new LinkedList<Residue>();
		LinkedList<Glycan> ret_structures = new LinkedList<Glycan>();
		for (Residue cloned_root : cloned_roots) {
			Glycan orig_structure = roots_map.get(cloned_root);
			Glycan ret_structure = new Glycan(cloned_root, false,
					orig_structure.getMassOptions());
			ret_structures.addLast(ret_structure);

			// add antennae
			for (Residue red_antenna : antennae) {
				if (antennae_map.get(red_antenna) == orig_structure) {
					assigned_antennae.addLast(red_antenna);
					ret_structure.addAntenna(cloneStructure(red_antenna, nodes, true));
				}
			}
/*			for (Iterator<Residue> l = antennae.iterator(); l.hasNext();) {
				Residue antenna = l.next();
				if (antennae_map.get(antenna) == orig_structure) {
					assigned_antennae.add(antenna);
					ret_structure.addAntenna(cloneStructure(antenna, nodes,
							true));
				}
			}
*/
		}

		// create structures from unassigned antennae
		for (Residue antenna : antennae) {
			if (!assigned_antennae.contains(antenna)) {
				Glycan orig_structure = antennae_map.get(antenna);
				ret_structures.add(new Glycan(cloneStructure(antenna, nodes,
						true), false, orig_structure.getMassOptions()));
			}
		}

		// remove unmatched repetitions
		for (Glycan s : ret_structures) {
			s.removeUnpairedRepetitions();
		}

		return ret_structures;
	}

	// ---------------
	// serialization

	/**
	 * Get the list of available formats for parsing glycan structures.
	 *
	 * @see GlycanParserFactory#getImportFormats
	 */
	static public Map<String, String> getImportFormats() {
		return GlycanParserFactory.getImportFormats();
	}

	/**
	 * Get the list of available formats for encoding glycan structures.
	 *
	 * @see GlycanParserFactory#getExportFormats
	 */
	static public Map<String, String> getExportFormats() {
		return GlycanParserFactory.getExportFormats();
	}

	/**
	 * Get the list of all available formats for glycan structures.
	 *
	 * @see GlycanParserFactory#getFormats
	 */
	static public Map<String, String> getFormats() {
		System.out.println("GlycanDocument1109");
		return GlycanParserFactory.getFormats();
	}

	/**
	 * Return <code>true</code> if the format is supported.
	 *
	 * @see GlycanParserFactory#isSequenceFormat
	 */
	static public boolean isSequenceFormat(String format) {
		return GlycanParserFactory.isSequenceFormat(format);
	}

	/**
	 * Return <code>true</code> if the format support multiple structures in the
	 * same string.
	 */
	static public boolean supportMultipleStructures(String format) {
		if (!GlycanParserFactory.isSequenceFormat(format))
			return true;

		try {
			GlycanParser parser = GlycanParserFactory.getParser(format);
			return (parser != null && parser instanceof GWSParser);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Parse the structures from a file content using the specified format.
	 *
	 * @see GlycanParserFactory#getParser
	 */
	public boolean importFrom(String filename, String format) {
		try {
			// read file
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			return importFromString(consume(br), format);
		} catch (Exception e) {
			LogUtils.report(e);
			return false;
		}
	}


	/**
	 * Parse the structures from a string using the specified format.
	 *
	 * @see GlycanParserFactory#getParser
	 */
	public boolean importFromString(String buffer, String format) {
		return importFromString(buffer, format, false);
	}

	/**
	 * Parse the structures from a string using the specified format.
	 *
	 * @param tolerate_unknown if <code>true</code> tolerate residues of a type that is not
	 *                         specified in the dictionary
	 * @see GlycanParserFactory#getParser
	 */
	public boolean importFromString(String buffer, String format, boolean tolerate_unknown) {
		try {
			// System.out.println("Importing from " + format + " with " +
			// tolerate_unknown);
			// read structures

			GlycanParser parser = GlycanParserFactory.getParser(format);
			parser.setTolerateUnknown(tolerate_unknown);
			fromString(buffer, true, true, parser);
			return true;
		} catch (Exception e) {
			LogUtils.report(e);
			return false;
		}
	}

	/**
	 * Encode the structures into a string using the specified format:
	 */
	public boolean exportFromStructure(Collection<Glycan> a_lstGlycan, String format) {
		try {
			String str_encode = "";
			GlycanParser parser = GlycanParserFactory.getParser(format);
			str_encode = toString(a_lstGlycan, parser);
			if (str_encode.equals("")) throw new Exception("Invalid output string");

			for (String s : str_encode.split(";")) this.lst_encode.addLast(s);
			return true;
		} catch (Exception e) {
			LogUtils.report(e);
			return false;
		}
	}

	/**
	 *  Encode all structures and return specific format
	 *  @author GIC 20210105
	 */
	public ArrayList<String> exportFromStructures(Collection<Glycan> a_lstGlycan, String format) throws Exception {
		ArrayList<String> arr = new ArrayList<>();
		GlycanParser parser = GlycanParserFactory.getParser(format); 
		for(Glycan glycan : a_lstGlycan) {
			LinkedList<Glycan> colGlycan = new LinkedList<Glycan>();
			colGlycan.add(glycan);
			arr.add(toString(colGlycan,parser).toString());
		} 
		return arr; 
	}
	
	/**
	 * Encode the structures into a file using the specified format.
	 *
	 * @see GlycanParserFactory#getParser
	 */
	public boolean exportTo(String filename, String format) {
		try {
			System.err.println("Exporting: " + format);
			System.err.println("To file: " + filename);
			// open file
			FileOutputStream fos = new FileOutputStream(filename);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

			// serialize structures
			GlycanParser parser = GlycanParserFactory.getParser(format);
			String str = toString(parser);
			if (str == null) throw new Exception("Invalid output string");

			// write to file
			bw.write(str, 0, str.length());
			bw.newLine();
			bw.close();

			return true;
		} catch (Exception e) {
			LogUtils.report(e);
			return false;
		}
	}

	/**
	 * Encode the selected structures into a file using the specified format.
	 *
	 * @see GlycanParserFactory#getParser
	 */
	static public boolean exportTo(Collection<Glycan> toexport,
								   String filename, String format) {
		try {
			// open file
			FileOutputStream fos = new FileOutputStream(filename);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

			// serialize structures
			GlycanParser parser = GlycanParserFactory.getParser(format);
			String str = toString(toexport, parser);
			if (str == null)
				throw new Exception("Invalid output string");

			// write to file
			bw.write(str, 0, str.length());
			bw.newLine();
			bw.close();

			return true;
		} catch (Exception e) {
			LogUtils.report(e);
			return false;
		}
	}

	/**
	 * Return a GlycoCT representation of the structures contained in the
	 * document.
	 *
	 * @see GlycoCTParser
	 */
	public String toGlycoCT() {
		return toString(structures, new GlycoCTParser(false));
	}

	/**
	 * Return a GlycoCTCondensed representation of the structures contained in the
	 * document.
	 *
	 * @see GlycoCTParser
	 */
	public String toGlycoCTCondensed() {
		return toString(structures, new GlycoCTCondensedParser(false));
	}

	protected void fromGlycoCT(String str, boolean merge, boolean fire,
							   boolean tolerate) throws Exception {
		fromString(str, merge, fire, new GlycoCTParser(tolerate));
	}

	/**
	 * Create a string representation of the structures contained in the
	 * document.
	 *
	 * @see GWSParser
	 */
	public String toString() {
		return toString(structures, new GWSParser());
	}

	/**
	 * Create a string representation of the structures contained in the
	 * document using the specified parser.
	 */
	public String toString(GlycanParser parser) {
		return toString(structures, parser);
	}

	/**
	 * Create a string representation of the structures contained in the
	 * document in the specified format.
	 *
	 * @see GlycanParserFactory#getParser
	 */
	public String toString(String format) {
		try {
			return toString(structures, GlycanParserFactory.getParser(format));
		} catch (Exception e) {
			LogUtils.report(e);
			return "";
		}
	}

	public String toStringWithCoordinates(String format, BBoxManager bboxManager) {
		try {
			return toString(structures, GlycanParserFactory.getParser(format), bboxManager);
		} catch (Exception e) {
			LogUtils.report(e);
			return "";
		}
	}

	/**
	 * Create a string representation of the specified structures.
	 *
	 * @see GWSParser
	 */
	static public String toString(Collection<Glycan> structures) {
		return toString(structures, new GWSParser());
	}


	static public String toString(Collection<Glycan> structures,
								  GlycanParser parser) {
		return toString(structures, parser, null);
	}

	/**
	 * Create a string representation of the specified structures using the
	 * given parser.
	 */
	static public String toString(Collection<Glycan> structures,
								  GlycanParser parser, BBoxManager bboxManager) {

		String str = "";
		if (parser instanceof GWSParser) {
			for (Iterator<Glycan> i = structures.iterator(); i.hasNext(); ) {
				str += parser.writeGlycan(i.next(), bboxManager);
				if (i.hasNext()) str += ";";
			}
		} else {
			if (bboxManager != null) { //At the moment this will force conversion to GlycoCT_XML
				str = parser.writeGlycan(structures.isEmpty() ? null : structures
						.iterator().next(), bboxManager);
			} else {
				str = parser.writeGlycan(structures.isEmpty() ? null : structures
						.iterator().next());
			}

		}

		return str;
	}

	public void fromString(String str, String format) throws Exception {
		setStructures(parseString(str, GlycanParserFactory.getParser(format)), true);
	}

	public void fromURL(final String resourceLocation, final String format) throws Exception {
		java.security.AccessController.doPrivileged(
				new java.security.PrivilegedAction<String>() {
					public String run() {
						try {
							InputStream inStream = null;

							URL url = new URL(resourceLocation);
							inStream = url.openStream();

							BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

							StringBuffer buffer = new StringBuffer();

							String line;
							while ((line = reader.readLine()) != null) {
								buffer.append(line + "\n");
							}

							fromString(buffer.toString(), format);
						} catch (Exception ex) {
							LogUtils.report(ex);
						}

						return null;
					}
				}
		);
	}

	/**
	 * Add the structures parsed from the input string.
	 *
	 * @param merge if <code>true</code> append the structures, otherwise
	 *              overwrite the existing ones.
	 */
	public void fromString(String str, boolean merge) throws Exception {
		fromString(str, merge, false, new GWSParser());
	}

	public void fromString(String str, boolean merge, boolean fire,
						   GlycanParser parser) throws Exception {
		if (merge)
			addStructures(parseString(str, parser), fire);
		else
			setStructures(parseString(str, parser), fire);
	}

	/**
	 * Parse structures from the input string using a specified parser.
	 */
	public LinkedList<Glycan> parseString(String str, GlycanParser parser)
			throws Exception {

		LinkedList<Glycan> parsed = new LinkedList<Glycan>();
		if (parser instanceof GWSParser) {
			for (String t : TextUtils.tokenize(str, ";"))
				parsed.addLast(parser.readGlycan(t,
						theWorkspace.getDefaultMassOptions()));
		} else
			parsed.addLast(parser.readGlycan(str,
					theWorkspace.getDefaultMassOptions()));

		return parsed;
	}

	/**
	 * Parse structures from the input string using a specified parser.
	 *
	 * @see GWSParser
	 */
	public LinkedList<Glycan> parseString(String str) throws Exception {
		// parse structures
		LinkedList<Glycan> parsed = new LinkedList<Glycan>();
		for (String t : TextUtils.tokenize(str, ";")) {
			parsed.addLast(GWSParser.fromString(t,
					theWorkspace.getDefaultMassOptions()));
		}
		return parsed;
	}

	/**
	 * Parse structures from the input string using a specified parser. Use the
	 * selected mass settings for all new structures.
	 *
	 * @see GWSParser
	 */
	static public LinkedList<Glycan> parseString(String str, MassOptions opt)
			throws Exception {
		// parse structures
		LinkedList<Glycan> parsed = new LinkedList<Glycan>();
		for (String t : TextUtils.tokenize(str, ";"))
			parsed.addLast(GWSParser.fromString(t, opt));
		return parsed;
	}

	/**
	 * Create a new document from its XML representation as part of a DOM tree.
	 */
	public void fromXML(Node struct_node, boolean merge) throws Exception {
		// clear
		if (!merge) {
			resetStatus();
			initData();
		} else
			setChanged(true);

		// read
		LinkedList<Node> g_nodes = XMLUtils.findAllChildren(struct_node, "Glycan");
		for (Node g_node : g_nodes)
			structures.addLast(Glycan.fromXML(g_node,
					theWorkspace.getDefaultMassOptions()));
	}

	/**
	 * Create an XML representation of this object to be part of a DOM tree.
	 */
	public Element toXML(Document document) {
		if (document == null)
			return null;

		// create root node
		Element struct_node = document.createElement("Structures");

		// add structures
		for (Glycan s : structures)
			struct_node.appendChild(s.toXML(document));

		return struct_node;
	}

	/**
	 * Default SAX handler to read a representation of this object from an XML
	 * stream.
	 */
	public static class SAXHandler extends SAXUtils.ObjectTreeHandler {

		private GlycanDocument theDocument;
		private boolean merge;

		/**
		 * Construct a new handler.
		 *
		 * @param _doc   recipient for the structures parsed from the XML
		 * @param _merge if <code>true</code> append the new structures to the
		 *               existing document.
		 */
		public SAXHandler(GlycanDocument _doc, boolean _merge) {
			theDocument = _doc;
			merge = _merge;
		}

		public boolean isElement(String namespaceURI, String localName,
								 String qName) {
			return qName.equals(getNodeElementName());
		}

		/**
		 * Return the element tag recognized by this handler
		 */
		public static String getNodeElementName() {
			return "Structures";
		}

		protected SAXUtils.ObjectTreeHandler getHandler(String namespaceURI,
														String localName, String qName) {
			if (qName.equals(Glycan.SAXHandler.getNodeElementName()))
				return new Glycan.SAXHandler(new MassOptions());
			return null;
		}

		protected Object finalizeContent(String namespaceURI, String localName,
										 String qName) throws SAXException {
			// clear
			if (!merge) {
				theDocument.resetStatus();
				theDocument.initData();
			} else
				theDocument.setChanged(true);

			// read data
			for (Object o : getSubObjects(Glycan.SAXHandler
					.getNodeElementName()))
				theDocument.structures.add((Glycan) o);

			return (object = theDocument);
		}
	}

	public void write(TransformerHandler th) throws SAXException {
		th.startElement("", "", "Structures", new AttributesImpl());
		for (Glycan s : structures)
			s.write(th);
		th.endElement("", "", "Structures");
	}
}

class DescendingDoubleComparator implements Comparator<Double> {
	public int compare(Double o1, Double o2) {
		return -o1.compareTo(o2);
	}
}