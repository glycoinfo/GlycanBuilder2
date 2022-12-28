/*
*   EuroCarbDB, a framework for carbohydrate bioinformatics
*
*   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
*   indicated by the @author tags or express copyright attribution
*   statements applied by the authors.  
*
*   This copyrighted material is made available to anyone wishing to use, modify,
*   copy, or redistribute it subject to the terms and conditions of the GNU
*   Lesser General Public License, as published by the Free Software Foundation.
*   A copy of this license accompanies this distribution in the file LICENSE.txt.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
*   for more details.
*
*   Last commit: $Rev$ by $Author$ on $Date::             $  
*/

package org.eurocarbdb.application.glycanbuilder.logutility;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

import java.awt.Frame;

import org.apache.log4j.Logger;
import org.eurocarbdb.application.glycanbuilder.util.ReportDialog;

/**
   Utility class with function to manage the logging of errors.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class LogUtils {
    private LogUtils() {}
    
    private static LoggerStorageIndex loggerStorageIndex=new LoggerStorageIndexImpl();
    
    public static void setLookupLogger(LoggerStorageIndex loggerStorageIndex){
    	LogUtils.loggerStorageIndex=loggerStorageIndex;
    }

    /**
       Specify if the logger should display a dialog with a report of
       the error
     */
    static public void setGraphicalReport(boolean flag) {
    	loggerStorageIndex.getLogger().setGraphicalReport(flag);
    }

    /**
       Return <code>true</code> if the logger should display a dialog
       with a report of the error
     */
    static public boolean getGraphicReport() {
    	return loggerStorageIndex.getLogger().getGraphicalReport();
    }
    

    /**
       Set the frame used to display the report dialog
     */
    static public void setReportOwner(Frame owner) {
    	loggerStorageIndex.getLogger().setFrameOwner(owner);
    }

    /**
       Return the frame used to display the report dialog
     */
    static public Frame getReportOwner() {
    	return loggerStorageIndex.getLogger().getFrameOwner();
    }

    /**
       Clear the information relative to the last occurred error
     */
    static public void clearLastError() {
    	loggerStorageIndex.getLogger().setHasLastError(false);
    	loggerStorageIndex.getLogger().setLastError("");
    	loggerStorageIndex.getLogger().setLastStackError("");
    }

    /**
       Return <code>true</code> if an error has been recently reported
     */
    static public boolean hasLastError() {
    	return loggerStorageIndex.getLogger().getHasLastError();
    }

    /**
       Return the error message corresponding to the last error
       reported
     */
    static public String getLastError() {
    	return loggerStorageIndex.getLogger().getLastError();
    }

    /**
       Return a string with the call stack corresponding to the last
       error reported
     */
    static public String getLastErrorStack() {
    	return loggerStorageIndex.getLogger().getLastStackError();
    }

    /**
       Return the error message corresponding to a given exception
     */
    static public String getError(Exception e) {
    return e.getMessage();
    }

    /**
       Return a string with the call stack corresponding to 
       a given exception
     */
    static public String getErrorStack(Exception e) {
    	StringWriter sw = new StringWriter();
    	e.printStackTrace(new PrintWriter(sw));       
    	return sw.getBuffer().toString();
    }

    /**
       Report a new error taking the information from the raised
       exception. Show a report dialog if needed.
     */
    static public void report(Exception e) {
    	if( e==null ) {
    		clearLastError();
    		return;
    	}
        
    	loggerStorageIndex.getLogger().setHasLastError(true);
    	loggerStorageIndex.getLogger().setLastError(getError(e));
    	
    	if(loggerStorageIndex.getLogger().getLastError()==null){
    		loggerStorageIndex.getLogger().setLastError("");
    	}
    	
    	loggerStorageIndex.getLogger().setLastStackError(getErrorStack(e));
    
    	if(loggerStorageIndex.getLogger().getLastStackError()==null){
    		loggerStorageIndex.getLogger().setLastStackError("");
    	}


    	if(loggerStorageIndex.getLogger().getGraphicalReport()) {
    		JOptionPane.showMessageDialog(null, e.getMessage(), "Error in GlycanBuilder2", JOptionPane.ERROR_MESSAGE);
    		new ReportDialog(loggerStorageIndex.getLogger().getFrameOwner(),loggerStorageIndex.getLogger().getLastStackError()).setVisible(true);
    	} else {
    		e.printStackTrace();
            // 20221130 S.TSUCHIYA comment out, investigating the error about java.io.FileNotFoundException
            //Logger.getLogger( LogUtils.class ).error("Error in GlycanBuilder2",e);
    	}
    }
}