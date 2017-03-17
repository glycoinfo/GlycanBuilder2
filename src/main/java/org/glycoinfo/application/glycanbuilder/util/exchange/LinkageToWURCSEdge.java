package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.WURCSFramework.wurcs.graph.DirectionDescriptor;
import org.glycoinfo.WURCSFramework.wurcs.graph.LinkagePosition;
import org.glycoinfo.WURCSFramework.wurcs.graph.Modification;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSEdge;

public class LinkageToWURCSEdge {

	private Linkage a_oParentLinkage;
	private Linkage a_oChildLinkage;
	
	private Residue a_oChildRES;
	private Residue a_oParentRES;
	private Residue a_oSubstituent;
		
	private LinkedList<WURCSEdge> a_aParentEdges = new LinkedList<WURCSEdge>();
	private LinkedList<WURCSEdge> a_aChildEdges = new LinkedList<WURCSEdge>();
	private Modification a_oModification;
	private int a_iMAPPosForParent = 0;
	private int a_iMAPPosForChild = 0;
	
	public void setLinkage(Linkage a_oLIN) {
		this.a_oParentLinkage = a_oLIN;
		this.a_oChildLinkage = a_oLIN;
	}
	
	public Residue getParent() {
		return this.a_oParentRES;
	}
	
	public Residue getChild() {
		return this.a_oChildRES;
	}
	
	public Residue getSubstituent() {
		return this.a_oSubstituent;
	}
	
	public LinkedList<WURCSEdge> getParentEdges() {
		return this.a_aParentEdges;
	}
	
	public LinkedList<WURCSEdge> getChildEdges() {
		return this.a_aChildEdges;
	}
	
	public int getMAPPositionForParent() {
		return this.a_iMAPPosForParent;
	}
	
	public Modification getModification() {
		return this.a_oModification;
	}
	
	public void start(Linkage a_oLIN) throws Exception {
		setLinkage(a_oLIN);
		
		/** extract parent and child residue */
		Residue a_oParentRES = a_oLIN.getParentResidue();
		Residue a_oChildRES = a_oLIN.getChildResidue();
		
		this.setParent(a_oParentRES);
		this.setChild(a_oChildRES);
		
		/** extract substituent or modification */
		this.makeModification();
		
		this.setWURCSEdge(true);
		
		if(this.a_oChildRES == null) return;

		this.setWURCSEdge(false);
	}
	
	protected void makeModification() throws Exception {
		Modification a_oMOD = new Modification("");
		
		if(this.a_oSubstituent != null) {
			ResidueToModification a_oR2M = new ResidueToModification();			
			a_oR2M.setSubstituentTemplate(this.a_oSubstituent);

			a_oR2M.setParentLinkage(this.a_oParentLinkage);
			if( this.a_oChildLinkage != this.a_oParentLinkage)
				a_oR2M.setChildLinkage(this.a_oChildLinkage);
			
			a_oR2M.start(this.a_oSubstituent);
			String a_sMAP = a_oR2M.getMAPCode();
			a_oMOD = new Modification(a_sMAP);
			this.a_iMAPPosForChild = a_oR2M.getMAPPositionForChildSide();
			this.a_iMAPPosForParent = a_oR2M.getMAPPositionForParentSide();
		}
		this.a_oModification = a_oMOD;
	}
	
