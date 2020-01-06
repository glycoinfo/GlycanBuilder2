package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import java.util.ArrayList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;
import org.glycoinfo.WURCSFramework.wurcs.graph.LinkagePosition;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSEdge;

public class RootOfFragmentsToWURCSEdge extends LinkageToWURCSEdge {

	private ArrayList<Residue> parents = new ArrayList<Residue>();
	
	public Residue  getParent() {
		return this.parents.get(0);
	}
	
	public ArrayList<Residue> getParents() {
		return this.parents;
	}
	
	public boolean isAlternative() {
		return (this.parents.size() > 1);
	}
	
	public void start(Residue _fragRoot) throws Exception {
		/* check parent */
		for(Residue coreRes : _fragRoot.getParentsOfFragment()) {
			if(coreRes.isSaccharide()) {
				this.parents.add(coreRes);
				continue;
			}

			if(_fragRoot.isSubstituent())
				throw new WURCSExchangeException("Substituent cannot connect to substituent.");
			throw new WURCSExchangeException("Substituent cannot be parent of underdetermined subtree.");
		}
		/* probability */
		if(_fragRoot.getParentLinkage().getBonds().get(0).getProbabilityHigh() != 100 ||
				_fragRoot.getParentLinkage().getBonds().get(0).getProbabilityLow() != 100)
			this.parents.add(_fragRoot.getParent());
		
		/**/
		if(_fragRoot.isSubstituent() && _fragRoot.getChildrenLinkages().size() > 1)
			throw new WURCSExchangeException("Substituent having two or more children is NOT handled in the system.");

		/* set fragment connection */
		this.setLinkage(_fragRoot.getParentLinkage());
		this.setChild(_fragRoot);
		this.makeModification();
		
		this.setWURCSEdge(true);
		Bond bond = _fragRoot.getParentLinkage().getBonds().get(0);
		
		for(WURCSEdge edge : this.getParentEdges()) {
			for(LinkagePosition linkagePosition : edge.getLinkages()) {
				linkagePosition.setProbabilityLower((double) bond.getProbabilityLow() / 100);
				linkagePosition.setProbabilityUpper((double) bond.getProbabilityHigh() / 100);
				linkagePosition.setProbabilityPosition( LinkagePosition.MODIFICATIONSIDE);
			}
		}
		
		if(_fragRoot.isSubstituent() && _fragRoot.getChildrenLinkages().isEmpty()) return;
		
		this.setWURCSEdge(false);		
	}
	
	public void clear() {
		this.parents = new ArrayList<Residue>();
	}
}
