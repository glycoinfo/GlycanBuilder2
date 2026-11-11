package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.glycoinfo.application.glycanbuilder.util.exchange.GlycanToWURCSException;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.graph.Modification;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.glycoinfo.application.glycanbuilder.dataset.SubstituentMAPDictionary;
import org.glycoinfo.application.glycanbuilder.util.exchange.exporter.GlycanToWURCSGraph;
import org.glycoinfo.application.glycanbuilder.util.exchange.importer.WURCSSequence2ToGlycan;
import org.glycoinfo.application.glycanbuilder.composition.GlycanComposition;

public class WURCS2Parser implements GlycanParser{
	
	public void setTolerateUnknown(boolean f) {}
	
	/**
	 * The WURCS for this structure, or an empty string.
	 *
	 * <p>This method has nowhere to throw - it is {@link GlycanParser}'s, and every caller treats an
	 * empty string as "this structure has no WURCS" - so when the conversion fails, the reason goes to
	 * {@link LogUtils#report}. The desktop application shows that as a dialog, and its export reads it
	 * back to name which structures came out blank. Nothing here has changed for those callers.
	 *
	 * <p>Anything else - a server writing WURCS for several people, a tool, a test - should call
	 * {@link #toWURCS} instead, which says why on the call. The last-error slot this method writes to
	 * is global, so on a server it belongs to whoever wrote last (#275).
	 */
	public String writeGlycan(Glycan structure) {
		try {
			String wurcs = encode(structure);

			return (wurcs == null) ? "" : wurcs;
		} catch (Exception cannotBeWritten) {
			// Reported as it was thrown, not as wrapped by toWURCS: the dialog and the export's
			// warning both quote this sentence, and they should keep saying what they said.
			LogUtils.report(cannotBeWritten);

			return "";
		}
	}

	/**
	 * The WURCS for this structure, or an exception that says why there is none.
	 *
	 * <p>{@link #writeGlycan} answers a structure it cannot write with an empty string and leaves the
	 * reason in a global slot. For 21 of the 23 heptoses and octoses that reason is "has no stereo
	 * recorded for it" and the caller - the web application's WURCS panel, an MCP encode - received
	 * {@code ""} and nothing else (#275). This is the same conversion, with the reason on the call.
	 *
	 * <p>It does not report to {@link LogUtils}: the exception is the report, and a caller that wants a
	 * dialog can show one.
	 *
	 * @throws GlycanToWURCSException naming why: a residue with no skeleton recorded, a composition of
	 *         something the encoder cannot name, a fragment, an empty composition, or the conversion
	 *         failing outright
	 */
	public String toWURCS(Glycan structure) throws GlycanToWURCSException {
		String wurcs;
		try {
			wurcs = encode(structure);
		} catch (Exception cannotBeWritten) {
			throw GlycanToWURCSException.wrapping(cannotBeWritten);
		}

		if (wurcs == null || wurcs.isEmpty()) {
			if (structure.isFragment())
				throw new GlycanToWURCSException("a fragment has no WURCS: WURCS describes whole structures");
			if (structure.isComposition())
				throw new GlycanToWURCSException("this composition holds no residues, so it has no WURCS");
			throw new GlycanToWURCSException("the conversion produced no WURCS and gave no reason");
		}

		return wurcs;
	}

	/**
	 * The conversion itself, shared by both doors.
	 *
	 * @return the WURCS, or null where there is nothing to write - a fragment, an empty composition -
	 *         which {@link #writeGlycan} has always answered in silence and {@link #toWURCS} names
	 * @throws Exception whatever the conversion threw, unwrapped
	 */
	private String encode(Glycan structure) throws Exception {
		if (structure.isFragment()) return null;

		// A composition is written as composition WURCS, which is a different encoding rather than a
		// worse one: it says which residues are present and that nothing is known about what joins
		// them, and it is what GlyTouCan and GlyCosmos match a composition against. This used to
		// return nothing at all, so "export to WURCS" on a composition wrote an empty file and said
		// why to nobody (#107).
		if (structure.isComposition())
			return GlycanComposition.toWURCS(structure);

		LinkageTypeOptimizer linkOpt = new LinkageTypeOptimizer();
		linkOpt.start(structure);

		GlycanToWURCSGraph glycan2graph = new GlycanToWURCSGraph();
		glycan2graph.start(structure);
		// MAPs go out as the dictionaries spell them, and the WURCS as WURCSFramework normalizes it -
		// which is the canonical WURCS. They used to be rewritten first, dropping the ^X of a
		// phosphorus of unknown configuration because the validator's warning said it "could be
		// normalized": *OP^XOCCN went out as *OPOCCN. WURCSFramework's normal form keeps the ^X, and
		// so does every phosphoethanolamine registered in GlyGen's dump, 3,015 of them, against none
		// without it.
		WURCSGraph graph = glycan2graph.getGraph();

		return new WURCSFactory(graph).getWURCS();
	}
	
