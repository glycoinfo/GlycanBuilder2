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

import java.util.*;

import org.eurocarbdb.application.glycanbuilder.dataset.CrossRingFragmentDictionary;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;

/**
 * This utility class is used to generate all possible fragments of a glycan
 * molecule.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class Fragmenter {

	protected boolean afragments = true;
	protected boolean bfragments = true;
	protected boolean cfragments = true;
	protected boolean xfragments = true;
	protected boolean yfragments = true;
	protected boolean zfragments = true;
	protected boolean internal_fragments = true;
	protected int max_no_cleavages = 2;
	protected int max_no_crossrings = 1;
	protected boolean small_ring_fragments = true;
	protected boolean iterate_ion_combinations = false;

	/**
	 * Initialize the fragmenter using the default options.
	 */

	public Fragmenter() {
	}

	/**
	 * Initialize the fragmenter using the given options.
	 */

	public Fragmenter(FragmentOptions opt) {
		if (opt != null) {
			afragments = opt.ADD_AFRAGMENTS;
			bfragments = opt.ADD_BFRAGMENTS;
			cfragments = opt.ADD_CFRAGMENTS;
			xfragments = opt.ADD_XFRAGMENTS;
			yfragments = opt.ADD_YFRAGMENTS;
			zfragments = opt.ADD_ZFRAGMENTS;

			internal_fragments = opt.INTERNAL_FRAGMENTS;

			max_no_cleavages = opt.MAX_NO_CLEAVAGES;
			max_no_crossrings = opt.MAX_NO_CROSSRINGS;

			iterate_ion_combinations = opt.ITERATE_ION_COMBINATIONS;
		}
	}

	/**
	 * Return the compute A-type fragments flag.
	 */
	public boolean getComputeAFragments() {
		return afragments;
	}

	/**
	 * Set the compute A-type fragments flag.
	 */
	public void setComputeAFragments(boolean f) {
		afragments = f;
	}

	/**
	 * Return the compute B-type fragments flag.
	 */
	public boolean getComputeBFragments() {
		return afragments;
	}

	/**
	 * Set the compute B-type fragments flag.
	 */
	public void setComputeBFragments(boolean f) {
		bfragments = f;
	}

	/**
	 * Return the compute C-type fragments flag.
	 */
	public boolean getComputeCFragments() {
		return afragments;
	}

	/**
	 * Set the compute C-type fragments flag.
	 */
	public void setComputeCFragments(boolean f) {
		cfragments = f;
	}

	/**
	 * Return the compute X-type fragments flag.
	 */
	public boolean getComputeXFragments() {
		return afragments;
	}

	/**
	 * Set the compute X-type fragments flag.
	 */
	public void setComputeXFragments(boolean f) {
		xfragments = f;
	}

	/**
	 * Return the compute Y-type fragments flag.
	 */
	public boolean getComputeYFragments() {
		return yfragments;
	}

	/**
	 * Set the compute Y-type fragments flag.
	 */
	public void setComputeYFragments(boolean f) {
		yfragments = f;
	}

	/**
	 * Return the compute Z-type fragments flag.
	 */
	public boolean getComputeZFragments() {
		return zfragments;
	}

	/**
	 * Set the compute Z-type fragments flag.
	 */
	public void setComputeZFragments(boolean f) {
		zfragments = f;
	}

	/**
	 * Return the compute internal fragments flag.
	 */
	public boolean getComputeInternalFragments() {
		return internal_fragments;
	}

	/**
	 * Set the compute internal fragments flag.
	 */
	public void setComputeInternalFragments(boolean f) {
		internal_fragments = f;
	}

	/**
	 * Return the maximum number of cleavages that this fragmenter will compute
	 * for each structure
	 */
	public int getMaxNoCleavages() {
		return max_no_cleavages;
	}

	/**
	 * Set the maximum number of cleavages that this fragmenter will compute for
	 * each structure
	 */
	public void setMaxNoCleavages(int i) {
		max_no_cleavages = i;
	}

	/**
	 * Return the maximum number of cross-ring fragments that this fragmenter
	 * will compute for each structure
	 */
	public int getMaxNoCrossRings() {
		return max_no_crossrings;
	}

	/**
	 * set the maximum number of cross-ring fragments that this fragmenter will
	 * compute for each structure
	 */
	public void setMaxNoCrossRings(int i) {
		max_no_crossrings = i;
	}

	public boolean isIterate_ion_combinations() {
		return iterate_ion_combinations;
	}

	public void setIterate_ion_combinations(boolean iterate_ion_combinations) {
		this.iterate_ion_combinations = iterate_ion_combinations;
	}

	/**
	 * Return <code>true</code> if this fragmenter will create fragments
	 * containing no intact saccharides.
	 */
	public boolean getComputeSmallRingFragments() {
		return small_ring_fragments;
	}

	/**
	 * Set to <code>true</code> if this fragmenter should create fragments
	 * containing no intact saccharides.
	 */
	public void setComputeSmallRingFragments(boolean f) {
		small_ring_fragments = f;
	}

	// profiling

	/**
	 * Generate all possible configuration of labile residues given a set of
	 * structures.
	 * 
	 * @see Glycan#detachLabileResidues
	 * @see Glycan#getAllLabilesConfigurations
	 */
	static public FragmentDocument generateLabilesConfigurations(
			Collection<Glycan> structures) {
		FragmentDocument ret = new FragmentDocument();
		for (Glycan s : structures) {
			FragmentCollection fc = new FragmentCollection();

			// prepare labiles
			Glycan parent = s.detachLabileResidues().removeDetachedLabiles();
			TypePattern labiles = parent.getLabilePositionsPattern();

			// remove exchanges from parent mass options
			MassOptions mass_opt = s.getMassOptions().removeExchanges();
			parent.setMassOptions(mass_opt);

			// compute fragments
			for (Glycan conf : parent.getAllLabilesConfigurations(labiles))
				fc.addFragment(conf, getFragmentType(conf));

			// add to document
			ret.addFragments(parent, fc);
		}

		return ret;
	}

	// ----------------
	// Fragments

	/**
	 * Compute all fragments of a given structure resulting from cleaveages at
	 * the specified residue. Fuzzy structures and structure with repeating
	 * units cannot be fragmented.
	 */
	public FragmentCollection computeFragments(Glycan structure, Residue current) {
		FragmentCollection fragments = new FragmentCollection();
		computeFragments(fragments, structure, current);
		return fragments;
	}

	/**
	 * Compute all fragments of a given structure resulting from cleavages at
	 * the specified residue. Fuzzy structures and structure with repeating
	 * units cannot be fragmented. Return the results in <code>fragments</code>
	 */
	public void computeFragments(FragmentCollection fragments,
			Glycan structure, Residue current) {

		if (structure != null && structure.contains(current)
				&& !structure.isFuzzy(true) && !structure.hasRepetition()) {

			MassOptions mass_opt = structure.getMassOptions().removeExchanges();

			// glycosidic cleavages
			if (canDoCleavage(current)) {
				if (bfragments) {
					Glycan b_frag = getBFragment(current, mass_opt);
					fragments.addFragment(b_frag, getFragmentType(b_frag));
				}
				if (cfragments) {
					Glycan c_frag = getCFragment(current, mass_opt);
					fragments.addFragment(c_frag, getFragmentType(c_frag));
				}
				if (yfragments) {
					Glycan y_frag = getYFragment(current, mass_opt);
					fragments.addFragment(y_frag, getFragmentType(y_frag));
				}
				if (zfragments) {
					Glycan z_frag = getZFragment(current, mass_opt);
					fragments.addFragment(z_frag, getFragmentType(z_frag));
				}
			}

			// cross ring fragments
			if (canDoRingFragment(current)) {
				if (afragments) {
					for (CrossRingFragmentType crt : CrossRingFragmentDictionary
							.getCrossRingFragmentTypesA(current)) {
						Glycan a_frag = getAFragment(current, crt, false,
								mass_opt);
						fragments.addFragment(a_frag, getFragmentType(a_frag));
					}
				}

				if (xfragments) {
					for (CrossRingFragmentType crt : CrossRingFragmentDictionary
							.getCrossRingFragmentTypesX(current)) {
						Glycan x_frag = getXFragment(current, crt, false,
								mass_opt);
						fragments.addFragment(x_frag, getFragmentType(x_frag));
					}
				}
			}
		}
	}

	/**
	 * Compute all fragments of a given structure.
	 */
	public FragmentCollection computeAllFragments(Glycan structure) {
		FragmentCollection fragments = new FragmentCollection();
		computeAllFragments(fragments, structure);
		return fragments;
	}

	/**
	 * Compute all fragments of a given structure. Return the results in
	 * <code>fragments</code>.
	 */
	public void computeAllFragments(FragmentCollection fragments,
			Glycan structure) {
		if (structure != null && !structure.isFuzzy(true)
				&& !structure.hasRepetition()) {
			// remove exchanges from parent mass options
			MassOptions mass_opt = structure.getMassOptions().removeExchanges();
			List<IonCloud> ionClouds;

			if(iterate_ion_combinations){
				ionClouds=mass_opt.ION_CLOUD.generateCombinations();
			}else{
				ionClouds=new ArrayList<IonCloud>();
				ionClouds.add(mass_opt.ION_CLOUD);
			}

			//LogUtils.report(new Exception("Ion cloud count: "+ionClouds.get(0).toString()+"|"+ionClouds.get(1).toString()+"|"+iterate_ion_combinations));

			for(IonCloud ionCloud:ionClouds){
				MassOptions options=mass_opt.clone();
				options.ION_CLOUD=ionCloud.clone();

				Glycan parent = structure.clone();
				parent.setMassOptions(options);

				// compute fragments
				if (parent.hasLabileResidues()) {
					computeAllFragmentsWithLabiles(fragments, parent,
							max_no_cleavages, Math.min(max_no_cleavages,
									max_no_crossrings), options);
					for (Glycan conf : parent.getAllLabilesConfigurations())
						fragments.addFragment(conf, getFragmentType(conf));
				} else {
					computeAllFragments(fragments, parent.getRoot(),
							max_no_cleavages, Math.min(max_no_cleavages,
									max_no_crossrings), options);
					fragments.addFragment(parent, getFragmentType(parent));
				}
			}
		}
	}

	protected void computeAllFragments(FragmentCollection fragments,
			Residue current, int cur_max_no_cleavages,
			int cur_max_no_crossrings, MassOptions mass_opt) {
		if (cur_max_no_cleavages == 0)
			return;

		// glycosidic cleavages
		if (canDoCleavage(current)) {
			if (bfragments) {
				Glycan b_frag = getBFragment(current, mass_opt);
				if (fragments.addFragment(b_frag, getFragmentType(b_frag)))
					computeAllFragments(fragments, b_frag.getRoot(),
							cur_max_no_cleavages - 1, cur_max_no_crossrings,
							mass_opt);

			}

			if (cfragments) {
				Glycan c_frag = getCFragment(current, mass_opt);
				if (fragments.addFragment(c_frag, getFragmentType(c_frag)))
					computeAllFragments(fragments, c_frag.getRoot(),
							cur_max_no_cleavages - 1, cur_max_no_crossrings,
							mass_opt);
			}

			if (yfragments) {
				Glycan y_frag = getYFragment(current, mass_opt);
				if (fragments.addFragment(y_frag, getFragmentType(y_frag)))
					computeAllFragments(fragments, y_frag.getRoot(),
							cur_max_no_cleavages - 1, cur_max_no_crossrings,
							mass_opt);
			}

			if (zfragments) {
				Glycan z_frag = getZFragment(current, mass_opt);
				if (fragments.addFragment(z_frag, getFragmentType(z_frag)))
					computeAllFragments(fragments, z_frag.getRoot(),
							cur_max_no_cleavages - 1, cur_max_no_crossrings,
							mass_opt);
			}
		}

		// cross ring cleavages
		if (cur_max_no_crossrings > 0 && canDoRingFragment(current)) {
			if (afragments) {
				for (CrossRingFragmentType crt : CrossRingFragmentDictionary
						.getCrossRingFragmentTypesA(current)) {
					Glycan a_frag = getAFragment(current, crt, false, mass_opt);
					if (fragments.addFragment(a_frag, getFragmentType(a_frag)))
						computeAllFragments(fragments, a_frag.getRoot(),
								cur_max_no_cleavages - 1,
								cur_max_no_crossrings - 1, mass_opt);
				}
			}

			if (xfragments) {
				for (CrossRingFragmentType crt : CrossRingFragmentDictionary
						.getCrossRingFragmentTypesX(current)) {
					Glycan x_frag = getXFragment(current, crt, false, mass_opt);
					if (fragments.addFragment(x_frag, getFragmentType(x_frag)))
						computeAllFragments(fragments, x_frag.getRoot(),
								cur_max_no_cleavages - 1,
								cur_max_no_crossrings - 1, mass_opt);
				}
			}
		}

		// recursion
		for (Linkage l : current.getChildrenLinkages())
			computeAllFragments(fragments, l.getChildResidue(),
					cur_max_no_cleavages, cur_max_no_crossrings, mass_opt);
	}

	protected void computeAllFragmentsWithLabiles(FragmentCollection fragments,
			Glycan structure, int cur_max_no_cleavages,
			int cur_max_no_crossrings, MassOptions mass_opt) {
		if (cur_max_no_cleavages == 0)
			return;

		structure = structure.detachLabileResidues();
		TypePattern avail_labiles = structure.getDetachedLabilesPattern();
		computeAllFragmentsWithLabiles(fragments, structure.getRoot(),
				avail_labiles, cur_max_no_cleavages, cur_max_no_crossrings,
				mass_opt);
	}

	protected void computeAllFragmentsWithLabiles(FragmentCollection fragments,
			Residue current, TypePattern avail_labiles,
			int cur_max_no_cleavages, int cur_max_no_crossrings,
			MassOptions mass_opt) {
		if (cur_max_no_cleavages == 0)
			return;

		// glycosidic cleavages
		if (canDoCleavage(current)) {
			if (bfragments) {
				for (Glycan b_frag : Glycan.getAllLabilesConfigurations(
						getBFragment(current, mass_opt), avail_labiles)) {
					if (fragments.addFragment(b_frag, getFragmentType(b_frag)))
						computeAllFragmentsWithLabiles(fragments, b_frag,
								cur_max_no_cleavages - 1,
								cur_max_no_crossrings, mass_opt);
				}
			}

			if (cfragments) {
				for (Glycan c_frag : Glycan.getAllLabilesConfigurations(
						getCFragment(current, mass_opt), avail_labiles)) {
					if (fragments.addFragment(c_frag, getFragmentType(c_frag)))
						computeAllFragmentsWithLabiles(fragments, c_frag,
								cur_max_no_cleavages - 1,
								cur_max_no_crossrings, mass_opt);
				}
			}

			if (yfragments) {
				for (Glycan y_frag : Glycan.getAllLabilesConfigurations(
						getYFragment(current, mass_opt), avail_labiles)) {
					if (fragments.addFragment(y_frag, getFragmentType(y_frag)))
						computeAllFragmentsWithLabiles(fragments, y_frag,
								cur_max_no_cleavages - 1,
								cur_max_no_crossrings, mass_opt);
				}
			}

			if (zfragments) {
				for (Glycan z_frag : Glycan.getAllLabilesConfigurations(
						getZFragment(current, mass_opt), avail_labiles)) {
					if (fragments.addFragment(z_frag, getFragmentType(z_frag)))
						computeAllFragmentsWithLabiles(fragments, z_frag,
								cur_max_no_cleavages - 1,
								cur_max_no_crossrings, mass_opt);
				}
			}
		}

		// cross ring cleavages
		if (cur_max_no_crossrings > 0 && canDoRingFragment(current)) {
			if (afragments) {
				for (CrossRingFragmentType crt : CrossRingFragmentDictionary
						.getCrossRingFragmentTypesA(current)) {
					for (Glycan a_frag : Glycan.getAllLabilesConfigurations(
							getAFragment(current, crt, true, mass_opt),
							avail_labiles)) {
						if ((!a_frag.isSmallRingFragment() || a_frag
								.countCharges() > 0)
								&& fragments.addFragment(a_frag,
										getFragmentType(a_frag)))
							computeAllFragmentsWithLabiles(fragments, a_frag,
									cur_max_no_cleavages - 1,
									cur_max_no_crossrings - 1, mass_opt);
					}
				}
			}

			if (xfragments) {
				for (CrossRingFragmentType crt : CrossRingFragmentDictionary
						.getCrossRingFragmentTypesX(current)) {
					for (Glycan x_frag : Glycan.getAllLabilesConfigurations(
							getXFragment(current, crt, true, mass_opt),
							avail_labiles)) {
						if ((!x_frag.isSmallRingFragment() || x_frag
								.countCharges() > 0)
								&& fragments.addFragment(x_frag,
										getFragmentType(x_frag)))
							computeAllFragmentsWithLabiles(fragments, x_frag,
									cur_max_no_cleavages - 1,
									cur_max_no_crossrings - 1, mass_opt);
					}
				}
			}
		}

		// recursion
		for (Linkage l : current.getChildrenLinkages())
			computeAllFragmentsWithLabiles(fragments, l.getChildResidue(),
					avail_labiles, cur_max_no_cleavages, cur_max_no_crossrings,
					mass_opt);
	}

	/**
	 * Return <code>true</code> if the structure can be cleaved at the specified
	 * residue with a glycosidic cleavage. Fuzzy structures and structure with
	 * repeating unit cannot be fragmented. If <code>tolerate_labiles</code> is
	 * <code>true</code> the fragments could be computed even if some labile
	 * residues have unspeficied connectivity.
	 * 
	 * @see Glycan#isFuzzy(boolean)
	 * @see #canDoCleavage(Residue)
	 */
	static public boolean canDoCleavage(Glycan structure, Residue current,
			boolean tolerate_labiles) {
		return (structure != null && !structure.isFuzzy(tolerate_labiles)
				&& !structure.hasRepetition() && canDoCleavage(current));
	}

	/**
	 * Return <code>true</code> if the structure can be cleaved at the specified
	 * residue with a glycosidic cleavage. Fuzzy structures and structure with
	 * repeating unit cannot be fragmented.
	 * 
	 * @see Glycan#isFuzzy
	 * @see #canDoCleavage(Residue)
	 */
	static public boolean canDoCleavage(Glycan structure, Residue current) {
		return (structure != null && !structure.isFuzzy()
				&& !structure.hasRepetition() && canDoCleavage(current));
	}

	/**
	 * Return <code>true</code> if a glycosidic-cleavage can be computed at the
	 * specified residue.
	 * 
	 * @see Residue#isCleavable
	 */
	static public boolean canDoCleavage(Residue current) {
		return (current != null && current.isCleavable()
				&& current.getParent() != null && current.getParent()
				.isCleavable());
	}

	/**
	 * Return <code>true</code> if the structure can be cleaved at the specified
	 * residue with a ring cleavage. Fuzzy structures and structure with
	 * repeating units cannot be fragmented. If <code>tolerate_labiles</code> is
	 * <code>true</code> the fragments could be computed even if some labile
	 * residues have unspeficied connectivity.
	 * 
	 * @see Glycan#isFuzzy(boolean)
	 * @see #canDoCleavage(Residue)
	 */
	static public boolean canDoRingFragment(Glycan structure, Residue current,
			boolean tolerate_labiles) {
		return (structure != null && !structure.isFuzzy(tolerate_labiles)
				&& !structure.hasRepetition() && canDoRingFragment(current));
	}

	/**
	 * Return <code>true</code> if the structure can be cleaved at the specified
	 * residue with a ring cleavage. Fuzzy structures and structure with
	 * repeating units cannot be fragmented.
	 * 
	 * @see Glycan#isFuzzy
	 * @see #canDoCleavage(Residue)
	 */
	static public boolean canDoRingFragment(Glycan structure, Residue current) {
		return (structure != null && !structure.isFuzzy()
				&& !structure.hasRepetition() && canDoRingFragment(current));
	}

	/**
	 * Return <code>true</code> if a ring-cleavage can be computed at the
	 * specified residue. All linkages with the residue must be valid and fully
	 * specified.
	 * 
	 * @see Residue#checkLinkages
	 */
	static public boolean canDoRingFragment(Residue current) {
		return (current != null && current.isSaccharide() && current
				.checkLinkages());
	}

	/**
	 * Return all possible configurations of labile residues for the A-type
	 * fragment resulting from the cleavage of the given structure at the
	 * specified residue.
	 * 
	 * @param type
	 *            the type of A fragment to be computed
	 * @see CrossRingFragmentDictionary#getCrossRingFragmentTypesA(Residue)
	 * @see Glycan getAllLabilesConfigurations
	 */
	public Collection<Glycan> getAllAFragmentsWithLabiles(Glycan structure,
			Residue current, CrossRingFragmentType type) {
		if (!canDoRingFragment(structure, current, true))
			return new LinkedList<Glycan>();

		structure = structure.detachLabileResidues();
		TypePattern avail_labiles = structure.getDetachedLabilesPattern();
		Glycan fragment = getAFragment(current, type, false, structure
				.getMassOptions().removeExchanges());
		if (fragment == null)
			return new LinkedList<Glycan>();
		return Glycan.getAllLabilesConfigurations(fragment
				.detachLabileResidues(), avail_labiles);
	}

	/**
	 * Return all possible configurations of labile residues for the B-type
	 * fragment resulting from the cleavage of the given structure at the
	 * specified residue.
	 * 
	 * @see Glycan getAllLabilesConfigurations
	 */
	public Collection<Glycan> getAllBFragmentsWithLabiles(Glycan structure,
			Residue current) {
		if (!canDoCleavage(structure, current, true))
			return new LinkedList<Glycan>();

		structure = structure.detachLabileResidues();
		TypePattern avail_labiles = structure.getDetachedLabilesPattern();
		Glycan fragment = getBFragment(current, structure.getMassOptions()
				.removeExchanges());
		if (fragment == null)
			return new LinkedList<Glycan>();
		return Glycan.getAllLabilesConfigurations(fragment
				.detachLabileResidues(), avail_labiles);
	}

	/**
	 * Return all possible configurations of labile residues for the C-type
	 * fragment resulting from the cleavage of the given structure at the
	 * specified residue.
	 * 
	 * @see Glycan getAllLabilesConfigurations
	 */
	public Collection<Glycan> getAllCFragmentsWithLabiles(Glycan structure,
			Residue current) {
		if (!canDoCleavage(structure, current, true))
			return new LinkedList<Glycan>();

		structure = structure.detachLabileResidues();
		TypePattern avail_labiles = structure.getDetachedLabilesPattern();
		Glycan fragment = getCFragment(current, structure.getMassOptions()
				.removeExchanges());
		if (fragment == null)
			return new LinkedList<Glycan>();
		return Glycan.getAllLabilesConfigurations(fragment
				.detachLabileResidues(), avail_labiles);
	}

	/**
	 * Return all possible configurations of labile residues for the X-type
	 * fragment resulting from the cleavage of the given structure at the
	 * specified residue.
	 * 
	 * @param type
	 *            the type of X fragment to be computed
	 * @see CrossRingFragmentDictionary#getCrossRingFragmentTypesX(Residue)
	 * @see Glycan getAllLabilesConfigurations
	 */
	public Collection<Glycan> getAllXFragmentsWithLabiles(Glycan structure,
			Residue current, CrossRingFragmentType type) {
		if (!canDoRingFragment(structure, current, true))
			return new LinkedList<Glycan>();

		structure = structure.detachLabileResidues();
		TypePattern avail_labiles = structure.getDetachedLabilesPattern();
		Glycan fragment = getXFragment(current, type, false, structure
				.getMassOptions().removeExchanges());
		if (fragment == null)
			return new LinkedList<Glycan>();
		return Glycan.getAllLabilesConfigurations(fragment
				.detachLabileResidues(), avail_labiles);
	}

	/**
	 * Return all possible configurations of labile residues for the Y-type
	 * fragment resulting from the cleavage of the given structure at the
	 * specified residue.
	 * 
	 * @see Glycan getAllLabilesConfigurations
	 */
	public Collection<Glycan> getAllYFragmentsWithLabiles(Glycan structure,
			Residue current) {
		if (!canDoCleavage(structure, current, true))
			return new LinkedList<Glycan>();

		structure = structure.detachLabileResidues();
		TypePattern avail_labiles = structure.getDetachedLabilesPattern();
		Glycan fragment = getYFragment(current, structure.getMassOptions()
				.removeExchanges());
		if (fragment == null)
			return new LinkedList<Glycan>();
		return Glycan.getAllLabilesConfigurations(fragment
				.detachLabileResidues(), avail_labiles);
	}

	/**
	 * Return all possible configurations of labile residues for the Z-type
	 * fragment resulting from the cleavage of the given structure at the
	 * specified residue.
	 * 
	 * @see Glycan getAllLabilesConfigurations
	 */
	public Collection<Glycan> getAllZFragmentsWithLabiles(Glycan structure,
			Residue current) {
		if (!canDoCleavage(structure, current, true))
			return new LinkedList<Glycan>();

		structure = structure.detachLabileResidues();
		TypePattern avail_labiles = structure.getDetachedLabilesPattern();
		Glycan fragment = getZFragment(current, structure.getMassOptions()
				.removeExchanges());
		if (fragment == null)
			return new LinkedList<Glycan>();
		return Glycan.getAllLabilesConfigurations(fragment
				.detachLabileResidues(), avail_labiles);
	}

	/**
	 * Return the A-type fragment resulting from the cleavage of the given
	 * structure at the specified residue.
	 * 
	 * @param type
	 *            the type of A fragment to be computed
	 * @see CrossRingFragmentDictionary#getCrossRingFragmentTypesA(Residue)
	 */
	public Glycan getAFragment(Glycan structure, Residue current,
			CrossRingFragmentType type) {
		if (!canDoRingFragment(structure, current))
			return null;
		return getAFragment(current, type, false, structure.getMassOptions()
				.removeExchanges());
	}

	protected Glycan getAFragment(Residue current, CrossRingFragmentType type,
			boolean allow_virtual_charges, MassOptions mass_opt) {

		Residue a_root = new Residue(type);
		a_root.setCleavedResidue(current.cloneResidue());

		// an A fragment must contain no bonds to its parent
		if (!internal_fragments
				&& type.anyValidPosition(current.getParentLinkage()
						.getChildPositions()))
			return null;

		// check which children are in
		for (Linkage l : current.getChildrenLinkages()) {
			if (type.areValidPositions(l.getParentPositions()))
				a_root.addChild(l.getChildResidue().cloneSubtree(), l
						.getBonds());
		}

		// no ring fragment next to a glycosidic cleavage
		if (!internal_fragments && a_root.hasGlycosidicCleavages())
			return null;

		// a glycan must contain at least one saccharide or a charge (in
		// negative mode)
		Glycan a_frag = new Glycan(a_root, false, mass_opt);
		if ((!a_frag.isSmallRingFragment() && !a_root.hasRingFragments())
				|| (small_ring_fragments == true && a_frag
				.countCharges(allow_virtual_charges) > 0))
			return a_frag;
		return null;
	}

	/**
	 * Return the B-type fragment resulting from the cleavage of the given
	 * structure at the specified residue.
	 */
	public Glycan getBFragment(Glycan structure, Residue current) {
		if (!canDoCleavage(structure, current))
			return null;
		return getBFragment(current, structure.getMassOptions()
				.removeExchanges());
	}

	protected Glycan getBFragment(Residue current, MassOptions mass_opt) {
		Residue b_frag = current.cloneSubtree();
		Residue b_root = ResidueDictionary.createBCleavage();
		b_root.setCleavedResidue(current.getParent().cloneResidue());
		b_root.addChild(b_frag, current.getParentLinkage().getBonds());

		// a glycan must contain at least one saccharide
		if (!b_root.hasSaccharideChildren())
			return null;
		return new Glycan(b_root, false, mass_opt);
	}

	/**
	 * Return the C-type fragment resulting from the cleavage of the given
	 * structure at the specified residue.
	 */
	public Glycan getCFragment(Glycan structure, Residue current) {
		if (!canDoCleavage(structure, current))
			return null;
		return getCFragment(current, structure.getMassOptions()
				.removeExchanges());
	}

	protected Glycan getCFragment(Residue current, MassOptions mass_opt) {

		Residue c_frag = current.cloneSubtree();
		Residue c_root = ResidueDictionary.createCCleavage();
		c_root.setCleavedResidue(current.getParent().cloneResidue());
		c_root.addChild(c_frag, current.getParentLinkage().getBonds());

		// a glycan must contain at least one saccharide
		if (!c_root.hasSaccharideChildren())
			return null;
		return new Glycan(c_root, false, mass_opt);
	}

	/**
	 * Return the X-type fragment resulting from the cleavage of the given
	 * structure at the specified residue.
	 * 
	 * @param type
	 *            the type of X fragment to be computed
	 * @see CrossRingFragmentDictionary#getCrossRingFragmentTypesX(Residue)
	 */
	public Glycan getXFragment(Glycan structure, Residue current,
			CrossRingFragmentType type) {
		if (!canDoRingFragment(structure, current))
			return null;
		return getXFragment(current, type, false, structure.getMassOptions()
				.removeExchanges());
	}

	public Glycan getXFragment(Residue current, CrossRingFragmentType type,
			boolean allow_virtual_charges, MassOptions mass_opt) {

		//
		Residue x_leaf = new Residue(type);
		x_leaf.setCleavedResidue(current.cloneResidue());

		// a X fragment must contain all the bonds to its parent
		if (!type.areValidPositions(current.getParentLinkage()
				.getChildPositions()))
			return null;

		// check which children are in
		for (Linkage l : current.getChildrenLinkages()) {
			if (type.areValidPositions(l.getParentPositions()))
				x_leaf.addChild(l.getChildResidue().cloneSubtree(), l
						.getBonds());
		}

		// an X fragment cannot have children (no internal fragments)
		if (!internal_fragments && x_leaf.hasSaccharideChildren())
			return null;

		// create tree structure
		Glycan x_frag = new Glycan(current.getTreeRoot().cloneSubtree(current,
				x_leaf), false, mass_opt);

		// no ring fragment next to a glycosidic cleavage
		if (!internal_fragments && x_leaf.hasGlycosidicCleavages())
			return null;

		// a glycan must contain at least one saccharide or a charge (in
		// negative mode)
		if ((!x_frag.isSmallRingFragment() && !x_leaf.hasRingFragments())
				|| (small_ring_fragments == true && x_frag
				.countCharges(allow_virtual_charges) > 0))
			return x_frag;
		return null;
	}

	/**
	 * Return the Y-type fragment resulting from the cleavage of the given
	 * structure at the specified residue.
	 */
	public Glycan getYFragment(Glycan structure, Residue current) {
		if (!canDoCleavage(structure, current))
			return null;
		return getYFragment(current, structure.getMassOptions()
				.removeExchanges());
	}

	protected Glycan getYFragment(Residue current, MassOptions mass_opt) {

		Residue y_leaf = ResidueDictionary.createYCleavage();
		y_leaf.setCleavedResidue(current.cloneResidue());
		Glycan ret = new Glycan(current.getTreeRoot().cloneSubtree(current,
				y_leaf), false, mass_opt);

		// a glycan must contain at least one saccharide
		if (!y_leaf.getParent().isSaccharide())
			return null;
		return ret;
	}

	/**
	 * Return the Z-type fragment resulting from the cleavage of the given
	 * structure at the specified residue.
	 */
	public Glycan getZFragment(Glycan structure, Residue current) {
		if (!canDoCleavage(structure, current))
			return null;
		return getZFragment(current, structure.getMassOptions()
				.removeExchanges());
	}

	protected Glycan getZFragment(Residue current, MassOptions mass_opt) {

		Residue z_leaf = ResidueDictionary.createZCleavage();
		z_leaf.setCleavedResidue(current.cloneResidue());
		Glycan ret = new Glycan(current.getTreeRoot().cloneSubtree(current,
				z_leaf), false, mass_opt);

		// a glycan must contain at least one saccharide
		if (!z_leaf.getParent().isSaccharide())
			return null;
		return ret;
	}

	/**
	 * Return the fragment resulting from the cleavage of the labile residue
	 * <code>current</code> from the given structure.
	 */
	public Glycan getLFragment(Glycan structure, Residue current) {
		if (!canDoCleavage(structure, current))
			return null;
		return getLFragment(current, structure.getMassOptions()
				.removeExchanges());
	}

	protected Glycan getLFragment(Residue current, MassOptions mass_opt) {

		Residue l_leaf = ResidueDictionary.createLCleavage();
		l_leaf.setCleavedResidue(current.cloneResidue());
		Glycan ret = new Glycan(current.getTreeRoot().cloneSubtree(current,
				l_leaf), false, mass_opt);

		// a glycan must contain at least one saccharide
		if (!l_leaf.getParent().isSaccharide())
			return null;
		return ret;
	}

	/**
	 * Return a representation of the type of fragment represented by this
	 * structure as a string formed by the cleavage types. Return the empty
	 * string if no cleavages are present
	 * 
	 * @see Residue#getCleavageType
	 */
	static public String getFragmentType(Glycan structure) {
		if (structure == null)
			return "";

		return getFragmentType(structure.getRoot());
	}

	static protected String getFragmentType(Residue node) {
		if (node == null)
			return "";

		String type = "";
		if (node.isCleavage() && !node.isLCleavage()) {
			type += node.getCleavageType();
			if (node.isRingFragment())
				type += "_{" + node.getCleavedResidue().getTypeName() + "}";
		}
		for (Linkage l : node.getChildrenLinkages())
			type += getFragmentType(l.getChildResidue());
		return type;
	}

}
