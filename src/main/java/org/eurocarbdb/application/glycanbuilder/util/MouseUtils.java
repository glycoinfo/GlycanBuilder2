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

package org.eurocarbdb.application.glycanbuilder.util;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

/**
   Utility class containing methods to facilitate the used of the
   mouse functions

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class MouseUtils {

    private MouseUtils() {}
    
    private static final int MOD_MASK = MouseEvent.CTRL_MASK | MouseEvent.SHIFT_MASK | MouseEvent.ALT_MASK | MouseEvent.META_MASK | MouseEvent.ALT_GRAPH_MASK;   

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to select objects has been pressed (left
    click) 
        @param e the {@link MouseEvent} information sent to the mouse
    events listener
    */
    static public boolean isSelectTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==1 && (e.getModifiers() & MOD_MASK)==0 );
    }

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to select additional objects has been
    pressed (ctrl + left click)
    @param e the {@link MouseEvent} information sent to the mouse
    events listener  
    */ 
    static public boolean isAddSelectTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==1 && ((e.getModifiers() & MOD_MASK)==MouseEvent.CTRL_MASK || (e.getModifiers() & MOD_MASK)==MouseEvent.META_MASK) );
    }

   /** 
       Return <code>true</code> if the combination of mouse and
       keyboard buttons used to select all objects in a range has been
       pressed (shift + left click)
       @param e the {@link MouseEvent} information sent to the mouse
       events listener  
   */  
    static public boolean isSelectAllTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==1 && (e.getModifiers() & MOD_MASK)==MouseEvent.SHIFT_MASK );
    }

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to perform actions has been pressed
    (double click)
        @param e the {@link MouseEvent} information sent to the mouse
    events listener  
    */  
    static public boolean isActionTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2 && (e.getModifiers() & MOD_MASK)==0 );
    }

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to perform special actions has been
    pressed (ctrl + double click)
        @param e the {@link MouseEvent} information sent to the mouse
    events listener  
    */  
     static public boolean isCtrlActionTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2 && (e.getModifiers() & MOD_MASK)==MouseEvent.CTRL_MASK );
    }

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to press buttons has been pressed (left
    click) 
        @param e the {@link MouseEvent} information sent to the mouse
    events listener
    */    
    static public boolean isPushTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getID()==MouseEvent.MOUSE_PRESSED && (e.getModifiers() & MOD_MASK)==0 );
    }

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to press buttons with a special function
    has been pressed (ctrl + left click)
        @param e the {@link MouseEvent} information sent to the mouse
    events listener  
    */    
    static public boolean isCtrlPushTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getID()==MouseEvent.MOUSE_PRESSED && (e.getModifiers() & MOD_MASK)==MouseEvent.CTRL_MASK );
    }

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to move objects has been pressed (shift
    + left click) 
        @param e the {@link MouseEvent} information sent to the mouse
    events listener  
    */    
    static public boolean isMoveTrigger(MouseEvent e) {
    return (e.getButton()==MouseEvent.BUTTON1 && e.getID()==MouseEvent.MOUSE_PRESSED && (e.getModifiers() & MOD_MASK)==MouseEvent.SHIFT_MASK );
    }

    /** 
    Return <code>true</code> if the combination of mouse and
    keyboard buttons used to open popup menus has been pressed
    (right click, or ctrl + click in Mac Os)
    @param e the {@link MouseEvent} information sent to the mouse
    events listener  
    */    
    static public boolean isPopupTrigger(MouseEvent e)  {
    return e.isPopupTrigger();
    }

    /**
       Return <code>true</code> if no mouse buttons are pressed
       @param e the {@link MouseEvent} information sent to the mouse
       events listener  
     */
    static public boolean isNothingPressed(MouseEvent e) {
    return ((e.getModifiers() & MOD_MASK)==0);    
    }

    /**
       Return <code>true</code> if the ctrl button was pressed
       @param e the {@link MouseEvent} information sent to the mouse
       events listener
     */
    static public boolean isCtrlPressed(MouseEvent e) {
    return ( (e.getModifiers() & MOD_MASK)==e.CTRL_MASK );
    }

 
}