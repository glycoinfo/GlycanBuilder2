package org.glycoinfo.application.glycanbuilder.dataset;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a monosaccharide's name tells the exporter about it: the stereo of its backbone carbons, the
 * modifications that name implies, and the substituents it owns by being called what it is.
 *
 * <p>This used to be asked of two tables in glycanformatconverter, which disagreed with each other
 * and with our own dictionary. Mur named its group "(R)CE", a name nothing here holds, and left out
 * the 2-amino besides; Api names its branch "3*MeOH", equally unknown here. Beyond being wrong in
 * places, the stereo came back as a parent sugar's name to be looked up in a third table and then
 * resolved against the residue's configuration - and that last step cannot reach the right digits
 * for a sugar whose C3 carries a carbon branch.
 *
 * <p>The stereo is recorded here as digits instead, one string per residue, written for the D form.
 * The other two forms follow from it, which was measured over every residue this builder holds and
 * holds without exception:
 *
 * <ul>
 *   <li>D - the string as recorded</li>
 *   <li>L - every digit swapped, 1 for 2, because an enantiomer inverts every centre</li>
 *   <li>unknown - 1 becomes 3 and 2 becomes 4, the relative form, which says how the carbons stand
 *       in relation to each other without saying which way round the molecule is</li>
 * </ul>
 *
 * <p>Residues marked as not following the residue's configuration keep their digits whatever the
 * drawing says. For the nonulosonates and the heptoses that is the chemistry - their names fix both
 * stereo blocks, so there is nothing left for a D or an L to change. For the dideoxyhexoses and for
 * Kdo and Dha it is inherited behaviour: the table this replaced answered them with a name that
 * already carried its configuration, so flipping the residue never reached them. That is recorded as
 * measured rather than corrected, so that replacing the table changed nothing. Whether it ought to
 * be corrected is a separate question.
 */
public class NativeMonosaccharideDictionary {

	/** What one residue's name says about it. */
	public static class Entry {

		private final String stereo;
		private final boolean followsConfiguration;
		private final char isomer;
		private final String substituents;
		private final String modifications;

		private Entry(String _stereo, boolean _followsConfiguration, char _isomer,
				String _substituents, String _modifications) {
			this.stereo = _stereo;
			this.followsConfiguration = _followsConfiguration;
			this.isomer = _isomer;
			this.substituents = _substituents;
			this.modifications = _modifications;
		}

		/**
		 * The stereo digits for a residue drawn in this configuration.
		 * @param _configuration 'D', 'L', or anything else for an unknown one.
		 */
		public String getStereo(char _configuration) {
			if (!this.followsConfiguration) return this.stereo;
			if (_configuration == 'D') return this.stereo;
			if (_configuration == 'L') return map(this.stereo, '1', '2', '2', '1');
			return map(this.stereo, '1', '3', '2', '4');
		}

		/** The D or L this residue reports as its own, which is not always the drawn one. */
		public char getIsomer() {
			return this.isomer;
		}

		/**
		 * What the name says about the carbons themselves, as "1*A_2*O_3*d" - the acid, the ketone and
		 * the deoxy that make a residue a nonulosonate rather than something the stereo alone
		 * describes. Empty when the name says nothing beyond the stereo.
		 */
		public String getModifications() {
			return this.modifications;
		}

		/** The substituents the name owns, as "2*N_3*(R)Lac", or "" when it owns none. */
		public String getSubstituents() {
			return this.substituents;
		}

		/** Whether the name owns the substituent written at that position, e.g. "3*(R)Lac". */
		public boolean owns(String _substituentNotation) {
			return !this.substituents.isEmpty() && this.substituents.contains(_substituentNotation);
		}

		private static String map(String _stereo, char _from1, char _to1, char _from2, char _to2) {
			StringBuilder mapped = new StringBuilder(_stereo.length());
			for (char digit : _stereo.toCharArray())
				mapped.append(digit == _from1 ? _to1 : digit == _from2 ? _to2 : digit);
			return mapped.toString();
		}
	}

