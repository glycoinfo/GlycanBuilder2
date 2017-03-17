package org.eurocarbdb.application.glycanbuilder.massutil;

public interface MassAware{
	public double computeMass();
	public double computeMass(String type);
	public boolean equals(MassAware aware);
	public String getName();
}