	private LinkedList<WURCSEdge> makeWURCSEdges(Linkage a_oLIN, int a_iMAPPosition, boolean a_bIsParent) {		
		LinkedList<WURCSEdge> a_aEdges = new LinkedList<WURCSEdge>();	
		
		if(a_oLIN.getBonds().size() == 1) {
			int a_iMAPPos = a_bIsParent ? this.a_iMAPPosForParent : this.a_iMAPPosForChild;
			ArrayList<Integer> a_aPositions = a_bIsParent ? 
				this.charToInteger(a_oLIN.getParentPositions()) : this.charToInteger(a_oLIN.getChildPositions());		
			WURCSEdge a_oEdge = this.makeWURCSEdge(a_aPositions, a_iMAPPos);
			a_aEdges.add(a_oEdge);

			return a_aEdges;
		}		
		
		/** bridge substituent */
		ArrayList<Integer> a_aParentPositions = this.charToInteger(a_oLIN.getBonds().get(0).getParentPositions());
		ArrayList<Integer> a_aChildPositions = this.charToInteger(a_oLIN.getBonds().get(1).getParentPositions());
		WURCSEdge a_oParentEdge = this.makeWURCSEdge(a_aParentPositions, this.a_iMAPPosForParent);
		WURCSEdge a_oChildEdge = this.makeWURCSEdge(a_aChildPositions, this.a_iMAPPosForChild);
		a_aEdges.add(a_oParentEdge);
		a_aEdges.add(a_oChildEdge);
		
		return a_aEdges;
	}

	private WURCSEdge makeWURCSEdge(ArrayList<Integer> a_aPositions, int a_iMAPPosition) {
		WURCSEdge a_oEdge = new WURCSEdge();
		for(Integer a_iPos : a_aPositions) {
			LinkagePosition a_oLinkPos = new LinkagePosition(a_iPos, DirectionDescriptor.N, a_iMAPPosition);
			if(a_iMAPPosition != 0)
				a_oLinkPos = new LinkagePosition(a_iPos, DirectionDescriptor.N, false, a_iMAPPosition, false);
		
			a_oEdge.addLinkage(a_oLinkPos);
		}

		return a_oEdge;
	}
	
	protected void setWURCSEdge(boolean a_bIsParent) {
		if(a_bIsParent) {
			this.a_aParentEdges = this.makeWURCSEdges(this.a_oParentLinkage, this.a_iMAPPosForParent, a_bIsParent);
		}else {
			this.a_aChildEdges = this.makeWURCSEdges(this.a_oChildLinkage, this.a_iMAPPosForChild, a_bIsParent);
		}
	}
	
	private void setParent(Residue _a_oParentRES) {
		if(_a_oParentRES.isSubstituent() || 
				_a_oParentRES.getType().getSuperclass().equals("Bridge")) {
			this.a_oSubstituent = _a_oParentRES;
			this.a_oParentLinkage = this.a_oSubstituent.getParentLinkage();
			_a_oParentRES = this.a_oParentLinkage.getParentResidue();
		}	
						
		this.a_oParentRES = _a_oParentRES;
	}
	
	protected void setChild(Residue _a_oChildRES) {
		if(_a_oChildRES.isSubstituent() || _a_oChildRES.isModificaiton()) {
			this.a_oSubstituent = _a_oChildRES;
		}else {
			this.a_oChildRES = _a_oChildRES;
		}

		if(_a_oChildRES.isEndRepetition()) {
			this.a_oChildRES = _a_oChildRES.getStartResidue();
			//this.a_oChildRES = this.a_oChildLinkage.getParentResidue().getStartResidue();
			return;
		}

		if(_a_oChildRES.getChildrenLinkages().isEmpty()) return;
		
		this.a_oChildRES = this.a_oChildLinkage.getChildResidue();
	}
	
	private ArrayList<Integer> charToInteger(Collection<Character> a_cPositions) {
		ArrayList<Integer> a_aPositions = new ArrayList<Integer>();
		for(Character a_cPos : a_cPositions) {
			if(a_cPos == '-' || a_cPos == '?')
				a_aPositions.add(-1);
			else
				a_aPositions.add(Integer.parseInt(String.valueOf(a_cPos)));
		}
		 
		return a_aPositions;
	}
	
	private ArrayList<Integer> charToInteger(char[] a_acPositions) {
		ArrayList<Integer> a_aPositions = new ArrayList<Integer>();
		
		for(int i = 0; i < a_acPositions.length; i++) {
			a_aPositions.add(Integer.parseInt(String.valueOf(a_acPositions[i])));
		}
		return a_aPositions;
	}
}
