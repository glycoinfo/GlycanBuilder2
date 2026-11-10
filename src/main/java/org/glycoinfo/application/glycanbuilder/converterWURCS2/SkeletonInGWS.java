package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.glycoinfo.application.glycanbuilder.util.exchange.exporter.ResidueAnalyzer;

/**
 * What a save to GWS loses of the skeletons read from WURCS: relative stereo the way round it was
 * read (#286), and the known centres of a monosaccharide read with unknown ones (#265).
 *
 * <p>The residue keeps the skeleton it was read from, and the WURCS writer writes it back so. GWS has
 * no place for it, and a residue saved and reopened is written as the writer builds it. Nothing is
 * left after reopening to say it happened, so the place to say so is the save.
 *
 * <p>Measured, not inferred: a structure is written to WURCS as it is and again after a round trip
 * through GWS, and only where the two differ are its residues named, with what each will become.
 */
public final class SkeletonInGWS {

	private SkeletonInGWS() {}

	/**
	 * The residues whose WURCS a GWS save would change, each as "name: as read -> as it will be
	 * written after reopening", or nothing.
	 */
	public static List<String> lostOnSave(Collection<Glycan> _structures) {
		List<String> lost = new ArrayList<String>();
		if (_structures == null) return lost;
		for (Glycan structure : _structures) {
			List<Residue> kept = residuesKeepingASkeleton(structure);
			if (kept.isEmpty()) continue;
			try {
				String now = new WURCS2Parser().writeGlycan(structure);
				Glycan reopened = Glycan.fromString(structure.toString());
				String after = (reopened == null) ? null : new WURCS2Parser().writeGlycan(reopened);
				if (now == null || now.isEmpty() || now.equals(after)) continue;
			} catch (Exception cannotBeCompared) {
				// a structure that cannot be written to WURCS has no WURCS to lose
				continue;
			}
			for (Residue r : kept)
				lost.add(r.getTypeName() + ": " + r.getReadSkeleton() + " -> " + builtWithout(r));
		}
		return lost;
	}

	private static List<Residue> residuesKeepingASkeleton(Glycan _structure) {
		List<Residue> kept = new ArrayList<Residue>();
		for (Residue r : _structure.getAllResidues())
			if (r.getReadSkeleton() != null) kept.add(r);
		return kept;
	}

	/** What the writer builds for a residue that has lost the skeleton it was read from. */
	private static String builtWithout(Residue _residue) {
		Residue reopened = _residue.cloneResidue();
		reopened.setReadSkeleton(null);
		try {
			ResidueAnalyzer analyzer = new ResidueAnalyzer();
			analyzer.ResidueToSkeletonCode(reopened);
			return analyzer.getSkeletonCode();
		} catch (Exception cannotBeBuilt) {
			return "?";
		}
	}
}
