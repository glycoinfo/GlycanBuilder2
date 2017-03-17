package org.eurocarbdb.application.glycanbuilder.logutility;

import java.awt.Frame;

public interface LoggerStorage{
	public void setGraphicalReport(boolean graphicalReport);
	public boolean getGraphicalReport();
	
	public void setFrameOwner(Frame theOwner);
	public Frame getFrameOwner();
	
	public void setHasLastError(boolean hasLastError);
	public boolean getHasLastError();
	
	public void setLastError(String lastError);
	public String getLastError();
	
	public void setLastStackError(String lastStackError);
	public String getLastStackError();
}
