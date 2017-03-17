package org.eurocarbdb.application.glycanbuilder.logutility;

import java.awt.Frame;

public class LoggerStorageImpl implements LoggerStorage{
	private boolean graphicalReport=false;
	private Frame theOwner=null;    
	private boolean hasLastError=false;
	private String lastError="";
	private String lastStackError="";
	
	@Override
	public void setGraphicalReport(boolean graphicalReport){
		this.graphicalReport=graphicalReport;
	}

	@Override
	public boolean getGraphicalReport(){
		return graphicalReport;
	}

	@Override
	public void setFrameOwner(Frame theOwner){
		this.theOwner=theOwner;
	}

	@Override
	public Frame getFrameOwner(){
		return theOwner;
	}

	@Override
	public void setHasLastError(boolean hasLastError){
		this.hasLastError=hasLastError;
	}

	@Override
	public boolean getHasLastError(){
		return hasLastError;
	}

	@Override
	public void setLastError(String lastError){
		this.lastError=lastError;
	}

	@Override
	public String getLastError(){
		return lastError;
	}

	@Override
	public void setLastStackError(String lastStackError){
		this.lastStackError=lastStackError;
	}

	@Override
	public String getLastStackError(){
		return lastStackError;
	}
}