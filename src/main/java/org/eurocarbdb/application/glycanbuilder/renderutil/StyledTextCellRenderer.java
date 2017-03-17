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

package org.eurocarbdb.application.glycanbuilder.renderutil;


import java.awt.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;


/**
   A {@link TableCellRenderer} that can render styled text with
   subscripts and superscripts.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class StyledTextCellRenderer extends JPanel implements TableCellRenderer, SVGUtils.Renderable {
    private static final long serialVersionUID = 0L;    

    private static final int MAX_LINE_SIZE = 30;

    protected Color unselectedForeground; 
    protected Color unselectedBackground; 
    protected static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1); 

    protected JTextPane textPane;
    protected GridBagLayout gbl;

    public StyledTextCellRenderer() {
    this(true);
    }

    public StyledTextCellRenderer(boolean opaque) {
    super(new GridBagLayout());

    // set text pane
    
    GridBagConstraints c = new GridBagConstraints();
    c.fill = c.HORIZONTAL;
    c.weightx = 1.; 
    add(textPane = new JTextPane(), c);
    
    this.setOpaque(opaque);
    textPane.setOpaque(opaque);
        textPane.setBorder(getNoFocusBorder());
    textPane.setAlignmentX(1.f);
    textPane.setAlignmentY(0.5f);

    // set styles
    StyledDocument doc = textPane.getStyledDocument();        

    //Style base_style = doc.addStyle("base",null);    
    Style sub_style = doc.addStyle("subscript",null);
    StyleConstants.setSubscript(sub_style,true);
    Style super_style = doc.addStyle("superscript",null);
    StyleConstants.setSuperscript(super_style,true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    
    super.invalidate();
    textPane.invalidate();
    
    // set colors
    
    if (isSelected) {
        textPane.setForeground(table.getSelectionForeground());
        textPane.setBackground(table.getSelectionBackground());
        super.setBackground(table.getSelectionBackground());
    }
    else {
        textPane.setForeground(table.getForeground());
        textPane.setBackground(table.getBackground());
        super.setBackground(table.getBackground());
    }
    
    textPane.setFont(table.getFont());

    // set borders

    if (hasFocus) {
            Border border = null;
            if( isSelected )
        border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");            
            if( border == null ) 
        border = UIManager.getBorder("Table.focusCellHighlightBorder");            
            super.setBorder(border);

        if (!isSelected && table.isCellEditable(row, column)) {
                Color fcol = UIManager.getColor("Table.focusCellForeground");
                if( fcol != null ) 
            textPane.setForeground(fcol);
                
        Color bcol = UIManager.getColor("Table.focusCellBackground");
                if( bcol != null ) 
            textPane.setBackground(bcol);
        }
    } else {
            super.setBorder(getNoFocusBorder());
    }

    // set text    
    setValue(value);

    // force resize
    textPane.setSize(Short.MAX_VALUE,Short.MAX_VALUE);

    // force the whole component to be visible
    textPane.setMinimumSize(textPane.getPreferredSize());

    return this;    
    }

    public Component getRendererComponent(java.awt.Font font, Color fg, Color bg, Object value) {

    super.invalidate();
    textPane.invalidate();

    // set properties
    textPane.setForeground(fg);
    textPane.setBackground(bg);
    textPane.setFont(font);

    // set text
    setValue(value);

    // force resize
    textPane.setSize(Short.MAX_VALUE,Short.MAX_VALUE);

    // force the whole component to be visible
    textPane.setMinimumSize(textPane.getPreferredSize());

    return this;
    }

  
    public void beforeRendering() {
    }   

    public Dimension getRenderableSize() {
    return getPreferredSize();
    }

    public void paintRenderable(Graphics2D g) {
    textPane.paint(g);
    }

    public void afterRendering() {
    }

    public String getValue() {
    return textPane.getText();
    }

    private void setValue(Object value) {        
    
    StyledDocument doc = textPane.getStyledDocument();        
    textPane.setText("");
    try {

        if( value!=null ) {
        int last_count = 0;
        String text = (String)value;
        StringBuilder buffer = new StringBuilder(text.length());           
        for (int i = 0; i < text.length(); i++) {
            if( text.charAt(i)=='_' || text.charAt(i)=='^' ) {
            // flush buffer
            if( buffer.length()>0 ) {            
                doc.insertString(doc.getLength(),buffer.toString(),doc.getStyle("base"));    
                buffer = new StringBuilder(text.length());                    
            }
            
            // get special text
            Style style = (text.charAt(i)=='_') ?doc.getStyle("subscript") :doc.getStyle("superscript");
            
            String toadd = "";
            if( i<(text.length()-1) ) {
                if( text.charAt(i+1)=='{' ) {
                int ind = TextUtils.findClosedParenthesis(text,i+2,'{','}');
                if( ind!=-1 ) {
                    toadd = text.substring(i+2,ind);
                    i = ind;
                }
                else {
                    toadd = text.substring(i+2);
                    i = text.length()-1;
                }
                }
                else {
                toadd = "" + text.charAt(i+1);
                i = i+1;
                }
                doc.insertString(doc.getLength(),toadd,style);
            }                                
            }
            else if( (buffer.length()-last_count)>MAX_LINE_SIZE && (text.charAt(i)==',' || text.charAt(i)==' ') ) {
            buffer.append('\n');    
            for( ;text.charAt(i+1)==' '; i++ );
            last_count = buffer.length();
            }
            else
            buffer.append(text.charAt(i));    
        }
        
        // flush buffer
        if( buffer.length()>0 ) 
            doc.insertString(doc.getLength(),buffer.toString(),doc.getStyle("base"));    
        }
    }
    catch(Exception e) {        
        LogUtils.report(e);
    }
    }
    

    private static Border getNoFocusBorder() {
    return noFocusBorder;
    }
    
    /*
    public void setForeground(Color c) {
        super.setForeground(c); 
        unselectedForeground = c; 
    }

    public void setBackground(Color c) {
        super.setBackground(c); 
        unselectedBackground = c; 
    }

    public void updateUI() {
        super.updateUI(); 
    setForeground(null);
    setBackground(null);
    }
    
    
    
    // The following methods are overridden as a performance measure to 
    // to prune code-paths are often called in the case of renders
    // but which we know are unnecessary.  Great care should be taken
    // when writing your own renderer to weigh the benefits and 
    // drawbacks of overriding methods like these.

    public boolean isOpaque() { 
    Color back = getBackground();
    Component p = getParent(); 
    if (p != null) { 
        p = p.getParent(); 
    }
    // p should now be the JTable. 
    boolean colorMatch = (back != null) && (p != null) && 
        back.equals(p.getBackground()) && 
            p.isOpaque();
    return !colorMatch && super.isOpaque(); 
    }

    public void invalidate() {}

    public void validate() {}

    public void revalidate() {}

    public void repaint(long tm, int x, int y, int width, int height) {}

    public void repaint(Rectangle r) { }

    public void repaint() { }
    */
     
}
