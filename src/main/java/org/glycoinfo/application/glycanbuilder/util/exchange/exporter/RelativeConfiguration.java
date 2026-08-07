package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import java.util.HashMap;
import java.util.Map;

/**
 * The stereo code of a base type whose absolute configuration is not known.
 *
 * <p>MolecularFramework's base types carry a stereo code for the D and L forms but not for the X
 * form, which is the one written when a residue's configuration is left open. These fifteen are
 * that missing half - one per base type that has an X form - and they are fixed: the stereo of a
 * monosaccharide's carbons is chemistry, not configuration.</p>
 */
class RelativeConfiguration {

	private static final Map<String, String> STEREO_CODES = new HashMap<String, String>();

	static {
		STEREO_CODES.put("xgro", "x");
		STEREO_CODES.put("xthr", "34");
		STEREO_CODES.put("xery", "44");
		STEREO_CODES.put("xara", "344");
		STEREO_CODES.put("xrib", "444");
		STEREO_CODES.put("xlyx", "334");
		STEREO_CODES.put("xxyl", "434");
		STEREO_CODES.put("xall", "4444");
		STEREO_CODES.put("xalt", "3444");
		STEREO_CODES.put("xman", "3344");
		STEREO_CODES.put("xglc", "4344");
		STEREO_CODES.put("xgul", "4434");
		STEREO_CODES.put("xido", "3434");
		STEREO_CODES.put("xtal", "3334");
		STEREO_CODES.put("xgal", "4334");
	}

	/**
	 * The stereo code for a base type of unknown absolute configuration.
	 * @param _baseTypeName Name of the base type, as MolecularFramework spells it.
	 * @return Returns the stereo code, or null when the name is not one of these.
	 */
	static String stereoCodeOf(String _baseTypeName) {
		if(_baseTypeName == null) return null;

		return STEREO_CODES.get(_baseTypeName.toLowerCase());
	}
}
