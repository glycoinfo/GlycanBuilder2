package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;

/**
 * What a save to GWS loses of the relative stereo read from WURCS (#286).
 *
 * <p>A residue read in relative stereo keeps which way round it was written, {@code ad3dh} or
 * {@code ad4dh}, and the WURCS writer writes it back so. GWS has no place for it, and a residue saved
 * and reopened is written the writer's way, which half the time is the other one. Nothing is left after
 * reopening to say it happened, so the place to say so is the save.
 *
 * <p>Measured, not inferred: a structure is written to WURCS as it is and again after a round trip
 * through GWS, and only where the two differ are its relative residues named.
 */
public final class RelativeStereoInGWS {

	private RelativeStereoInGWS() {}

	/**
	 * The relative residues whose WURCS a GWS save would change, each as "name: as read -> as it
	 * would be written after reopening", or nothing.
	 */
	public static List<String> lostOnSave(Collection<Glycan> _structures) {
		List<String> lost = new ArrayList<String>();
		if (_structures == null) return lost;
		for (Glycan structure : _structures) {
			List<Residue> relative = relativeResidues(structure);
			if (relative.isEmpty()) continue;
			try {
				String now = new WURCS2Parser().writeGlycan(structure);
				Glycan reopened = Glycan.fromString(structure.toString());
				String after = (reopened == null) ? null : new WURCS2Parser().writeGlycan(reopened);
				if (now == null || now.isEmpty() || now.equals(after)) continue;
			} catch (Exception cannotBeCompared) {
				// a structure that cannot be written to WURCS has no WURCS to lose
				continue;
			}
			for (Residue r : relative) {
				String read = r.getRelativeSkeleton();
				lost.add(r.getTypeName() + ": " + read + " -> " + turnedOver(read));
			}
		}
		return lost;
	}

	private static List<Residue> relativeResidues(Glycan _structure) {
		List<Residue> relative = new ArrayList<Residue>();
		for (Residue r : _structure.getAllResidues())
			if (r.getRelativeSkeleton() != null) relative.add(r);
		return relative;
	}

	private static String turnedOver(String _skeletonCode) {
		StringBuilder turned = new StringBuilder(_skeletonCode.length());
		for (char carbon : _skeletonCode.toCharArray())
			turned.append(carbon == '3' ? '4' : carbon == '4' ? '3' : carbon);
		return turned.toString();
	}
}
