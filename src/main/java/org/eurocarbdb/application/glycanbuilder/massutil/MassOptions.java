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

import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Configuration;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;



/**
 * Contains all the possible modifiers that could apply to a glycan molecule
 * without changing the number and identity of residues.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */

public class MassOptions {

	// mass computation options

	/** Main isotope option */
	public static final String ISOTOPE_MONO = "MONO";
	/** Average isotope option */
	public static final String ISOTOPE_AVG = "AVG";
	/** Select which type of mass to compute */
	public String ISOTOPE = ISOTOPE_MONO;

	/** Structure is underivatized */
	public static final String NO_DERIVATIZATION = "Und";
	/** Structure is permethylated */
	public static final String PERMETHYLATED = "perMe";
	/** Structure is heavy permethylated */
	public static final String HEAVYPERMETHYLATION = "perMe(C^13)";
	/** Structure is perdeuteromethylated */
	public static final String PERDMETHYLATED = "perDMe";
	/** Structure is peracetylated */
	public static final String PERACETYLATED = "perAc";
	/** Structure is perdeuteroacetylated */
	public static final String PERDACETYLATED = "perDAc";
	/** List of possible persubstitutions */
	public static final String[] DERIVATIZATIONS = new String[] {
			NO_DERIVATIZATION, PERMETHYLATED, HEAVYPERMETHYLATION, PERDMETHYLATED, PERACETYLATED,
			PERDACETYLATED };

	/** Structure persubstitution */
	public String DERIVATIZATION = PERMETHYLATED;

	/** Reducing end modification */
	public ResidueType REDUCING_END_TYPE = ResidueType.createFreeReducingEnd();

	/**
	 * List of charges associated to the glycan molecule due to the ionization
	 * process in the mass spectrometer
	 */
	public IonCloud ION_CLOUD = new IonCloud("Na");

	/**
	 * List of neutral exchanges between charges present on the glycan molecule
	 * due to acidic groups and exogenous ions
	 */
	public IonCloud NEUTRAL_EXCHANGES = new IonCloud();

	// constants

	/** Empty ion */
	public static final String NO_ION = "0";
	/** H+ ion */
	public static final String ION_H = "H";
	/** Li+ ion */
	public static final String ION_LI = "Li";
	/** Na+ ion */
	public static final String ION_NA = "Na";
	/** K+ ion */
	public static final String ION_K = "K";

	public static final String ION_CL = "Cl";
	
	public static final String ION_H2PO4 = "H2PO4";
	

	// pojo

	/**
	 * Return the type of mass that will be computed.
	 */
	public String getIsotope() {
		return ISOTOPE;
	}

	/**
	 * Set the type of mass that will be computed.
	 */
	public void setIsotope(String s) {
		ISOTOPE = s;
	}

	/**
	 * Return the type of persubstitution applied to the structure.
	 */
	public String getDerivatization() {
		return DERIVATIZATION;
	}

	/**
	 * Set the type of persubstitution applied to the structure.
	 */
	public void setDerivatization(String s) {
		DERIVATIZATION = s;
	}

	/**
	 * Return the type of reducing end modification applied to the structure.
	 */
	public ResidueType getReducingEndType() {
		return REDUCING_END_TYPE;
	}

	/**
	 * Set the type of reducing end modification applied to the structure.
	 */
	public void setReducingEndType(ResidueType redend) {
		REDUCING_END_TYPE = redend;
	}

	/**
	 * Set the type of reducing end modification applied to the structure.
	 * 
	 * @see ResidueDictionary#getResidueType
	 */
	public void setReducingEndTypeString(String type) throws Exception {
		REDUCING_END_TYPE = ResidueDictionary.getResidueType(type);
	}

	/**
	 * Return the identifier of the type of reducing end modification applied to
	 * the structure.
	 * 
	 * @see ResidueType#getName
	 */
	public String getReducingEndTypeString() {
		return REDUCING_END_TYPE.getName();
	}

	// methods

	/**
	 * Create new object using the default settings.
	 */
	public MassOptions() {
	}

	/**
	 * Create new object with a specific type of persubstitution.
	 */
	public MassOptions(String deriv) {
		DERIVATIZATION = deriv;
		REDUCING_END_TYPE = ResidueType.createFreeReducingEnd();
	}

	/**
	 * Create new object with a specific type of persubstitution and reducing
	 * end modification.
	 */
	public MassOptions(String deriv, String marker) {
		DERIVATIZATION = deriv;

		REDUCING_END_TYPE = ResidueDictionary.findResidueType(marker);
		if (REDUCING_END_TYPE == null)
			REDUCING_END_TYPE = ResidueType.createFreeReducingEnd();
	}

