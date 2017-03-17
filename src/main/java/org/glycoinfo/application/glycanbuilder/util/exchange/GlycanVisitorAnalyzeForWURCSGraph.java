package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.util.exchange.TrivialNameDescriptor;
import org.glycoinfo.glycanbuilder.util.visitor.GlycanVisitor;

public class GlycanVisitorAnalyzeForWURCSGraph implements GlycanVisitor {

	private ArrayList<Linkage> a_aModificationLinkages = new ArrayList<Linkage>();
	private ArrayList<Linkage> a_aGlycosidicLinkages = new ArrayList<Linkage>();
	private HashMap<Linkage, Residue> a_mLinkageToRepRes = new HashMap<Linkage, Residue>();
	
	private ArrayList<Residue> a_aRESs = new ArrayList<Residue>();
	private ArrayList<Residue> a_aRootOfFragments = new ArrayList<Residue>();
	private ArrayList<Residue> a_aSubstituents = new ArrayList<Residue>();
	
	private TrivialNameDescriptor a_enumTrivialDescriptor;
	private Collection<Residue> a_aResidues;
	private HashSet<Linkage> a_aArrangedSubstituent = new HashSet<Linkage>();
	
	
	public ArrayList<Residue> getResidues() {
		ArrayList<Residue> a_aResidues = new ArrayList<Residue>();
		a_aResidues.addAll(this.a_aRESs);
		//a_aResidues.addAll(this.a_aSubstituents);
		//a_aResidues.addAll(this.a_aRepeatingBlocks);	
		return a_aResidues;
	}
	
	public ArrayList<Residue> getMonosaccharides() {
		return this.a_aRESs;
	}
	
	public ArrayList<Residue> getSubstituent() {
		return this.a_aSubstituents;
	}
	
	public ArrayList<Residue> getRootOfFragments() {
		return this.a_aRootOfFragments;
	}
	
	public Residue getRepeatingResidueByLinkage(Linkage a_oLIN) {
		if(!this.a_mLinkageToRepRes.containsKey(a_oLIN))
			return null;
		return this.a_mLinkageToRepRes.get(a_oLIN);
	}
	
	public ArrayList<Linkage> getLinkages() {
		ArrayList<Linkage> a_aLinkages = new ArrayList<Linkage>();
		a_aLinkages.addAll(this.a_aGlycosidicLinkages);
		a_aLinkages.addAll(this.a_aModificationLinkages);
		
		return a_aLinkages;
	}
	
	@Override
	public void visit(Residue a_oResidue) {
		if(a_oResidue.isStartRepetition() || a_oResidue.isEndCyclic() || a_oResidue.isStartCyclic() ||
				a_oResidue.getType().getSuperclass().equals("Bridge") || this.isContain(a_oResidue.getParentLinkage())) return;
		
		if((a_oResidue.isSubstituent() || a_oResidue.isModificaiton())) {
			if(a_oResidue.hasParent()) {
				if(isProbability(a_oResidue.getParentLinkage()) || a_oResidue.getParent().isBracket())
					this.a_aRootOfFragments.add(a_oResidue);
				else
					this.a_aSubstituents.add(a_oResidue);
			}
		} else if (a_oResidue.isEndRepetition()) {
			this.a_mLinkageToRepRes.put(a_oResidue.getParentLinkage(), a_oResidue);
		} else if (a_oResidue.hasParent() && a_oResidue.getParent().isBracket()) {
			if(a_oResidue.isComposition()) {
				this.a_aRESs.add(a_oResidue);
			}else if(a_oResidue.isSubstituent()) {
				this.a_aRootOfFragments.add(a_oResidue);
			}else{	
				this.a_aRootOfFragments.add(a_oResidue);
				this.a_aRESs.add(a_oResidue);				
			}
		}else {
			if(a_oResidue.hasParent() && this.isProbability(a_oResidue.getParentLinkage()))
				this.a_aRootOfFragments.add(a_oResidue);
			
			this.a_aRESs.add(a_oResidue);	
		}
				
		return;
	}

