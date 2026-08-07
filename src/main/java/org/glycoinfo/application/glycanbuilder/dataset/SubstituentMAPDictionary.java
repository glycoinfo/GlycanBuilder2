package org.glycoinfo.application.glycanbuilder.dataset;

import java.util.HashMap;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.glycoinfo.WURCSFramework.util.map.MAPFactory;

/**
 * The MAP codes of the substituents and bridges this builder knows, indexed by their normalized
 * form.
 *
 * <p>WURCS lets the same group be written more than one way - {@code *OP^XOCCN/3O/3=O} and
 * {@code *OPOCCN/3O/3=O} are the same phosphoethanolamine, and a validator will ask for the second.
 * The dictionaries this builder converts through only recognise one spelling each, so writing the
 * normalized form and reading it back would not round trip.
 *
 * <p>Normalizing both the incoming MAP and every known MAP, and comparing those, removes the
 * question of spelling from the comparison: {@link #toDictionaryForm} turns whatever a WURCS string
 * happens to carry into the one spelling the conversion recognises, which is what makes it safe to
 * emit the normalized form on the way out.</p>
 */
public class SubstituentMAPDictionary {

	/** normalized MAP -> the spelling held in the dictionaries */
	private static Map<String, String> normalizedToDictionary = null;

	/** normalized MAP -> the residue type carrying it */
	private static Map<String, ResidueType> normalizedToResidueType = null;

	/**
	 * The normalized form of a MAP, or the MAP unchanged when it cannot be normalized.
	 * @param _map MAP code to normalize.
	 * @return Returns the normalized MAP code.
	 */
	public static String normalize(String _map) {
		if (_map == null || _map.isEmpty()) return "";

		try {
			MAPFactory factory = new MAPFactory(_map);
			factory.normalize();
			String normalized = factory.getMAPString();
			return (normalized == null || normalized.isEmpty()) ? _map : normalized;
		} catch (Exception unableToNormalize) {
			return _map;
		}
	}

	/**
	 * The form a MAP is written in on the way out.
	 *
	 * <p>Full normalization also rewrites the star indices that say which end of the MAP each
	 * linkage attaches to, and for several bridges the result is a MAP the validator then rejects -
	 * the normalizer suggests it, but WURCS does not accept it. Only the part of the normalization
	 * that is safe to apply is kept: dropping the {@code ^X} that marks a phosphorus of unknown
	 * configuration, which is what a validator flags on every phosphate ester.</p>
	 *
	 * @param _map MAP code to write.
	 * @return Returns the MAP as it should appear in a WURCS string.
	 */
	public static String normalizeForOutput(String _map) {
		if (_map == null || _map.isEmpty()) return _map;

		String normalized = normalize(_map);
		// accept the normalization only where it changed nothing but the phosphorus notation
		return _map.replace("P^X", "P").equals(normalized) ? normalized : _map;
	}

	/**
	 * Rewrites a MAP into the spelling the conversion dictionaries recognise, leaving it alone when
	 * it belongs to no known substituent.
	 * @param _map MAP code as it appears in a WURCS string.
	 * @return Returns the dictionary spelling of the MAP.
	 */
	public static String toDictionaryForm(String _map) {
		if (_map == null || _map.isEmpty()) return _map;

		String dictionaryMAP = getNormalizedToDictionary().get(normalize(_map));
		return (dictionaryMAP == null) ? _map : dictionaryMAP;
	}

	/**
	 * The residue type a MAP stands for, however the MAP happens to be spelled.
	 * @param _map MAP code as it appears in a WURCS string.
	 * @return Returns the residue type, or null when no substituent carries this MAP.
	 */
	public static ResidueType findResidueTypeByMAP(String _map) {
		if (_map == null || _map.isEmpty()) return null;

		return getNormalizedToResidueType().get(normalize(_map));
	}

	private static synchronized Map<String, String> getNormalizedToDictionary() {
		if (normalizedToDictionary == null) build();
		return normalizedToDictionary;
	}

	private static synchronized Map<String, ResidueType> getNormalizedToResidueType() {
		if (normalizedToResidueType == null) build();
		return normalizedToResidueType;
	}

	/** Clears the index so it is rebuilt against a newly loaded dictionary. */
	public static synchronized void clear() {
		normalizedToDictionary = null;
		normalizedToResidueType = null;
	}

	private static void build() {
		Map<String, String> byMAP = new HashMap<String, String>();
		Map<String, ResidueType> byType = new HashMap<String, ResidueType>();

		index(ResidueDictionary.allResidues(), byMAP, byType);
		index(CrossLinkedSubstituentDictionary.getCrossLinkedSubstituents(), byMAP, byType);

		normalizedToDictionary = byMAP;
		normalizedToResidueType = byType;
	}

	private static void index(Iterable<ResidueType> _types, Map<String, String> _byMAP,
			Map<String, ResidueType> _byType) {
		if (_types == null) return;

		for (ResidueType type : _types) {
			String map = type.getMAP();
			if (map == null || map.isEmpty()) continue;

			String key = normalize(map);
			// first one wins, so a substituent and a bridge sharing a MAP keep the substituent
			if (!_byMAP.containsKey(key)) _byMAP.put(key, map);
			if (!_byType.containsKey(key)) _byType.put(key, type);
		}
	}
}
