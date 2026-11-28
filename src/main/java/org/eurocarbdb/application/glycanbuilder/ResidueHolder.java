package org.eurocarbdb.application.glycanbuilder;

public class ResidueHolder {
	public Residue res;
	/**
	 * Each residue copied, and its copy, when a copy is to be relinked afterwards (#340); null when
	 * it is not. Keyed by identity: two residues of the same kind are two residues.
	 */
	public java.util.Map<Residue, Residue> copies;

	/**
	 * The repeats read so far and not yet closed, the innermost last, for the GWS reader (#394). GWS
	 * is written depth first, so each "]" closes the last "[" still open, as a bracket closes a
	 * bracket. A single slot lost the outer repeat as soon as an inner one opened, and the outer "]"
	 * then had nothing to close: a structure with repeats nested in a repeat could be saved and not
	 * opened again.
	 */
	public java.util.Deque<Residue> openRepeats = new java.util.ArrayDeque<Residue>();
}
