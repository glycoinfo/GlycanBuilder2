package org.eurocarbdb.application.glycanbuilder;

public class ResidueHolder {
	public Residue res;
	/**
	 * Each residue copied, and its copy, when a copy is to be relinked afterwards (#340); null when
	 * it is not. Keyed by identity: two residues of the same kind are two residues.
	 */
	public java.util.Map<Residue, Residue> copies;
}
