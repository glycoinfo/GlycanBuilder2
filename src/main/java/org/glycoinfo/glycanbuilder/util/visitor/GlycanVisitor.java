package org.glycoinfo.glycanbuilder.util.visitor;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;

public interface GlycanVisitor {
	public abstract void visit ( Residue a_oResidue);
	public abstract void visit ( Linkage a_oLinkage);
	
	public abstract void start ( Glycan a_oGlycan);
	
	public abstract void clear();
}