	/**
	 * Create new object with a specific type of persubstitution and reducing
	 * end modification.
	 */
	public MassOptions(String deriv, ResidueType marker) {
		DERIVATIZATION = deriv;

		REDUCING_END_TYPE = marker;
		if (REDUCING_END_TYPE == null)
			REDUCING_END_TYPE = ResidueType.createFreeReducingEnd();
	}

	/**
	 * Used to create an object where all values undetermined.
	 */
	public MassOptions(boolean undetermined) {
		if (undetermined) {
			ISOTOPE = "---";

			DERIVATIZATION = "---";
			REDUCING_END_TYPE = null;

			ION_CLOUD.set(MassOptions.ION_H, 999);
			ION_CLOUD.set(MassOptions.ION_NA, 999);
			ION_CLOUD.set(MassOptions.ION_LI, 999);
			ION_CLOUD.set(MassOptions.ION_K, 999);
			ION_CLOUD.set(MassOptions.ION_CL, 999);
			ION_CLOUD.set(MassOptions.ION_H2PO4, 999);

			NEUTRAL_EXCHANGES.set(MassOptions.ION_H, 999);
			NEUTRAL_EXCHANGES.set(MassOptions.ION_NA, 999);
			NEUTRAL_EXCHANGES.set(MassOptions.ION_LI, 999);
			NEUTRAL_EXCHANGES.set(MassOptions.ION_K, 999);
			NEUTRAL_EXCHANGES.set(MassOptions.ION_H2PO4, 999);
		}
	}

	/**
	 * Create an object representing a situation where no modifications are
	 * applied to the structure.
	 */
	public static MassOptions empty() {
		MassOptions ret = new MassOptions();

		ret.DERIVATIZATION = NO_DERIVATIZATION;
		ret.REDUCING_END_TYPE = ResidueType.createFreeReducingEnd();

		ret.ION_CLOUD = new IonCloud();
		ret.NEUTRAL_EXCHANGES = new IonCloud();

		return ret;
	}

	/**
	 * Return <code>true</code> if the object contains some undetermined values.
	 */
	public boolean isUndetermined() {
		return (ISOTOPE.equals("---") || DERIVATIZATION.equals("---")
				|| REDUCING_END_TYPE == null || ION_CLOUD.isUndetermined() || NEUTRAL_EXCHANGES
					.isUndetermined());
	}

	/*
	 * public void updateIsotope() { if( ISOTOPE.equals(ISOTOPE_MONO) ) { REDEND
	 * = REDEND_MONO; METHYL = METHYL_MONO; DMETHYL = DMETHYL_MONO; ACETYL =
	 * ACETYL_MONO; DACETYL = DACETYL_MONO; H_ION = H_ION_MONO; NA_ION =
	 * NA_ION_MONO; LI_ION = LI_ION_MONO; K_ION = K_ION_MONO; H = H_MONO; C =
	 * C_MONO; N = N_MONO; O = O_MONO; H2O = H2O_MONO; NH3 = NH3_MONO; CO2 =
	 * CO2_MONO; } else { REDEND = REDEND_AVG; METHYL = METHYL_AVG; DMETHYL =
	 * DMETHYL_AVG; ACETYL = ACETYL_AVG; DACETYL = DACETYL_AVG; H_ION =
	 * H_ION_AVG; NA_ION = NA_ION_AVG; LI_ION = LI_ION_AVG; K_ION = K_ION_AVG; H
	 * = H_AVG; C = C_AVG; N = N_AVG; O = O_AVG; H2O = H2O_AVG; NH3 = NH3_AVG;
	 * CO2 = CO2_AVG; } }
	 */

	/**
	 * Return a copy of this object.
	 */
	public MassOptions clone() {
		MassOptions ret = new MassOptions();

		ret.ISOTOPE = this.ISOTOPE;

		ret.DERIVATIZATION = this.DERIVATIZATION;
		ret.REDUCING_END_TYPE = this.REDUCING_END_TYPE;

		ret.ION_CLOUD = this.ION_CLOUD.clone();
		ret.NEUTRAL_EXCHANGES = this.NEUTRAL_EXCHANGES.clone();
		// ret.updateIsotope();

		return ret;
	}

	/**
	 * Reset the neutral exchanges.
	 */
	public MassOptions removeExchanges() {
		MassOptions ret = this.clone();
		ret.NEUTRAL_EXCHANGES = new IonCloud();
		return ret;
	}

