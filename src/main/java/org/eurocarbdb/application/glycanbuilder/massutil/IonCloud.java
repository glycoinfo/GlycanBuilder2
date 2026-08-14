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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.util.Combinator;
import org.eurocarbdb.util.MutableInteger;

/**
 * Manages a collection of charges that will be associated to a glycan
 * structure. The identity of the possible ions is defined in
 * {@link MassOptions}
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class IonCloud {

	protected TreeMap<String, Integer> ions;
	protected TreeMap<String, Double> ionNameToChargeMass;
	protected int ionsNum;
	protected int ionsRelCount;
	protected double ionsTotalMass;

	/**
	 * Empty constructor
	 */
	public IonCloud() {
		ions = new TreeMap<String, Integer>();
		ionNameToChargeMass = new TreeMap<String, Double>();
		ionsNum = 0;
		ionsRelCount = 0;
		ionsTotalMass = 0.;
	}

	/**
	 * Create a new object from an initialization string
	 * 
	 * @see #initFromString
	 */
	public IonCloud(String init) {
		this();

		try {
			initFromString(init);
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}

	// methods

	public boolean equals(Object other) {
		if (!(other instanceof IonCloud))
			return false;
		return this.toString().equals(other.toString());
	}

	/**
	 * Compute the mass/charge value of a structure with a certain mass given
	 * the charges in this object
	 * 
	 * @param mass
	 *            the mass of the glycan molecule
	 */
	public double computeMZ(double mass) {
		return computeMZ(mass, false);
	}

	/**
	 * Compute the mass/charge value of a structure with a certain mass given the charges in this
	 * object, weighing the adducts the same way the structure was weighed.
	 *
	 * <p>The adducts used to be whatever they were when the ion was <em>set</em>, which is always
	 * the monoisotopic figure - so an average neutral mass arrived carrying a monoisotopic adduct,
	 * and the m/z was neither (#203). It matters most for potassium and chloride, +0.135 and +0.484
	 * per adduct, and is nil for sodium, which has one stable isotope and would let a test pass
	 * while proving nothing.
	 *
	 * @param mass
	 *            the mass of the glycan molecule
	 * @param average
	 *            whether the adducts are to be averaged too, as {@link MassOptions#isAverage} says
	 */
	public double computeMZ(double mass, boolean average) {
		if (mass <= 0.)
			return mass;
		double ionsMass = getIonsMass(average);
		if (ionsNum == 0)
			return (mass + ionsMass);
		return (mass + ionsMass) / ionsNum;
	}

	/**
	 * Compute the original mass of a structure with a certain mass/charge value
	 * given the charges in this object
	 * 
	 * @param mz
	 *            the mass/charge value of the glycan molecule with the
	 *            associated charges
	 */
	public double computeMass(double mz) {
		return computeMass(mz, false);
	}

	/**
	 * Compute the original mass of a structure with a certain mass/charge value given the charges in
	 * this object, taking the adducts off in the same weighing they were put on in.
	 *
	 * @param mz
	 *            the mass/charge value of the glycan molecule with the associated charges
	 * @param average
	 *            whether the adducts were averaged, as {@link MassOptions#isAverage} says
	 * @see #computeMZ(double, boolean)
	 */
	public double computeMass(double mz, boolean average) {
		if (mz <= 0.)
			return mz;
		double ionsMass = getIonsMass(average);
		if (ionsNum == 0)
			return (mz - ionsMass);

		return (mz * ionsNum - ionsMass);
	}

	/**
	 * Return <code>true</code> if the object represent a set of charges that
	 * could be found associated to a glycan structure
	 */
	public boolean isRealistic() {
		for (Map.Entry<String, Integer> c : ions.entrySet())
			if (!c.getKey().equals(MassOptions.ION_H)
					&& c.getValue().intValue() < 0)
				return false;
		return true;
	}

	/**
	 * Return <code>true</code> if the total charge is negative
	 */
	public boolean isNegative() {
		return (ionsRelCount < 0);
	}

	/**
	 * Return the total number of charges
	 */
	public int getNoCharges() {
		return Math.abs(ionsRelCount);
	}

	/**
	 * Return <code>true</code> if the number of charges is undetermined.
	 */
	public boolean isUndetermined() {
		for (Map.Entry<String, Integer> c : this.ions.entrySet())
			if (c.getValue() == 999)
				return true;
		return false;
	}

	// data access

	/**
	 * Create a copy of the object.
	 */
	public IonCloud clone() {
		IonCloud ret = new IonCloud();

		for (Map.Entry<String, Integer> c : this.ions.entrySet())
			ret.ions.put(c.getKey(), c.getValue());
				
		for (Map.Entry<String, Double> c : this.ionNameToChargeMass.entrySet())
			ret.ionNameToChargeMass.put(c.getKey(), c.getValue());

		ret.ionsNum = this.ionsNum;
		ret.ionsRelCount = this.ionsRelCount;
		ret.ionsTotalMass = this.ionsTotalMass;

		return ret;
	}

	/**
	 * Reset the object to contain no charges
	 */
	public void clear() {
		ions.clear();
		ionsNum = 0;
		ionsRelCount = 0;
		ionsTotalMass = 0.;
	}

	/**
	 * Return a copy of this object to which the content of a second object is
	 * added
	 */
	public IonCloud and(IonCloud other) {
		IonCloud ret = this.clone();
		ret.add(other);
		return ret;
	}

	/**
	 * Add the content of a second object
	 */
	public void add(IonCloud other) {
		if (other == null)
			return;

		for (Map.Entry<String, Integer> c : other.ions.entrySet())
			add(c.getKey(), c.getValue());
	}

	/**
	 * Return a copy of this object to which a new charge has been added
	 */
	public IonCloud and(String charge) {
		return and(charge, 1);
	}

	/**
	 * Return a copy of this object to which a certain quantity of a new charge
	 * has been added
	 */
	public IonCloud and(String charge, int quantity) {
		Molecule adduct = moleculeFor(charge);
		if (adduct == null)
			return this.clone();
		return this.and(charge, adduct.getMass(), quantity);
	}

	/**
	 * Add a certain quantity of a new charge to this object
	 */
	public void add(String charge, int quantity) {
		Molecule adduct = moleculeFor(charge);
		if (adduct != null)
			add(charge, adduct.getMass(), quantity);
	}

	/**
	 * Set the quantity of a specific charge
	 */
	public void set(String charge, int quantity) {
		Molecule adduct = moleculeFor(charge);
		if (adduct != null)
			set(charge, adduct.getMass(), quantity);
	}

	/**
	 * Return a copy of this object to which a certain quantity of a new charge
	 * has been added
	 */
	public IonCloud and(String charge_name, double charge_mass, int quantity) {
		IonCloud ret = this.clone();
		ret.add(charge_name, charge_mass, quantity);
		return ret;
	}

	/**
	 * Add a certain quantity of a new charge to this object
	 */
	public void add(String charge_name, double charge_mass, int quantity) {

		if (quantity == 0)
			return;

		// add to list
		if (ions.containsKey(charge_name))
			ions.put(charge_name, ions.get(charge_name) + quantity);
		else
			ions.put(charge_name, quantity);
		if (ions.get(charge_name) == 0)
			ions.remove(charge_name);

		// update count
		ionsRelCount += quantity;
		ionsNum = Math.abs(ionsRelCount);
		
		ionNameToChargeMass.put(charge_name, charge_mass);

		// update mass
		ionsTotalMass += quantity * charge_mass;
	}
	
	public List<IonCloud> generateCombinations(){
		List<String> ionList=new ArrayList<String>();
		for(String chargeName:ions.keySet()){
			int num=ions.get(chargeName);
			for(int i=0;i<num;i++){
				ionList.add(chargeName);
			}
		}
		
		//LogUtils.report(new Exception("size: "+ionList.toString()));
		
		List<IonCloud> ionClouds=new ArrayList<IonCloud>();
		HashSet<List<String>> ionCombinationsList=new HashSet<List<String>>(); 
		//LogUtils.report(new Exception("size: "+ionCombinationsList.toString()));
		for(List<String> ionCloudContents:ionCombinationsList){
			IonCloud ionCloud=new IonCloud();
			Map<String,MutableInteger> ionNameToQuantity=new HashMap<String,MutableInteger>();
			for(String ionName:ionCloudContents){
				if(ionNameToQuantity.containsKey(ionName)==false){
					ionNameToQuantity.put(ionName, new MutableInteger());
				}
				ionNameToQuantity.get(ionName).mutI++;
			}
			
			for(String ionName:ionNameToQuantity.keySet()){
				int quantity=ionNameToQuantity.get(ionName).mutI;
				
				//LogUtils.report(new Exception(ionName+"|"+quantity));
				
				ionCloud.add(ionName, ionNameToChargeMass.get(ionName), quantity);
			}
			
			ionClouds.add(ionCloud);
		}
		
		return ionClouds;
	}

	/**
	 * Set the quantity of a specific charge
	 */
	public void set(String charge_name, double charge_mass, int quantity) {
		add(charge_name, charge_mass, -get(charge_name));
		add(charge_name, charge_mass, quantity);
	}

	/**
	 * Copy the content of a second object on this one
	 * 
	 * @param skip_undetermined
	 *            <code>true</code> if charges with undetermined quantity should
	 *            be ignored
	 */
	public boolean set(IonCloud other, boolean skip_undetermined) {
		boolean changed = false;
		if (!skip_undetermined || other.get(MassOptions.ION_H) != 999) {
			this.set(MassOptions.ION_H, other.get(MassOptions.ION_H));
			changed = true;
		}
		if (!skip_undetermined || other.get(MassOptions.ION_NA) != 999) {
			this.set(MassOptions.ION_NA, other.get(MassOptions.ION_NA));
			changed = true;
		}
		if (!skip_undetermined || other.get(MassOptions.ION_LI) != 999) {
			this.set(MassOptions.ION_LI, other.get(MassOptions.ION_LI));
			changed = true;
		}
		if (!skip_undetermined || other.get(MassOptions.ION_K) != 999) {
			this.set(MassOptions.ION_K, other.get(MassOptions.ION_K));
			changed = true;
		}
		if (!skip_undetermined || other.get(MassOptions.ION_CL) != 999) {
			this.set(MassOptions.ION_CL, other.get(MassOptions.ION_CL));
			changed = true;
		}
		if (!skip_undetermined || other.get(MassOptions.ION_H2PO4) != 999) {
			this.set(MassOptions.ION_H2PO4, other.get(MassOptions.ION_H2PO4));
			changed = true;
		}

		return changed;
	}

	/**
	 * Return the identities of the charges contained in this object
	 */
	public Vector<String> getIons() {
		Vector<String> ret = new Vector<String>();
		for (Map.Entry<String, Integer> e : this.ions.entrySet())
			ret.add(e.getKey());
		return ret;
	}

	/**
	 * Return the quantity of a specific charge
	 */
	public int get(String charge_name) {
		Integer ret = ions.get(charge_name);
		return (ret == null) ? 0 : ret;
	}

	/**
	 * Merge the content of a second object to this one
	 */
	public void merge(IonCloud other) {
		this.merge(MassOptions.ION_H, other);
		this.merge(MassOptions.ION_NA, other);
		this.merge(MassOptions.ION_LI, other);
		this.merge(MassOptions.ION_K, other);
		this.merge(MassOptions.ION_CL, other);
		this.merge(MassOptions.ION_H2PO4, other);
	}

	/**
	 * Merge the information about a specific charge in a second object to this
	 * one
	 */
	public void merge(String charge_name, IonCloud other) {
		if (other.get(charge_name) == 999)
			this.set(charge_name, 999);
		else if (this.get(charge_name) != other.get(charge_name))
			this.set(charge_name, 999);
	}

	// member access

	/**
	 * Return the total number of ions
	 */
	public int size() {
		return ionsNum;
	}

	/**
	 * Return the total number of ions
	 */
	public int getIonsNum() {
		return ionsNum;
	}

	/**
	 * Return the total mass
	 */
	public double getIonsMass() {
		return ionsTotalMass;
	}

	/**
	 * The total mass of the charges in this object, weighed monoisotopically or on average.
	 *
	 * <p>Recomputed from the adducts rather than read off {@link #ionsTotalMass}, which is whatever
	 * was passed when each ion was set and is therefore always monoisotopic for the six named
	 * adducts (#203).
	 *
	 * <p>A charge added with an explicit mass - a neutral exchange, say - keeps the mass it was
	 * given. There is no second figure to reach for, and inventing one would be worse than using the
	 * one the caller supplied.
	 *
	 * @param average
	 *            whether to take each known adduct's average mass rather than its monoisotopic one
	 */
	public double getIonsMass(boolean average) {
		if (!average)
			return ionsTotalMass;

		double total = 0.;
		for (Map.Entry<String, Integer> e : this.ions.entrySet()) {
			Molecule adduct = moleculeFor(e.getKey());
			double mass = (adduct != null) ? adduct.getAverageMass()
					: ionNameToChargeMass.getOrDefault(e.getKey(), 0.);
			total += e.getValue() * mass;
		}
		return total;
	}

	/**
	 * The molecule for one of the named adducts, or <code>null</code> for a charge this class does
	 * not know by name.
	 *
	 * <p>One place, because adding a seventh adduct used to mean finding six copies of this chain.
	 */
	private static Molecule moleculeFor(String charge_name) {
		if (MassOptions.ION_H.equals(charge_name))
			return MassUtils.h_ion;
		if (MassOptions.ION_LI.equals(charge_name))
			return MassUtils.li_ion;
		if (MassOptions.ION_NA.equals(charge_name))
			return MassUtils.na_ion;
		if (MassOptions.ION_K.equals(charge_name))
			return MassUtils.k_ion;
		if (MassOptions.ION_CL.equals(charge_name))
			return MassUtils.cl_ion;
		if (MassOptions.ION_H2PO4.equals(charge_name))
			return MassUtils.h2po4_ion;
		return null;
	}

	/**
	 * Return a map containing the identities and quantities of the charges in
	 * this object
	 */
	public Map<String, Integer> getIonsMap() {
		return ions;
	}

	/**
	 * Convert this object to a {@link Molecule} object containing the same
	 * information
	 */
	public Molecule getMolecule() throws Exception {
		Molecule ret = new Molecule();
		for (Map.Entry<String, Integer> e : this.ions.entrySet()) {
			Molecule adduct = moleculeFor(e.getKey());
			if (adduct != null)
				ret.add(adduct, e.getValue());
		}
		return ret;
	}

	// serialization

	/**
	 * Create a new object from its string representation.
	 * 
	 * @throws Exception
	 *             if the string is in the wrong format
	 * @see #initFromString
	 */
	static public IonCloud fromString(String str) throws Exception {
		IonCloud ret = new IonCloud();
		ret.initFromString(str);
		return ret;
	}

	/**
	 * Create a new object from its string representation. The string must
	 * contain either 0 or a mathematical formula specifying the identity and
	 * quantity of the charges (Example: 2Na-2H)
	 * 
	 * @throws Exception
	 *             if the string is in the wrong format
	 * @see #initFromString
	 */
	public void initFromString(String str) throws Exception {
		clear();
		if (str == null || str.length() == 0 || str.equals("0"))
			return;

		char[] str_buffer = str.toCharArray();
		for (int i = 0; i < str_buffer.length;) {
			StringBuilder count = new StringBuilder();
			StringBuilder ion = new StringBuilder();

			// read sign
			if (str_buffer[i] == '+' || str_buffer[i] == '-') {
				if (str_buffer[i] == '-')
					count.append(str_buffer[i]);
				i++;
			}
			if (i == str_buffer.length)
				throw new Exception("Invalid string format: <" + str + ">");

			// read count
			if (Character.isDigit(str_buffer[i])) {
				for (; i < str_buffer.length
						&& Character.isDigit(str_buffer[i]); i++)
					count.append(str_buffer[i]);
			} else 
				count.append('1');

			if (i == str_buffer.length)
				throw new Exception("Invalid string format: <" + str + ">");

			//Changed from isLetter to isLetterOrDigit, 03/08/11 David R. Damerell (david@nixbioinf.org)
			if (Character.isLetterOrDigit(str_buffer[i])) {
				for (; i < str_buffer.length
						&& Character.isLetterOrDigit(str_buffer[i]); i++)
					ion.append(str_buffer[i]);
			} else
				throw new Exception("Invalid string format: <" + str + ">");
			
			this.add(ion.toString(), Integer.valueOf(count.toString()));
		}
	}

	/**
	 * Return a string representation of this object
	 * 
	 * @see #initFromString
	 */
	public String toString() {

		StringBuilder sb = new StringBuilder();

		// first positives then negatives
		for (Map.Entry<String, Integer> entry : ions.entrySet()) {
			if (entry.getValue() > 0) {
				if (sb.length() > 0)
					sb.append('+');
				if (entry.getValue() > 1)
					sb.append(entry.getValue());
				sb.append(entry.getKey());
			}
		}

		for (Map.Entry<String, Integer> entry : ions.entrySet()) {
			if (entry.getValue() < 0) {
				if (entry.getValue() == -1)
					sb.append('-');
				else
					sb.append(entry.getValue());
				sb.append(entry.getKey());
			}
		}

		if (sb.length() == 0)
			sb.append('0');

		return sb.toString();
	}
}