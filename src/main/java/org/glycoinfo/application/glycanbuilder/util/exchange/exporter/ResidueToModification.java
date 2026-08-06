package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.application.glycanbuilder.dataset.SubstituentMAPDictionary;
import org.glycoinfo.WURCSFramework.util.oldUtil.SubstituentTemplate;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ResidueToModification {

	// substituents with no usable template in glycanformatconverter at all - WURCS2 export
	// stays unsupported for these. The pyruvates used to be listed here too, because their
	// MAP was re-derived from SubstituentTypeToMAP, which cannot tell (S) from (R) (all three
	// share the glycoCT notation "pyruvate", and its S entry carries ^R); bridge MAPs are now
	// taken from BaseCrossLinkedTemplate, which keeps the stereo, so they work again.
	private static final Set<String> WURCS2_UNSUPPORTED_SUBSTITUENTS = new HashSet<>(Arrays.asList(
			"Pyr", "Ino"));

	// a plain ether bridge carries no chemistry of its own: WURCS writes it as the two linkage
	// positions alone, e.g. [a2122h-1x_1-5_4-6], so "*O*" must not be emitted as a MAP
	private static final String ETHER_BRIDGE_MAP = "*O*";

	private Residue subtituent;
	private Linkage childLinkage = null;
	private Linkage parentLinkage = null;

	private int parentMAPPosition = 0;
	private int childMAPPosition = 0;
	private String headAtom = "";
	private String tailAtom = "";
	private String map = "";
	
	private LinkageType parentLinkageType;
	private LinkageType childLinkageType;

	
	public String getMAPCode() {
		return this.map;
	}
	
	public int getMAPPositionForParentSide() {
		return this.parentMAPPosition;
	}
	
	public int getMAPPositionForChildSide() {
		return this.childMAPPosition;
	}
	
	public String getHeadAtom() {
		return this.headAtom;
	}
	
	public String getTailAtom() {
		return this.tailAtom;
	}

	public void setParentLinkage(Linkage _linkage) {
		this.parentLinkage = _linkage;
	}
	
	public void setChildLinkage(Linkage _linkage) {
		this.childLinkage = _linkage;
	}
	
	public void setSubstituentTemplate(Residue _substituent) throws Exception {
		String map = _substituent.getType().getMAP();
		if (map == null || map.isEmpty())
			throw new Exception("WURCS2 export is not supported for substituent \""
					+ _substituent.getTypeName() + "\"");
	}
	
	public void start(Residue _substituent) throws Exception {
		this.subtituent = _substituent;

		if(this.subtituent.getParentLinkage() != null)
			this.parentLinkage = this.subtituent.getParentLinkage();
		if(this.parentLinkage == null)
			throw new Exception("Substituent should have parent linkage");
		if(!this.subtituent.getChildrenLinkages().isEmpty())
			this.childLinkage = this.subtituent.getChildrenLinkages().get(0);

		this.parentLinkageType = this.parentLinkage.getParentLinkageType();
		this.childLinkageType = this.parentLinkage.getChildLinkageType();

		if(this.parentLinkageType == LinkageType.UNKNOWN)
			this.parentLinkageType = LinkageType.H_AT_OH;

		// A bridge's template MAP is already the finished code: it carries its own linking atoms
		// (*OPO* for a phosphate, *N* for an imino bridge) and its own stereo (*OC^SO* vs *OC^RO*).
		// Re-deriving it from the glycoCT notation loses both, so it is used verbatim here.
		if(this.subtituent.getType().isBridge()) {
			this.applyCrossLinkedTemplate();
			this.normalizeMAP();
			return;
		}

		// The dictionary records what each substituent's MAP is, so a substituent attached at one
		// position is written from that rather than assembled again from its notation.
		if(this.applyOwnMAP()) return;

		// A substituent spanning two positions is a bridge, and the bridges have their own
		// dictionary. One arriving here is a substituent that was given a second bond, which this
		// builder no longer offers - only a structure saved before it stopped can still hold one.
		if(this.parentLinkage.getBonds().size() > 1)
			throw new Exception("A substituent cannot span two positions: \""
					+ _substituent.getTypeName() + "\". The bridge of the same name spans two.");

		throw new Exception("No MAP recorded for substituent \"" + _substituent.getTypeName() + "\"");
	}

	/**
	 * Puts the MAP into the form a validator asks for - notably without the {@code ^X} that marks a
	 * phosphorus of unknown configuration.
	 *
	 * <p>Normalizing can also introduce the star indices that mark the two ends of a divalent MAP,
	 * and WURCS then requires each linkage to name the end it attaches to. The two go together: a
	 * MAP written {@code *1NS*2} with a linkage written {@code 4-6} is rejected, while the same MAP
	 * with {@code 4n1-6n2} is accepted.</p>
	 */
	private void normalizeMAP() {
		if(this.map == null || this.map.isEmpty()) return;

		String normalized = SubstituentMAPDictionary.normalize(this.map);
		if(normalized.equals(this.map)) return;

		if(this.hasStarIndices(normalized) && !this.hasStarIndices(this.map)) {
			// only a divalent MAP has two ends to tell apart: within one monosaccharide both bonds
			// hang off the parent linkage, between two it is the parent and the child linkage
			boolean isDivalent = this.parentLinkage.getBonds().size() > 1 || this.childLinkage != null;
			if(!isDivalent) return;

			Boolean isSwap = this.resolveSwap();
			this.parentMAPPosition = (isSwap != null && isSwap) ? 2 : 1;
			this.childMAPPosition = (isSwap != null && isSwap) ? 1 : 2;
		}

		this.map = normalized;
	}

	private boolean hasStarIndices(String _map) {
		return _map.contains("*1") && _map.contains("*2");
	}

	/**
	 * Writes the MAP the dictionary records for this substituent, when it has one and hangs off a
	 * single position.
	 * @return Returns true when the MAP was written from the dictionary.
	 */
	private boolean applyOwnMAP() {
		if(this.parentLinkage.getBonds().size() != 1) return false;
		if(this.subtituent.getType().isBridge()) return false;

		String ownMAP = this.subtituent.getType().getMAP();
		if(ownMAP == null || ownMAP.isEmpty()) return false;

		this.headAtom = this.atomNextToStar(ownMAP, true);
		this.tailAtom = this.atomNextToStar(ownMAP, false);
		this.map = ownMAP;
		this.normalizeMAP();

		return true;
	}

	/**
	 * Takes the MAP of a bridge substituent straight from its cross-linked template, which already
	 * spells out the linking atoms and the stereo. A plain ether bridge ({@value #ETHER_BRIDGE_MAP})
	 * has no MAP in WURCS - the two linkage positions alone say everything - so it yields "".
	 */
	private void applyCrossLinkedTemplate() {
		String templateMAP = this.subtituent.getType().getMAP();

		if(templateMAP == null || templateMAP.equals("") || templateMAP.equals(ETHER_BRIDGE_MAP)) {
			this.map = "";
			return;
		}

		// the ends have to be known first: which one the parent attaches to is read from them
		this.headAtom = this.atomNextToStar(templateMAP, true);
		this.tailAtom = this.atomNextToStar(templateMAP, false);

		// "*1...*2" pins which end of the MAP each linkage attaches to
		if(templateMAP.contains("*1") && templateMAP.contains("*2")) {
			Boolean isSwap = this.resolveSwap();
			this.parentMAPPosition = (isSwap != null && isSwap) ? 2 : 1;
			this.childMAPPosition = (isSwap != null && isSwap) ? 1 : 2;
		}

		this.map = templateMAP;
	}

	/** The atom a MAP starts or ends with, ignoring the "*" and any star index. */
	private String atomNextToStar(String _map, boolean _isHead) {
		String stripped = _map.split("/")[0].replaceAll("\\*[12]?", " ").trim();
		if(stripped.isEmpty()) return "";

		if(_isHead)
			return stripped.startsWith("Cl") || stripped.startsWith("Br") ?
					stripped.substring(0, 2) : stripped.substring(0, 1);

		return stripped.substring(stripped.length() - 1);
	}


	/** Which end of a divalent MAP the parent linkage attaches to; null when the MAP is unordered. */
	private Boolean resolveSwap() {
		// A bridge's own MAP says which way round it goes: a sugar does not attach through the
		// nitrogen end when the other end is something else, and that is the only asymmetry among
		// the bridges - N and the succinates have the same atom at both ends, and a triphosphate
		// reaches the sugar through an oxygen either way.
		if(this.subtituent.getType().isBridge())
			return ("N".equals(this.headAtom) && !"N".equals(this.tailAtom)) ? Boolean.TRUE : null;

		Boolean isSwap = null;

		if(isSwap == null && this.parentLinkageType != this.childLinkageType) {
			if(this.parentLinkageType == LinkageType.H_AT_OH)
				isSwap = false;
			else if (this.childLinkageType == LinkageType.H_AT_OH)
				isSwap = true;
		}

		return isSwap;
	}

}