	public Glycan readGlycan(String str, MassOptions mass_opt) throws Exception{
		if(str.equals("") || !str.contains("WURCS")) throw new Exception(str + " is wrong format");
		mass_opt.setDerivatization("Und");
		mass_opt.ION_CLOUD.set("Na", 0);

		// The sequence says what its reducing end is, and where it says nothing it is a free end.
		// The mass options handed in carry whatever was last chosen in the dialog, and with
		// "Remember files after restarting" on that outlives the session - so a structure imported
		// after a restart arrived carrying somebody's -Asn from a previous sitting, on a sequence
		// that never mentioned one (#179). Cleared here for the same reason the derivatization and
		// the ion cloud above are: what the sequence states is the answer, and what it does not
		// state is a default rather than a leftover.
		mass_opt.setReducingEndType(org.eurocarbdb.application.glycanbuilder.ResidueType.createFreeReducingEnd());
		
		str = str.trim();		
		if(str.contains("\t")) str = str.substring(str.indexOf("\t") + 1);
		
		refuseInvalid(str);

		WURCSGraph graph = new WURCSFactory(str).getGraph();
		// whatever spelling the MAPs arrived in, hand the conversion the one it recognises
		this.rewriteMAPs(graph, false);

		Glycan glycan;
		try {
			WURCSSequence2ToGlycan seq22glycan = new WURCSSequence2ToGlycan();
			seq22glycan.start(new WURCSFactory(graph), mass_opt);
			glycan = seq22glycan.getGlycan();
		} catch (RuntimeException undescribed) {
			// The conversion's own failures come out as raw NullPointerExceptions and
			// StringIndexOutOfBounds - "String index out of range: 8" for a substituent placed at
			// position 9 of a hexose, say - which name nothing and read as crashes rather than as
			// answers (#123). This is the one door every WURCS enters through, so the translation
			// happens here: what kind of failure, on which sequence, with the original underneath
			// for whoever needs the trace.
			throw new WURCSToGlycanException("could not convert this WURCS to a structure ("
					+ undescribed.getClass().getSimpleName()
					+ (undescribed.getMessage() != null ? ": " + undescribed.getMessage() : "")
					+ "): " + shortenedForError(str), undescribed);
		}

		// A WURCS with two connections between the same pair of residues - a bridge plus a direct
		// bond, as in G11127BT - comes out of the conversion as a genuine cycle in what every
		// walker downstream assumes is a tree. The first of them to touch it then descends
		// forever, and the report is a StackOverflowError far from the cause (#125). Refusing here
		// with a sentence keeps the document untouched and gives the caller something it can
		// actually catch; drawing such structures needs a representation for the second
		// connection, which is its own piece of work.
		refuseCycles(glycan.getRoot(), java.util.Collections.newSetFromMap(
				new java.util.IdentityHashMap<Residue, Boolean>()));
		refuseShortRead(glycan, str);

		// Say what each bond is made of here, where the structure is built, rather than leaving it
		// to whoever writes it out (#4). This pass used to run in writeGlycan alone, so a structure
		// read from WURCS carried UNVALIDATED bonds until the moment it was written back to WURCS -
		// and anything else that asked, a GlycoCT writer or a renderer, was asking a placeholder.
		new LinkageTypeOptimizer().start(glycan);

		return glycan;
	}