	private static final Map<String, Entry> RESIDUES;
	static {
		Map<String, Entry> residues = new LinkedHashMap<String, Entry>();
		residues.put("4eLeg",          new Entry("11122", false, 'D', "5*N_7*N",        "1*A_2*O_3*d_9*m"));
		residues.put("6dAlt",          new Entry("1222",  true,  'L', "",               "6*m"));
		residues.put("6dAltNAc",       new Entry("1222",  true,  'L', "2*NAc",          "6*m"));
		residues.put("6dGul",          new Entry("2212",  true,  'D', "",               "6*m"));
		residues.put("6dTal",          new Entry("1112",  true,  'D', "",               "6*m"));
		residues.put("6dTalNAc",       new Entry("1112",  true,  'D', "2*NAc",          "6*m"));
		residues.put("Abe",            new Entry("212",   false, 'D', "",               "3*d_6*m"));
		residues.put("Aci",            new Entry("21111", false, 'L', "5*N_7*N",        "1*A_2*O_3*d_9*m"));
		residues.put("All",            new Entry("2222",  true,  'D', "",               ""));
		residues.put("AllA",           new Entry("2222",  true,  'D', "",               ""));
		residues.put("AllN",           new Entry("2222",  true,  'D', "2*N",            ""));
		residues.put("AllNAc",         new Entry("2222",  true,  'D', "2*NAc",          ""));
		residues.put("Alt",            new Entry("1222",  true,  'L', "",               ""));
		residues.put("AltA",           new Entry("1222",  true,  'L', "",               ""));
		residues.put("AltN",           new Entry("1222",  true,  'L', "2*N",            ""));
		residues.put("AltNAc",         new Entry("1222",  true,  'L', "2*NAc",          ""));
		residues.put("Api",            new Entry("11",    false, 'L', "3*MeOH",         "3*6"));
		residues.put("Ara",            new Entry("122",   true,  'L', "",               ""));
		residues.put("Bac",            new Entry("2122",  true,  'D', "2*N_4*N",        "6*m"));
		residues.put("Col",            new Entry("121",   false, 'L', "",               "3*d_6*m"));
		residues.put("D-gro-D-manHep", new Entry("11222", false, 'D', "",               ""));
		residues.put("Dha",            new Entry("112",   false, 'D', "",               "1*A_2*O_3*d_7*A"));
		residues.put("Dig",            new Entry("222",   false, 'D', "",               "2*d_6*m"));
		residues.put("Fru",            new Entry("122",   true,  'D', "",               "1*h_2*O"));
		residues.put("Fuc",            new Entry("2112",  true,  'L', "",               "6*m"));
		residues.put("FucNAc",         new Entry("2112",  true,  'L', "2*NAc",          "6*m"));
		residues.put("Gal",            new Entry("2112",  true,  'D', "",               ""));
		residues.put("GalA",           new Entry("2112",  true,  'D', "",               ""));
		residues.put("GalN",           new Entry("2112",  true,  'D', "2*N",            ""));
		residues.put("GalNAc",         new Entry("2112",  true,  'D', "2*NAc",          ""));
		residues.put("Glc",            new Entry("2122",  true,  'D', "",               ""));
		residues.put("GlcA",           new Entry("2122",  true,  'D', "",               ""));
		residues.put("GlcN",           new Entry("2122",  true,  'D', "2*N",            ""));
		residues.put("GlcNAc",         new Entry("2122",  true,  'D', "2*NAc",          ""));
		residues.put("Gul",            new Entry("2212",  true,  'D', "",               ""));
		residues.put("GulA",           new Entry("2212",  true,  'D', "",               ""));
		residues.put("GulN",           new Entry("2212",  true,  'D', "2*N",            ""));
		residues.put("GulNAc",         new Entry("2212",  true,  'D', "2*NAc",          ""));
		residues.put("Hex",            new Entry("",      false, 'X', "",               ""));
		residues.put("HexA",           new Entry("",      false, 'X', "",               ""));
		residues.put("HexN",           new Entry("",      false, 'X', "2*N",            ""));
		residues.put("HexNAc",         new Entry("",      false, 'X', "2*NAc",          ""));
		residues.put("Ido",            new Entry("1212",  true,  'L', "",               ""));
		residues.put("IdoA",           new Entry("1212",  true,  'L', "",               ""));
		residues.put("IdoN",           new Entry("1212",  true,  'L', "2*N",            ""));
		residues.put("IdoNAc",         new Entry("1212",  true,  'L', "2*NAc",          ""));
		residues.put("Kdn",            new Entry("21122", false, 'D', "",               "1*A_2*O_3*d"));
		residues.put("Kdo",            new Entry("1122",  false, 'D', "",               "1*A_2*O_3*d"));
		residues.put("L-gro-D-manHep", new Entry("11221", false, 'D', "",               ""));
		residues.put("Leg",            new Entry("21122", false, 'D', "5*N_7*N",        "1*A_2*O_3*d_9*m"));
		residues.put("Lyx",            new Entry("112",   true,  'D', "",               ""));
		residues.put("Man",            new Entry("1122",  true,  'D', "",               ""));
		residues.put("ManA",           new Entry("1122",  true,  'D', "",               ""));
		residues.put("ManN",           new Entry("1122",  true,  'D', "2*N",            ""));
		residues.put("ManNAc",         new Entry("1122",  true,  'D', "2*NAc",          ""));
		residues.put("Mur",            new Entry("2122",  true,  'D', "2*N_3*(R)Lac",   ""));
		residues.put("MurNAc",         new Entry("2122",  true,  'D', "2*NAc_3*(R)Lac", ""));
		residues.put("MurNGc",         new Entry("2122",  true,  'D', "2*NGc_3*(R)Lac", ""));
		residues.put("Neu",            new Entry("21122", false, 'D', "5*N",            "1*A_2*O_3*d"));
		residues.put("NeuAc",          new Entry("21122", false, 'D', "5*NAc",          "1*A_2*O_3*d"));
		residues.put("NeuGc",          new Entry("21122", false, 'D', "5*NGc",          "1*A_2*O_3*d"));
		residues.put("Non",            new Entry("",      false, 'X', "",               ""));
		residues.put("Oli",            new Entry("122",   true,  'D', "",               "2*d_6*m"));
		residues.put("Par",            new Entry("222",   false, 'D', "",               "3*d_6*m"));
		residues.put("Pen",            new Entry("",      false, 'X', "",               ""));
		residues.put("Pse",            new Entry("22111", false, 'L', "5*N_7*N",        "2*O_3*d_9*m"));
		residues.put("Psi",            new Entry("222",   true,  'D', "",               "1*h_2*O"));
		residues.put("Qui",            new Entry("2122",  true,  'D', "",               "6*m"));
		residues.put("QuiNAc",         new Entry("2122",  true,  'D', "2*NAc",          "6*m"));
		residues.put("Rha",            new Entry("1122",  true,  'L', "",               "6*m"));
		residues.put("RhaNAc",         new Entry("1122",  true,  'L', "2*NAc",          "6*m"));
		residues.put("Rib",            new Entry("222",   true,  'D', "",               ""));
		residues.put("Sor",            new Entry("212",   true,  'L', "",               "1*h_2*O"));
		residues.put("Tag",            new Entry("112",   true,  'D', "",               "1*h_2*O"));
		residues.put("Tal",            new Entry("1112",  true,  'D', "",               ""));
		residues.put("TalA",           new Entry("1112",  true,  'D', "",               ""));
		residues.put("TalN",           new Entry("1112",  true,  'D', "2*N",            ""));
		residues.put("TalNAc",         new Entry("1112",  true,  'D', "2*NAc",          ""));
		residues.put("Tyv",            new Entry("122",   false, 'D', "",               "3*d_6*m"));
		residues.put("Xyl",            new Entry("212",   true,  'D', "",               ""));
		residues.put("dHex",           new Entry("",      false, 'X', "",               ""));
		residues.put("dHexNAc",        new Entry("",      false, 'X', "2*NAc",          ""));
		residues.put("ddHex",          new Entry("",      false, 'X', "",               ""));
		residues.put("ddNon",          new Entry("",      false, 'X', "5*N_7*N",        ""));
		RESIDUES = Collections.unmodifiableMap(residues);
	}

	/**
	 * What this residue's name says about it, or null when nothing here does - the answer for the
	 * placeholders that stand in for an unassigned residue, and for dHexA, whose deoxy carbon its own
	 * definition never names.
	 */
	public static Entry forResidueName(String _residueName) {
		return RESIDUES.get(_residueName);
	}
}
