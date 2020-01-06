package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

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

	private Linkage parentLinkage;
	private Linkage childLinkage;
	
	private Residue childRes;
	private Residue parentRes;
	private Residue substituent;
		
	private LinkedList<WURCSEdge> parentEdges;
	private LinkedList<WURCSEdge> childEdges;
	private Modification modification;
	private int mapPos4Parent = 0;
	private int mapPos4Child = 0;

	public LinkageToWURCSEdge() {
		parentEdges = new LinkedList<WURCSEdge>();
		childEdges = new LinkedList<WURCSEdge>();
	}

	public void setLinkage(Linkage a_oLIN) {
		this.parentLinkage = a_oLIN;
		this.childLinkage = a_oLIN;
	}
	
	public Residue getParent() {
		return this.parentRes;
	}
	
	public Residue getChild() {
		return this.childRes;
	}
	
	public Residue getSubstituent() {
		return this.substituent;
	}
	
	public LinkedList<WURCSEdge> getParentEdges() {
		return this.parentEdges;
	}
	
	public LinkedList<WURCSEdge> getChildEdges() {
		return this.childEdges;
	}
	
	public int getMAPPositionForParent() {
		return this.mapPos4Parent;
	}
	
	public Modification getModification() {
		return this.modification;
	}
	
	public void start(Linkage _linkage) throws Exception {
		setLinkage(_linkage);
		
		// extract parent and child residue
		Residue parentRes = _linkage.getParentResidue();
		Residue childRes = _linkage.getChildResidue();
		
		this.setParent(parentRes);
		this.setChild(childRes);
		
		// extract substituent or modification
		this.makeModification();
		
		this.setWURCSEdge(true);
		
		if(this.childRes == null) return;

		this.setWURCSEdge(false);
	}
	
	protected void makeModification() throws Exception {
		Modification mod = new Modification("");
		
		if(this.substituent != null) {
			ResidueToModification res2mod = new ResidueToModification();
			res2mod.setSubstituentTemplate(this.substituent);

			res2mod.setParentLinkage(this.parentLinkage);
			if( this.childLinkage != this.parentLinkage)
				res2mod.setChildLinkage(this.childLinkage);
			
			res2mod.start(this.substituent);
			String map = res2mod.getMAPCode();
			mod = new Modification(map);
			this.mapPos4Child = res2mod.getMAPPositionForChildSide();
			this.mapPos4Parent = res2mod.getMAPPositionForParentSide();
		}
		this.modification = mod;
	}
	
	private LinkedList<WURCSEdge> makeWURCSEdges(Linkage _linkage, int _mapPosition, boolean _isParent) {
		LinkedList<WURCSEdge> edges = new LinkedList<WURCSEdge>();
		
		if(_linkage.getBonds().size() == 1) {
			int mapPos = _isParent ? this.mapPos4Parent : this.mapPos4Child;
			ArrayList<Integer> positions = _isParent ?
				this.charToInteger(_linkage.getParentPositions()) : this.charToInteger(_linkage.getChildPositions());
			WURCSEdge a_oEdge = this.makeWURCSEdge(positions, mapPos);
			edges.add(a_oEdge);

			return edges;
		}		
		
		// bridge substituent
		ArrayList<Integer> parentPositions = charToInteger(_linkage.getBonds().get(0).getParentPositions());
		ArrayList<Integer> childPositions = charToInteger(_linkage.getBonds().get(1).getParentPositions());
		WURCSEdge parentEdge = this.makeWURCSEdge(parentPositions, this.mapPos4Parent);
		WURCSEdge childEdge = this.makeWURCSEdge(childPositions, this.mapPos4Child);
		edges.add(parentEdge);
		edges.add(childEdge);
		
		return edges;
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
			this.parentEdges = this.makeWURCSEdges(this.parentLinkage, this.mapPos4Parent, a_bIsParent);
		}else {
			this.childEdges = this.makeWURCSEdges(this.childLinkage, this.mapPos4Child, a_bIsParent);
		}
	}
	
	private void setParent(Residue _parentRES) {
		if(_parentRES.isSubstituent() ||
				_parentRES.getType().getSuperclass().equals("Bridge")) {
			this.substituent = _parentRES;
			this.parentLinkage = this.substituent.getParentLinkage();
			_parentRES = this.parentLinkage.getParentResidue();
		}	
						
		this.parentRes = _parentRES;
	}
	
	protected void setChild(Residue _childRES) {
		if(_childRES.isSubstituent() || _childRES.isModificaiton()) {
			this.substituent = _childRES;
		}else {
			this.childRes = _childRES;
		}

		if(_childRES.isEndRepetition()) {
			this.childRes = _childRES.getStartResidue();
			//this.a_oChildRES = this.a_oChildLinkage.getParentResidue().getStartResidue();
			return;
		}

		if(_childRES.getChildrenLinkages().isEmpty()) return;
		
		this.childRes = this.childLinkage.getChildResidue();
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
	
	private ArrayList<Integer> charToInteger(char _position) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		
		ret.add(_position == '?' ? -1 : Integer.parseInt(String.valueOf(_position)));
		
		return ret;
	}
	
	private ArrayList<Integer> charToInteger(char[] a_acPositions) {
		ArrayList<Integer> a_aPositions = new ArrayList<Integer>();

		for(int i = 0; i < a_acPositions.length; i++) {
			a_aPositions.add((String.valueOf(a_acPositions[i]).equals("?")) ? -1 : Integer.parseInt(String.valueOf(a_acPositions[i])));
		}
		return a_aPositions;
	}
}