	@Override
	public void visit(Linkage a_oLinkage) {
		if(a_oLinkage == null) return;
		
		Residue a_oChild = a_oLinkage.getChildResidue();
		
		if(a_oChild.isEndCyclic() || a_oChild.isComposition() || 
				a_oChild.getType().getSuperclass().equals("Bridge") || this.isContain(a_oLinkage)) return;
		
		if(a_oChild.isSubstituent() && !a_oChild.getParent().isBracket() && !isProbability(a_oLinkage)) {
			this.a_aModificationLinkages.add(a_oLinkage);
		}else if(a_oChild.isEndRepetition()) {
			this.a_mLinkageToRepRes.put(a_oLinkage, a_oChild);
			this.a_aGlycosidicLinkages.add(a_oLinkage);
		}
		
		if(a_oLinkage.getParentResidue() != null) {
			/** for cyclic structure */
			Residue a_oParent = a_oLinkage.getParentResidue();
			
			/** スタートリプの時は適応できるが、単糖の時ではこの処理には問題がある*/
			/**　EndCyclicの時に定義しないと逆転してしまうようだ*/
			
			if(a_oParent.isStartCyclic()) {
				Residue a_oEndCyclicResidue = this.getEndCyclicResidue();
				Linkage a_oLIN;
				if(!a_oChild.isRepetition()) {
					a_oLIN = new Linkage(a_oEndCyclicResidue, a_oChild, a_oLinkage.getBonds());
				}else {
					a_oLIN = new Linkage(a_oEndCyclicResidue, a_oChild.getChildAt(0), a_oLinkage.getBonds());
				}
				a_oLIN.getBonds().get(0).setChildPosition(a_oLinkage.getChildPositionsSingle());
				a_oLIN.getBonds().get(0).setParentPosition(a_oLinkage.getParentPositionsSingle());
				this.a_aGlycosidicLinkages.add(a_oLIN);
			}
			
			if(a_oParent.isEndRepetition()) {
				a_oParent = a_oParent.getParent();
				this.a_aGlycosidicLinkages.add(new Linkage(a_oParent, a_oChild, a_oChild.getParentLinkage().getBonds()));
			}
			
			if(a_oChild.isStartRepetition()) {
				if(a_oParent.isSaccharide()) {
					Linkage a_oLIN = new Linkage(a_oParent, a_oChild.getChildAt(0), a_oChild.getParentLinkage().getBonds());
					this.a_aGlycosidicLinkages.add(a_oLIN);
				}
			}
			
			if(a_oChild.isSaccharide() && a_oChild.getParent().isSaccharide() && !isProbability(a_oLinkage)) {
				this.a_aGlycosidicLinkages.add(a_oLinkage);
			}
			if(a_oLinkage.getParentResidue().isBracket() && a_oChild.isSaccharide())
				this.a_aGlycosidicLinkages.add(a_oLinkage);
			//if(a_oLinkage.getParentResidue().getType().getSuperclass().equals("Bridge"))
			//	this.a_aGlycosidicLinkages.add(a_oLinkage);
		}
		return;
	}

	@Override
	public void start(Glycan a_oGlycan) {
		this.clear();
		this.a_aResidues = a_oGlycan.getAllResidues();
		
		for(Residue a_oRES : this.a_aResidues) {
			if(a_oRES.getParentLinkage() == null && !a_oRES.isSaccharide()) continue;
			
			/** for native substituent */
			this.extractNativeSubstituent(a_oRES);
		
			/** for reisidue */
			this.visit(a_oRES);
			
			/** for linkage */
			this.visit(a_oRES.getParentLinkage());
		}	
	}

	@Override
	public void clear() {
		this.a_aGlycosidicLinkages = new ArrayList<Linkage>();
		this.a_aModificationLinkages = new ArrayList<Linkage>();
		this.a_mLinkageToRepRes = new HashMap<Linkage, Residue>();
		this.a_aRESs = new ArrayList<Residue>();
		this.a_aRootOfFragments = new ArrayList<Residue>();
		this.a_aSubstituents = new ArrayList<Residue>();
	}

	private void extractNativeSubstituent(Residue a_oRES) {
		this.a_enumTrivialDescriptor = TrivialNameDescriptor.forTrivialName(a_oRES.getTypeName());
		
		if(this.a_enumTrivialDescriptor == null && a_oRES.getType().getSuperclass().equals("Nonulosonate"))
			this.a_enumTrivialDescriptor = TrivialNameDescriptor.forTrivialName(a_oRES.getType().getCompositionClass());
		
		if(this.a_enumTrivialDescriptor == null || 
				this.a_enumTrivialDescriptor.getSubstituent().isEmpty()) return;
		
		for(String a_sSUB : this.a_enumTrivialDescriptor.getSubstituent()) {
			String[] a_aSUBs = a_sSUB.split("\\*");
			for(Linkage a_oLIN : a_oRES.getChildrenLinkages()) {
				if(a_oLIN.getChildResidue().isSubstituent()) {
					if(a_oLIN.getParentPositionsSingle() == a_aSUBs[0].charAt(0)) {
						a_aSUBs[1] = a_aSUBs[1] + a_oLIN.getChildResidue().getTypeName();
						this.a_aArrangedSubstituent.add(a_oLIN);
					}
				}
			}
			
			try {
				Residue a_oSubRES = ResidueDictionary.newResidue(a_aSUBs[1]);
				a_oSubRES.setParentLinkage(new Linkage(a_oRES, a_oSubRES, a_aSUBs[0].charAt(0)));
				this.a_aSubstituents.add(a_oSubRES);
				this.a_aModificationLinkages.add(a_oSubRES.getParentLinkage());	
			} catch (Exception e) {
				System.out.println(a_aSUBs.toString() + " is error");
			}
		}
	}
	
	private Residue getEndCyclicResidue() {
		for(Residue a_oRES : this.a_aResidues) {
			if(a_oRES.getEndCyclicResidue() != null) return a_oRES;
		}
		return null;
	}
	
	private boolean isProbability(Linkage a_oLinkage) {
		if(a_oLinkage.getBonds().get(0).getProbabilityHigh() != 100 || 
				a_oLinkage.getBonds().get(0).getProbabilityLow() != 100) return true;

		return false;
	}
	
	private boolean isContain(Linkage a_oLIN) {
		return this.a_aArrangedSubstituent.contains(a_oLIN);
	}
}