	/**
	 * Merge two sets of options. If some values are different in the two sets
	 * they are set to undetermined. Charges and neutral exchanges are merged.
	 * 
	 * @see IonCloud#merge
	 */
	public void merge(MassOptions other) {
		if (!other.ISOTOPE.equals(this.ISOTOPE))
			this.ISOTOPE = "---";

		if (!other.DERIVATIZATION.equals(this.DERIVATIZATION))
			this.DERIVATIZATION = "---";

		if (other.REDUCING_END_TYPE == null
				|| (this.REDUCING_END_TYPE != null && !other.REDUCING_END_TYPE
						.getName().equals(this.REDUCING_END_TYPE.getName())))
			this.REDUCING_END_TYPE = null;

		this.ION_CLOUD.merge(other.ION_CLOUD);
		this.NEUTRAL_EXCHANGES.merge(other.NEUTRAL_EXCHANGES);
	}

	/**
	 * Retrieve the reducing end modification from the structure.
	 */
	public void synchronize(Glycan structure) {
		Residue redend = structure.getRoot();
		if (redend != null && redend.isReducingEnd() && !redend.isCleavage())
			REDUCING_END_TYPE = redend.getType();
	}

	/**
	 * Copy the values from another object. Ignore the undetermined values.
	 */
	public boolean setValues(MassOptions options) {
		if (options == null)
			return false;

		boolean changed = false;
		if (!options.ISOTOPE.equals("---")) {
			this.ISOTOPE = options.ISOTOPE;
			changed = true;
		}

		if (!options.DERIVATIZATION.equals("---")) {
			this.DERIVATIZATION = options.DERIVATIZATION;
			changed = true;
		}

		if (options.REDUCING_END_TYPE != null) {
			this.REDUCING_END_TYPE = options.REDUCING_END_TYPE;
			changed = true;
		}

		changed |= this.ION_CLOUD.set(options.ION_CLOUD, true);
		changed |= this.NEUTRAL_EXCHANGES.set(options.NEUTRAL_EXCHANGES, true);

		return changed;
	}

	/**
	 * Return <code>true</code> if the two objects contains the same value.
	 */
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof MassOptions))
			return false;

		return this.toString().equals(other.toString());
	}

	/**
	 * Generate an hash code for this object using its string representation.
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Create a string representation of the object.
	 */

	public String toString() {
		return ISOTOPE + "," + DERIVATIZATION + "," + ION_CLOUD + ","
				+ NEUTRAL_EXCHANGES + "," + REDUCING_END_TYPE.getName();
	}

	/**
	 * Create a new object by parsing it from its string representation.
	 * 
	 * @throws Exception
	 *             if the string is in the wrong format
	 */
	public static MassOptions fromString(String str) throws Exception {
		LinkedList<String> tokens = TextUtils.tokenize(str, ",");

		MassOptions ret = new MassOptions();
		ret.ISOTOPE = tokens.get(0);
		ret.DERIVATIZATION = tokens.get(1);
		ret.ION_CLOUD = IonCloud.fromString(tokens.get(2));
		ret.NEUTRAL_EXCHANGES = IonCloud.fromString(tokens.get(3));
		if (tokens.size() > 4)
			ret.REDUCING_END_TYPE = ResidueDictionary.findResidueType(tokens
					.get(4));

		// ret.updateIsotope();

		return ret;
	}

	/**
	 * Store all mass settings in a configuration object.
	 */
	public void store(Configuration config) {
		config.put("MassOptions", "isotope", ISOTOPE);
		config.put("MassOptions", "derivatization", DERIVATIZATION);
		config.put("MassOptions", "reducing_end_type",
				REDUCING_END_TYPE.getName());
		config.put("MassOptions", "ion_cloud", ION_CLOUD.toString());
		config.put("MassOptions", "neutral_exchanges",
				NEUTRAL_EXCHANGES.toString());
	}

	/**
	 * Retrieve all mass settings from a configuration object.
	 */
	public void retrieve(Configuration config) {
		try {
			ISOTOPE = config.get("MassOptions", "isotope", ISOTOPE);
			DERIVATIZATION = config.get("MassOptions", "derivatization",
					DERIVATIZATION);
			REDUCING_END_TYPE = ResidueDictionary.findResidueType(config.get(
					"MassOptions", "reducing_end_type",
					REDUCING_END_TYPE.getName()));

			ION_CLOUD.initFromString(config.get("MassOptions", "ion_cloud",
					ION_CLOUD.toString()));
			NEUTRAL_EXCHANGES.initFromString(config.get("MassOptions",
					"neutral_exchanges", NEUTRAL_EXCHANGES.toString()));
		} catch (Exception e) {
			LogUtils.report(e);
		}

		// updateIsotope();
	}
}
