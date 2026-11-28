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
import org.glycoinfo.application.glycanbuilder.util.AnomericCarbons;

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

		refuseAGroupWhereNoneCanBe(structure);

		// A ring is built from its anomeric carbon, and one set to ? made no ring: the residue went out
		// as an open chain, or the writer failed (#332). Written with its type's, which is where it is.
		// A residue joined to a parent with its ring not known either is WURCS's u further out than the
		// reducing end, which no monosaccharide joined so can be: refused, as it was, but saying why.
		Residue unknownForm = AnomericCarbons.anUnknownFormFurtherOut(structure);
		if (unknownForm != null)
			throw new Exception(unknownForm.getTypeName() + " is joined to its parent with neither its ring nor its"
					+ " anomeric carbon known (u in WURCS). Only a reducing end can be left so, since a"
					+ " monosaccharide joined by its anomeric carbon is a ring, so this structure is not written");

		Runnable putBack = AnomericCarbons.takeTheirTypes(structure);
		String wurcs;
		try {
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

			wurcs = new WURCSFactory(graph).getWURCS();
		} finally {
			putBack.run();
		}
		// and what comes out is checked as what goes in is: a WURCS no validator accepts is not written
		try {
			refuseInvalid(wurcs);
		} catch (WURCSToGlycanException invalid) {
			throw new Exception("the WURCS this structure makes is not valid, so it is not written - "
					+ invalid.getMessage().replaceFirst("^this WURCS is not valid: ", ""));
		}
		return wurcs;
	}

	/**
	 * Refuse a substituent on a carbon its residue cannot give one to (#320, #321): one with no
	 * hydroxyl, a 6-deoxy's C6, a nonulosonate's C3; or one the residue's name already uses, a FucNAc's
	 * C2, a Mur's C3. Such a structure can no longer be drawn, but one saved before can be opened, and
	 * writing it gave WURCS no validator accepts, or failed with only "Invalide residue.".
	 */
	private static void refuseAGroupWhereNoneCanBe(Glycan _structure) throws Exception {
		for (org.eurocarbdb.application.glycanbuilder.Residue parent : _structure.getAllResidues()) {
			if (!parent.isSaccharide()) continue;
			for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage link : parent.getChildrenLinkages()) {
				org.eurocarbdb.application.glycanbuilder.Residue child = link.getChildResidue();
				if (child == null || !child.isSubstituent() || link.getBonds().size() != 1) continue;
				char position = link.getParentPositionsSingle();
				if (!Character.isDigit(position) || parent.canHoldAt(position, child)) continue;
				throw new Exception(child.getTypeName() + " at " + position + " of " + parent.getTypeName()
						+ ": " + parent.getTypeName() + " has no hydroxyl there, its own group is there already, or it is"
						+ " its amine, which takes no N-linked group and no phosphate - so this structure has no WURCS");
			}
		}
	}
	
	public Glycan readGlycan(String str, MassOptions mass_opt) throws Exception{
		if(str.equals("") || !str.contains("WURCS")) throw new WURCSToGlycanException(str + " is wrong format");
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
		
		// The validator runs beside the conversion rather than before it (#346): on a large structure
		// it is the slowest part of a read - G19350QA, 78 residues of unknown linkage, spent 11.4 s
		// in it against 7.3 s for everything else - and the two do not depend on each other. Its
		// verdict is taken exactly as before: if it finds the sequence invalid, that is the answer,
		// whatever the conversion made of it.
		final String sequence = str;
		java.util.concurrent.Future<WURCSToGlycanException> invalid = VALIDATION.submit(() -> {
			try {
				refuseInvalid(sequence);
				return null;
			} catch (WURCSToGlycanException refused) {
				return refused;
			}
		});

		Glycan glycan;
		try {
			glycan = convert(str, mass_opt);
		} catch (Exception failed) {
			WURCSToGlycanException refused = verdictOf(invalid);
			if (refused != null) throw refused;
			throw failed;
		}
		WURCSToGlycanException refused = verdictOf(invalid);
		if (refused != null) throw refused;

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
	/** One worker for the validator of each read, kept for the life of the application; daemons. */
	private static final java.util.concurrent.ExecutorService VALIDATION =
			java.util.concurrent.Executors.newCachedThreadPool(runnable -> {
				Thread worker = new Thread(runnable, "WURCS validation");
				worker.setDaemon(true);
				return worker;
			});

	/** What the validator said: the refusal, or null when it found nothing wrong or could not finish. */
	private static WURCSToGlycanException verdictOf(java.util.concurrent.Future<WURCSToGlycanException> _invalid)
			throws InterruptedException {
		try {
			return _invalid.get();
		} catch (java.util.concurrent.ExecutionException validatorFailed) {
			// a validator that cannot finish has said nothing about the sequence; the conversion decides
			return null;
		}
	}

	/** The sequence converted to a structure, unchecked by the validator, which runs beside it. */
	private Glycan convert(String _wurcs, MassOptions _massOpt) throws Exception {
		WURCSFactory wf;
		try {
			wf = new WURCSFactory(_wurcs);
		} catch (Exception e) {
			if (e.getMessage() != null) {
				throw new WURCSToGlycanException(e.getMessage(), e);
			} else {
				throw new WURCSToGlycanException("Could not parse", e);
			}
		}

		// whatever spelling the MAPs arrived in, hand the conversion the one it recognises. Only when
		// one was respelled is the sequence built again from the graph: building it is a whole
		// normalization, 3.7 s of G19350QA's read, and was done twice for every sequence (#346)
		WURCSGraph graph = wf.getGraph();
		if (this.rewriteMAPs(graph, false)) wf = new WURCSFactory(graph);

		try {
			WURCSSequence2ToGlycan seq22glycan = new WURCSSequence2ToGlycan();
			seq22glycan.start(wf, _massOpt);
			return seq22glycan.getGlycan();
		} catch (WURCSToGlycanException ex) {
			throw ex;
		} catch (Exception undescribed) {
			// The conversion's own failures come out as raw NullPointerExceptions and
			// StringIndexOutOfBounds - "String index out of range: 8" for a substituent placed at
			// position 9 of a hexose, say - which name nothing and read as crashes rather than as
			// answers (#123). This is the one door every WURCS enters through, so the translation
			// happens here: what kind of failure, on which sequence, with the original underneath
			// for whoever needs the trace.
			throw new WURCSToGlycanException("could not convert this WURCS to a structure ("
					+ undescribed.getClass().getSimpleName()
					+ (undescribed.getMessage() != null ? ": " + undescribed.getMessage() : "")
					+ "): " + shortenedForError(_wurcs), undescribed);
		}
	}

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
		java.util.List<String> reasons = new java.util.ArrayList<String>();
		for (String error : errors) reasons.add(reasonIn(error));
		throw new WURCSToGlycanException("this WURCS is not valid: " + String.join("; ", reasons)
				+ ": " + shortenedForError(_wurcs));
	}

	/** A validator error that could not parse the sequence: it, in full, and then the reason. */
	private static final java.util.regex.Pattern PARSE_ERROR = java.util.regex.Pattern.compile(
			"^Error in parsing WURCS due to the exception: .*? - \\[[A-Za-z]+\\] (.+)$",
			java.util.regex.Pattern.DOTALL);

	/**
	 * The part of a validator error that says what is wrong (#335).
	 *
	 * <p>When the validator cannot parse a sequence, its error quotes the whole of it and then the
	 * reason: "Error in parsing WURCS due to the exception: WURCS=2.0/... - [WURCSExchangeException]
	 * Unknown CarbonDescriptor is found. p of a1221p". The refusal adds the sequence again, so it
	 * appeared twice and the part at fault was lost in the middle. Only the reason is kept. The
	 * validator's other errors name the part at fault without the sequence, and are kept as they are.
	 */
	static String reasonIn(String _error) {
		if (_error == null) return "";
		java.util.regex.Matcher parse = PARSE_ERROR.matcher(_error.strip());
		return parse.matches() ? parse.group(1).strip() : _error;
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
			throw new WURCSToGlycanException("this WURCS makes two connections between the same residues"
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
	/** @return whether any MAP was respelled */
	private boolean rewriteMAPs(WURCSGraph graph, boolean toNormalized) {
		boolean respelled = false;
		for (Modification modification : graph.getModifications()) {
			String map = modification.getMAPCode();
			if (map == null || map.isEmpty()) continue;

			String rewritten = toNormalized ?
					SubstituentMAPDictionary.normalizeForOutput(map) : SubstituentMAPDictionary.toDictionaryForm(map);
			if (!rewritten.equals(map)) {
				modification.setMAPCode(rewritten);
				respelled = true;
			}
		}
		return respelled;
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}
