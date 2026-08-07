package org.glycoinfo.application.glycanbuilder.dataset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The substituents a residue owns by virtue of its name, for the residues where the tables outside
 * this project get them wrong.
 *
 * <p>Which substituents belong to a trivial name is answered by two separate tables in
 * glycanformatconverter, and they disagree with each other and with our own dictionary. Muramic acid
 * is where it shows. MurNAc and MurNGc are answered by one of them, which names the group on carbon
 * 3 "(R)Lac" - a substituent we hold. Plain Mur is answered by the other, which calls the same group
 * "(R)CE", a name nothing here holds; asking the dictionary for it does not fail either, it quietly
 * returns a placeholder that is not a substituent, and the exporter then looks for a backbone that
 * was never built. That entry also leaves out the 2-amino, so resolving the name alone would have
 * Mur write 3-O-lactyl-glucose: a valid string for the wrong molecule.
 *
 * <p>PubChem CID 441038 gives muramic acid as
 * (2R)-2-[(3R,4R,5S,6R)-3-amino-2,5-dihydroxy-6-(hydroxymethyl)oxan-4-yl]oxypropanoic acid - the
 * amino on the sugar's C2, the lactyl ether on its C3 - which is what this table says.
 *
 * <p>Both directions read this same table. The exporter uses it to know which substituents not to
 * ask the drawing for, and the importer to know which ones not to draw, and they have to agree or a
 * residue is written with its own substituents twice over.
 */
public class NativeSubstituentDictionary {

	private static final Map<String, String> OWN_SUBSTITUENTS;
	static {
		Map<String, String> substituents = new HashMap<String, String>();
		substituents.put("Mur", "2*N_3*(R)Lac");
		OWN_SUBSTITUENTS = Collections.unmodifiableMap(substituents);
	}

	/**
	 * The substituents this residue owns, written as the exporter's notation - "2*N_3*(R)Lac" - or
	 * null when this project has nothing to say about the residue and the outside tables should be
	 * asked instead.
	 */
	public static String forResidueName(String _residueName) {
		return OWN_SUBSTITUENTS.get(_residueName);
	}

	/** Whether this residue owns the substituent named at that position, e.g. "3*(R)Lac". */
	public static boolean owns(String _residueName, String _substituentNotation) {
		String substituents = forResidueName(_residueName);
		return substituents != null && substituents.contains(_substituentNotation);
	}
}
