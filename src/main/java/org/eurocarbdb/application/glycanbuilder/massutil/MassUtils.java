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

package org.eurocarbdb.application.glycanbuilder.massutil;

import java.util.*;
import java.io.*;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

/**
 * Utility class holding the information about atoms and isotopes.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class MassUtils {

	// ---- static variables

	private static HashMap<String, Atom> atoms;
	private static HashMap<Atom, Isotope> atom_main_isotope;
	private static HashMap<Atom, ArrayList<Isotope>> atom_other_isotopes;
	private static HashMap<Atom, ArrayList<Isotope>> atom_all_isotopes;

	/** An atom of hydrogen. */
	public static Atom hydrogen;

	/** An electon. */
	public static Atom electron;

	/** A molecule of water */
	public static Molecule water;

	/** A methyl group. */
	public static Molecule methyl;
	
	/** A C^13 methyl, see http://cell.ccrc.uga.edu/~pierce/alvarez_isotop_paper.pdf */
	public static Molecule heavyMethyl;

	/** A deutero-methyl group. */
	public static Molecule dmethyl;

	/** An acetyl group. */
	public static Molecule acetyl;

	/** A deutero-acetyl group. */
	public static Molecule dacetyl;

	/** A proton. */
	public static Molecule h_ion;

	/** A lithium ion. */
	public static Molecule li_ion;

	/** A sodium ion. */
	public static Molecule na_ion;

	/** A potassiom ion. */
	public static Molecule k_ion;

	/** A chlorine ion. */
	public static Molecule cl_ion;

	/** A H2PO4 ion */
	public static Molecule h2po4_ion;
	
	static {
		atoms = new HashMap<String, Atom>();
		atom_main_isotope = new HashMap<Atom, Isotope>();
		atom_other_isotopes = new HashMap<Atom, ArrayList<Isotope>>();
		atom_all_isotopes = new HashMap<Atom, ArrayList<Isotope>>();

		loadAtoms("/conf/atoms");
		loadIsotopes("/conf/isotopes");

		try {
			water = new Molecule("H2O");
			hydrogen = getAtom("H");
			electron = getAtom("e");

			methyl = new Molecule("CH3");
			dmethyl = new Molecule("CD3");
			acetyl = new Molecule("C2H3O");
			dacetyl = new Molecule("C2D3O");
			
			heavyMethyl = new Molecule("C^13H3");

			h_ion = new Molecule("H+");
			li_ion = new Molecule("Li+");
			na_ion = new Molecule("Na+");
			k_ion = new Molecule("K+");
			cl_ion = new Molecule("Cl-");
			
			h2po4_ion = new Molecule("H2PO4-");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private MassUtils() {
	}

	/**
	 * Load all information about atoms from a configuration file.
	 */
	private static void loadAtoms(String filename) {
		try {
			// open file
			java.net.URL file_url = MassUtils.class.getResource(filename);
			if (file_url == null)
				throw new FileNotFoundException(filename);
			BufferedReader is = new BufferedReader(new InputStreamReader(
					file_url.openStream()));

			// read dictionary
			String line;
			while ((line = is.readLine()) != null) {
				line = TextUtils.trim(line);
				if (line.length() > 0 && !line.startsWith("%")) {
					Atom atom = new Atom(line);
					atoms.put(atom.getSymbol(), atom);
				}
			}

			is.close();
		} catch (Exception e) {
			LogUtils.report(e);
			atoms.clear();
		}
	}

	/**
	 * Load all information about isotopes from a configuration file.
	 */
	private static void loadIsotopes(String filename) {
		try {
			// open file
			java.net.URL file_url = MassUtils.class.getResource(filename);
			if (file_url == null)
				throw new FileNotFoundException(filename);
			BufferedReader is = new BufferedReader(new InputStreamReader(
					file_url.openStream()));

			// read dictionary
			String line;
			while ((line = is.readLine()) != null) {
				line = TextUtils.trim(line);
				if (line.length() > 0 && !line.startsWith("%")) {
					Isotope isotope = new Isotope(line);

					if (atoms.get(isotope.getAtomSymbol()) == null)
						throw new Exception("Invalid atom name: "
								+ isotope.getAtomSymbol());

					if (isotope.isStable()) {
						Atom atom = atoms.get(isotope.getAtomSymbol());
						if (atom_all_isotopes.get(atom) == null)
							atom_all_isotopes.put(atom, new ArrayList<Isotope>());
						atom_all_isotopes.get(atom).add(isotope);
						
						Atom isotopeAtomObj=new Atom(atom.getSymbol()+"^"+isotope.getAtomicNumber()+"\t"+atom.getName()+"("+isotope.getAtomicNumber()+")"+"\t"+isotope.getMass()+"\t"+isotope.getMass());
						atoms.put(isotopeAtomObj.getSymbol(), isotopeAtomObj);
					}
				}
			}

			// classify isotopes
			for (Map.Entry<Atom, ArrayList<Isotope>> e : atom_all_isotopes
					.entrySet()) {
				Atom atom = e.getKey();
				ArrayList<Isotope> isotopes = e.getValue();

				// find main isotope
				Isotope main_isotope = isotopes.get(0);
				for (Isotope isotope : isotopes) {
					if (isotope.getAbundance() > main_isotope.getAbundance())
						main_isotope = isotope;
				}
				atom_main_isotope.put(atom, main_isotope);

				// collect other isotopes
				ArrayList<Isotope> other_isotopes = new ArrayList<Isotope>();
				for (Isotope isotope : isotopes) {
					if (isotope != main_isotope)
						other_isotopes.add(isotope);
				}
				atom_other_isotopes.put(atom, other_isotopes);

				// put main isotope in first position
				ArrayList<Isotope> all_isotopes = atom_all_isotopes.get(atom);
				all_isotopes.remove(main_isotope);
				all_isotopes.add(0, main_isotope);
			}

			is.close();
		} catch (Exception e) {
			LogUtils.report(e);
			atoms.clear();
		}
	}

	// --- methods

	/**
	 * Return the atom of type <code>atom_name</code>.
	 * 
	 * @throws Exception
	 *             if the atom do not exists
	 */
	public static Atom getAtom(String atom_name) throws Exception {
		Atom ret = atoms.get(atom_name);
		if (ret == null)
			throw new Exception("Invalid atom " + atom_name);
		return ret;
	}

	/**
	 * Return the main isotope for the atom <code>atom</code>.
	 */
	public static Isotope getMainIsotope(Atom atom) {
		if (atom == null)
			return null;
		return atom_main_isotope.get(atom);
	}

	/**
	 * Return all possible isotopes for the atom <code>atom</code>.
	 */
	public static ArrayList<Isotope> getAllIsotopes(Atom atom) {
		if (atom == null)
			return null;
		return atom_all_isotopes.get(atom);
	}
}