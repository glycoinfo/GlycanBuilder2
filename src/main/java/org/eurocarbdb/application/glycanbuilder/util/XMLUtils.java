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
import java.net.*;
import java.io.*;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
   Utility class with functions to facilitate the parsing of XML files
   using the DOM architecture.
   
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class XMLUtils {

    private XMLUtils() {}

    /**
       Create an empty DOM document.
     */
    static public Document newDocument() {
    try {
        DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }    
    catch(Exception e) {
        LogUtils.report(e);
        return null;
    }
    }

    /**
       Read a DOM document from a string in XML format.
     */
    static public Document read(String data) {
    try {        
        DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        return builder.parse(new ByteArrayInputStream(data.getBytes()));
    }
    catch(Exception e) {
        LogUtils.report(e);
        return null;
    }  
    }

    /**
       Read a DOM document from an array of bytes in XML format.
     */
    static public Document read(byte[] data) {
    try {        
        DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        return builder.parse(new ByteArrayInputStream(data));
    }
    catch(Exception e) {
        LogUtils.report(e);
        return null;
    }  
    }

    /**
       Read a DOM document from an input stream.
     */
    static public Document read(InputStream is) {
    try {        
        DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(is);
    }
    catch(Exception e) {
        LogUtils.report(e);
        return null;
    }  
    }

    /**
       Write a DOM document to an output stream.
     */
    static public boolean write(OutputStream os, Document d) {
    try {
        if( os==null || d==null )
        return false;

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        
        DOMSource source = new DOMSource(d);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        StreamResult result = new StreamResult(bw); // if you open a StreamResult with a File and the filename contain spaces you get an exception

        transformer.transform(source, result);      
        return true;
    }
    catch(Exception e) {
        LogUtils.report(e);
        return false;
    }
    }
    
    /**
       Return the first children of a node with a given name.
       @return <code>null</code> if no child is found
     */
    static public Node findChild(Node node, String child_name) {
    if( node==null )
        return null;
    
    NodeList children = node.getChildNodes();
    for( int i=0; i<children.getLength(); i++ ) {
        Node child = children.item(i);
        if( child.getNodeName().equals(child_name) )
        return child;
    }
    return null;
    }

    /**
       Return the first children of a node with a given name.
       @throws Exception if no child is found
     */
    static public Node assertChild(Node node, String child_name) throws Exception {
    Node ret = findChild(node,child_name);
    if( ret==null )
        throw new Exception("Cannot find " + child_name + " node");
    return ret;
    }

    /**
       Return a list of all the children of a given node
     */
    static public Vector<Node> findAllChildren(Node node) {
    if( node==null )
        return null;

    Vector<Node> found_children = new Vector<Node>();
    NodeList children = node.getChildNodes();
    for( int i=0; i<children.getLength(); i++ ) {
        Node child = children.item(i);
        if( child.getNodeType()==Node.ELEMENT_NODE )
        found_children.add(child);
    }
    return found_children;
    }

    /**
       Return a list of all the children of a given node with a
       specific name
     */
    static public LinkedList<Node> findAllChildren(Node node, String child_name) {
    if( node==null )
        return null;

    LinkedList<Node> found_children = new LinkedList<Node>();
    NodeList children = node.getChildNodes();
    for( int i=0; i<children.getLength(); i++ ) {
        Node child = children.item(i);
        if( child.getNodeName().equals(child_name)) found_children.addLast(child);
    }
    return found_children;
    }

    /**
       Return the first children of a node with a given name. If no
       child has that name a new one of type element is added to the
       node and returned. 
     */
    static public Node findChildAssertive(Node node, String child_name) {
    if( node==null )
        return null;
    
    Node child = findChild(node,child_name);
    if( child!=null ) 
        return child;    
    return node.appendChild(node.getOwnerDocument().createElement(child_name));
    }

    /**
       Set the text of a specified node. A text section is added to
       the node if necessary.
     */
    static public void setText(Node node, String value) {
    if( node==null )
        return;

    Node child = findChild(node,"#text");
    if( child==null ) 
        node.appendChild(node.getOwnerDocument().createTextNode(value));
    else
        child.setNodeValue(value);
    }

    /**
       Return the text associated with a node, or <code>null</code> if
       no text section is present.
     */
    static public String getText(Node node) {
    if( node==null )
        return null;

    Node child = findChild(node,"#text");
    if( child==null ) 
        return null;
    return child.getNodeValue();
    }
    
    /**
       Return the value of an attribute of a node or an empty string
       if the attribute is not present.
     */
    static public String getAttribute(Node node, String att_name) {
    if( node==null )
        return "";
    return getText(node.getAttributes().getNamedItem(att_name));
    }

    /**
       Return the value of an attribute of a node as an integer number
       or <code>null</code> if the attribute is not present.
     */
    static public Integer getIntegerAttribute(Node node, String att_name) {
    if( node==null )
        return null;
    String text = getText(node.getAttributes().getNamedItem(att_name));
    if( text==null )
        return null;
    return Integer.valueOf(text);
    }

    /**
       Return the value of an attribute of a node as a real number
       or <code>null</code> if the attribute is not present.
     */
    static public Double getDoubleAttribute(Node node, String att_name) {
    if( node==null )
        return null;
    String text = getText(node.getAttributes().getNamedItem(att_name));
    if( text==null )
        return null;
    return Double.valueOf(text);
    }

    /**
       Return the value of an attribute of a node as a boolean variable
       or <code>null</code> if the attribute is not present.
     */
    static public Boolean getBooleanAttribute(Node node, String att_name) {
    if( node==null )
        return null;
    String text = getText(node.getAttributes().getNamedItem(att_name));
    if( text==null )
        return null;
    return Boolean.valueOf(text);
    }
}