	/**
	 * Refuse a WURCS that WURCSFramework's validator reports errors in, naming them (#236).
	 *
	 * <p>The conversion does not check what it is given, and on an invalid sequence it stopped part way
	 * and returned what it had: a twelve-residue structure with a GlcNAc whose carbon 2 carried both
	 * its N-acetyl and a linkage came in as two residues, with no word said. Warnings are not refused;
	 * they say a sequence could be written more canonically, not that it is wrong.
	 */
	static void refuseInvalid(String _wurcs) throws WURCSToGlycanException {
		org.glycoinfo.WURCSFramework.util.validation.WURCSValidator validator =
				new org.glycoinfo.WURCSFramework.util.validation.WURCSValidator();
		try {
			validator.start(_wurcs);
		} catch (RuntimeException validatorFailed) {
			// a validator that cannot finish has said nothing about the sequence; the conversion decides
			return;
		}
		java.util.List<String> errors = validator.getReport().getErrors();
		if (errors == null || errors.isEmpty()) return;
		throw new WURCSToGlycanException("this WURCS is not valid: " + String.join("; ", errors)
				+ ": " + shortenedForError(_wurcs));
	}

	/**
	 * Refuse a structure the conversion built with fewer monosaccharides than the WURCS has (#236).
	 *
	 * <p>The second guard, for what the validator passes and the conversion still drops: a sucrose-type
	 * bond between two anomeric carbons lost its glucose, and a repeat unit all but one of its
	 * residues, each weighing what was left and saying nothing. Better no structure than part of one
	 * presented as the whole.
	 */
	static void refuseShortRead(Glycan _glycan, String _wurcs) throws WURCSToGlycanException {
		if (_glycan == null || _glycan.isComposition()) return;
		int expected;
		try {
			expected = Integer.parseInt(_wurcs.split("/")[1].split(",")[1]);
		} catch (RuntimeException noCountToCompare) {
			return;
		}
		int read = 0;
		java.util.Set<String> unknown = new java.util.TreeSet<String>();
		for (Residue r : _glycan.getAllResidues()) {
			// a bridge - the P of a phosphodiester, the N of an amide - is built as a saccharide and is
			// none of the WURCS's monosaccharides; counted, it hid a glycerol read as nothing
			if (r.isBridge()) continue;
			if (r.isSaccharide()) read++;
			// what the reader named and no dictionary has, made a placeholder of no kind (#282)
			else if (!r.isReducingEnd() && !org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary.hasResidueType(r.getTypeName())
					&& !org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary.hasResidueType(r.getTypeName()))
				unknown.add(r.getTypeName());
		}
		if (read < expected)
			throw new WURCSToGlycanException("only " + read + " of the " + expected
					+ " monosaccharides in this WURCS could be read, so it is not opened"
					+ (unknown.isEmpty() ? "" : " (" + String.join(", ", unknown)
							+ (unknown.size() == 1 ? " is" : " are") + " no residue this application knows)")
					+ ": " + shortenedForError(_wurcs));
	}

	/**
	 * The sequence, cut to fit an error message.
	 * @param sequence The WURCS being read.
	 * @return Returns at most eighty characters of it.
	 */
	private static String shortenedForError(String sequence) {
		String flat = sequence.strip();

		return flat.length() > 80 ? flat.substring(0, 80) + "..." : flat;
	}

	/**
	 * Walks the residue tree and throws where it finds itself again.
	 * @param residue Residue to walk from.
	 * @param visited Every residue already walked, by identity.
	 * @throws Exception If the tree has a cycle in it.
	 */
	private static void refuseCycles(Residue residue, java.util.Set<Residue> visited) throws Exception {
		if (residue == null) return;
		if (!visited.add(residue))
			throw new Exception("this WURCS makes two connections between the same residues"
					+ " (a ring through a bridge), which cannot be represented yet");

		for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage linkage : residue.getChildrenLinkages())
			refuseCycles(linkage.getChildResidue(), visited);
	}

	/**
	 * Rewrites every MAP in the graph, either into its normalized form on the way out or into the
	 * spelling the conversion dictionaries recognise on the way in. Doing both through the same
	 * table is what keeps a structure identical across a round trip.
	 * @param graph Graph whose modifications are rewritten in place.
	 * @param toNormalized True to normalize, false to use the dictionary spelling.
	 */
	private void rewriteMAPs(WURCSGraph graph, boolean toNormalized) {
		for (Modification modification : graph.getModifications()) {
			String map = modification.getMAPCode();
			if (map == null || map.isEmpty()) continue;

			String rewritten = toNormalized ?
					SubstituentMAPDictionary.normalizeForOutput(map) : SubstituentMAPDictionary.toDictionaryForm(map);
			if (!rewritten.equals(map)) modification.setMAPCode(rewritten);
		}
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}
