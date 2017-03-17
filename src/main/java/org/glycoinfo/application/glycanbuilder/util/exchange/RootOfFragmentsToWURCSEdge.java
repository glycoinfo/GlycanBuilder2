package org.glycoinfo.application.glycanbuilder.util.exchange;

import java.util.ArrayList;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;
import org.glycoinfo.WURCSFramework.wurcs.graph.LinkagePosition;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSEdge;

public class RootOfFragmentsToWURCSEdge extends LinkageToWURCSEdge{

	private ArrayList<Residue> a_aParents = new ArrayList<Residue>();
	
	public Residue getParent() {
		return this.a_aParents.get(0);
	}
	
	public ArrayList<Residue> getParents() {
		return this.a_aParents;
	}
	
	public boolean isAlternative() {
		return (this.a_aParents.size() > 1);
	}
	
	public void start(Residue a_oRootOfFragments) throws Exception {
		/** check parent */
		for(Residue a_oRES : a_oRootOfFragments.getParentsOfFragment()) {
			if(a_oRES.isSaccharide()) {
				this.a_aParents.add(a_oRES);
				continue;
			}
			 
			if(a_oRootOfFragments.isSubstituent())
				throw new WURCSExchangeException("Substituent cannot connect to substituent.");
			throw new WURCSExchangeException("Substituent cannot be parent of underdetermined subtree.");
		}
		/** probability */
		if(a_oRootOfFragments.getParentLinkage().getBonds().get(0).getProbabilityHigh() != 100 ||
				a_oRootOfFragments.getParentLinkage().getBonds().get(0).getProbabilityLow() != 100)
			this.a_aParents.add(a_oRootOfFragments.getParent());
		
		/***/
		if(a_oRootOfFragments.isSubstituent() && a_oRootOfFragments.getChildrenLinkages().size() > 1)
			throw new WURCSExchangeException("Substituent having two or more children is NOT handled in the system.");

		/** set fragment connection */
		this.setLinkage(a_oRootOfFragments.getParentLinkage());
		this.setChild(a_oRootOfFragments);
		this.makeModification();
		
		this.setWURCSEdge(true);
		Bond a_oBond = a_oRootOfFragments.getParentLinkage().getBonds().get(0);
		
		for(WURCSEdge a_oEdge : this.getParentEdges()) {
			for(LinkagePosition a_oLIN : a_oEdge.getLinkages()) {
				a_oLIN.setProbabilityLower((double) a_oBond.getProbabilityLow() / 100);
				a_oLIN.setProbabilityUpper((double) a_oBond.getProbabilityHigh() / 100);
				a_oLIN.setProbabilityPosition( LinkagePosition.MODIFICATIONSIDE);
			}
		}
		
		if(a_oRootOfFragments.isSubstituent() && a_oRootOfFragments.getChildrenLinkages().isEmpty()) return;
		
		this.setWURCSEdge(false);		
	}
	
	public void clear() {
		this.a_aParents = new ArrayList<Residue>();
	}
}
