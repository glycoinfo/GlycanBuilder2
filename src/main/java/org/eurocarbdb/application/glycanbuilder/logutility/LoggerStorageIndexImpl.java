package org.eurocarbdb.application.glycanbuilder.logutility;

public class LoggerStorageIndexImpl implements LoggerStorageIndex{
	private static LoggerStorageImpl loggerStorage=new LoggerStorageImpl();
	
	@Override
	public LoggerStorage getLogger(){
		return loggerStorage;
	}
}
