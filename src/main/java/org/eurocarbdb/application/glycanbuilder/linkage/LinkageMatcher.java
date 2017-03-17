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


package org.eurocarbdb.application.glycanbuilder.linkage;

import java.util.regex.*;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;


/**
   Match a rule to a linkage. Is used by the {@link
   LinkageStyleDictionary} to decide the style of the linkage for
   rendering. The base class has different implementations depending
   on the rules applied.

   @see LinkageStyle

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public abstract class LinkageMatcher {

    /**
       Return a matcher corresponding the rule specified by the
       string. A rule is a boolean predicate containing boolean
       operators and conditions matching certain information about the
       linkage.  The standard binary operators (&amp; = and, &brvbar;
       = or, &#94; = xor, &#33; = not) apply. Normal parenthesis can
       be used for the boolean operations.

       <p> 
       
       The possible linkage conditions are: 
       
       <ul>
         <li>1: always true </li> 
         <li>0: always false </li> 
         <li>re: parent is a reducing end marker </li> 
         <li>ps: parent is a monosaccharide </li> 
         <li>pr: parent is a ring fragment </li> 
         <li>px: parent is a special residue </li> 
         <li>pc: parent is a glycosidic cleaveage</li> 
         <li>pb: parent is a bracket residue</li> 
         <li>pp: parent is a repeating block indicator</li> 
         <li>pa: parent is an anchor point</li> 
     <li>cs: child is a monosaccharide </li> 
         <li>cr: child is a ring fragment </li> 
         <li>cx: child is a special residue </li> 
         <li>cc: child is a glycosidic cleaveage</li> 
         <li>cp: child is a repeating block indicator</li> 
       </ul>
       
     */
    static public LinkageMatcher parse(String init) {
    try {
        return OperatorFactory.fromString(init);
    }
    catch(Exception e) {
        LogUtils.report(e);
        return null;
    }
    }

    /**
       Return <code>true</code> if the rule represented by this object
       matches the linkage information
       @param parent the parent residue in the linkage
       @param link the linkage for which the style should be retrieved
       @param child the child residue in the linkage       
     */
    abstract public boolean matches(Residue parent, Linkage link, Residue child);
}

class OperatorFactory {

    static public LinkageMatcher fromString(String init) throws Exception{
    init = TextUtils.trim(init);
    init = TextUtils.removeTrailingParentheses(init);
    init = TextUtils.trim(init);

    if( init.equals("") )
        return new TrueCondition();
    
    // look for binary operators
    int ind = TextUtils.findFirstOfWithParentheses(init,"&|^");
    if( ind!=-1 ) {
        String lop = init.substring(0,ind);
        char   op  = init.charAt(ind);
        String rop = init.substring(ind+1);
        
        if( op=='&' )
        return new AndOperator(fromString(lop),fromString(rop));
        if( op=='|' )
        return new OrOperator(fromString(lop),fromString(rop));
        if( op=='^' )
        return new XOrOperator(fromString(lop),fromString(rop));
        throw new Exception("Memory error");
    }
    
    // look for unary operators
    if( init.startsWith("!") )
        return new NegationOperator(fromString(init.substring(1)));

    // parse condition
    return ConditionFactory.fromString(init);    
    }
}


class NegationOperator extends LinkageMatcher {
    protected LinkageMatcher op;

    public NegationOperator(LinkageMatcher _op) {
    op = _op;
    }
    
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return !op.matches(parent,link,child);
    }
    
    public String toString() {
    return "!(" + op.toString() + ")";
    }
}

class AndOperator extends LinkageMatcher {

    protected LinkageMatcher op1;
    protected LinkageMatcher op2;

    public AndOperator(LinkageMatcher _op1, LinkageMatcher _op2) {
    op1 = _op1;
    op2 = _op2;
    }

    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (op1.matches(parent,link,child) && op2.matches(parent,link,child));
    }

    public String toString() {
    return "(" + op1.toString() + ")&(" + op2.toString() + ")";
    }
}

class OrOperator extends LinkageMatcher {

    protected LinkageMatcher op1;
    protected LinkageMatcher op2;

    public OrOperator(LinkageMatcher _op1, LinkageMatcher _op2) {
    op1 = _op1;
    op2 = _op2;
    }

    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (op1.matches(parent,link,child) | op2.matches(parent,link,child));
    }

    public String toString() {
    return "(" + op1.toString() + ")|(" + op2.toString() + ")";
    }
}

class XOrOperator extends LinkageMatcher {

    protected LinkageMatcher op1;
    protected LinkageMatcher op2;

    public XOrOperator(LinkageMatcher _op1, LinkageMatcher _op2) {
    op1 = _op1;
    op2 = _op2;
    }

    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (op1.matches(parent,link,child) ^ op2.matches(parent,link,child));
    }

    public String toString() {
    return "(" + op1.toString() + ")^(" + op2.toString() + ")";
    }
}

class ConditionFactory {

    static public LinkageMatcher fromString(String init) throws Exception {
    
    if( TextUtils.findFirstOf(init,"&|^!")!=-1 )
        throw new Exception("Invalid condition format: " + init);

    init = TextUtils.trim(init);
    if( init.equals("1") || init.equals("true") )
        return new TrueCondition();
    if( init.equals("0") || init.equals("false") )
        return new FalseCondition();    

    int ind = init.indexOf('=');
    if( ind==-1 ) {       
        String attribute = init;
        if( attribute.equals("re") )
        return new ReducingEndCondition();
        if( attribute.equals("ps") )
        return new ParentIsSaccharideCondition();
        if( attribute.equals("pr") )
        return new ParentIsRingFragmentCondition();
        if( attribute.equals("px") )
        return new ParentIsSpecialCondition();
        if( attribute.equals("pc") )
        return new ParentIsCleavageCondition();
        if( attribute.equals("pb") )
        return new ParentIsBracketCondition();
        if( attribute.equals("pp") )
        return new ParentIsRepetitionCondition();
        if( attribute.equals("pa") )
        return new ParentIsAttachPointCondition();
        if( attribute.equals("cs") )
        return new ChildIsSaccharideCondition();
        if( attribute.equals("cr") )
        return new ChildIsRingFragmentCondition();
        if( attribute.equals("cx") )
        return new ChildIsSpecialCondition();
        if( attribute.equals("cc") )
        return new ChildIsCleavageCondition();
        if( attribute.equals("cb") )
        return new ChildIsBracketCondition();
        if( attribute.equals("cp") )
        return new ChildIsRepetitionCondition();

        throw new Exception("Invalid attribute name: <" + attribute + ">");
    }
    else {
        String attribute = init.substring(0,ind);
        String value = init.substring(ind+1);

        if( attribute.equals("pt") ) // parent type
        return new ParentTypeCondition(value);        
        if( attribute.equals("ct") ) // child type
        return new ChildTypeCondition(value);
        
        if( attribute.equals("lp") ) // linkage position
        return new LinkagePositionCondition(value);
        if( attribute.equals("as") ) // child anomeric state
        return new ChildAnomericStateCondition(value);
        if( attribute.equals("ac") ) // child anomeric carbon
        return new ChildAnomericCarbonCondition(value);      

        throw new Exception("Invalid attribute name: <" + attribute + ">");
    }
    }
}

class TrueCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return true;
    }

    public String toString() {
    return "1";
    }
}

class FalseCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return false;
    }

    public String toString() {
    return "0";
    }

}


class ReducingEndCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isReducingEnd());
    }

    public String toString() {
    return "re";
    }
}

class ParentIsSaccharideCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isSaccharide());
    }

    public String toString() {
    return "ps";
    }
}

class ParentIsRingFragmentCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isRingFragment());
    }

    public String toString() {
    return "pr";
    }
}

class ParentIsSpecialCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isSpecial());
    }

    public String toString() {
    return "px";
    }
}

class ParentIsCleavageCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isCleavage());
    }

    public String toString() {
    return "pc";
    }
}

class ParentIsBracketCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isBracket());
    }

    public String toString() {
    return "pb";
    }
}


class ParentIsRepetitionCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isRepetition());
    }

    public String toString() {
    return "pp";
    }
}


class ParentIsAttachPointCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && parent.isAttachPoint());
    }

    public String toString() {
    return "pa";
    }
}

class ChildIsSaccharideCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (child!=null && child.isSaccharide());
    }

    public String toString() {
    return "cs";
    }
}


class ChildIsRingFragmentCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (child!=null && child.isRingFragment());
    }

    public String toString() {
    return "cr";
    }
}


class ChildIsSpecialCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (child!=null && child.isSpecial());
    }

    public String toString() {
    return "cx";
    }
}

class ChildIsCleavageCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (child!=null && child.isCleavage());
    }

    public String toString() {
    return "cc";
    }
}

class ChildIsBracketCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (child!=null && child.isBracket());
    }

    public String toString() {
    return "cb";
    }
}

class ChildIsRepetitionCondition extends LinkageMatcher {
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (child!=null && child.isRepetition());
    }

    public String toString() {
    return "cp";
    }
}


abstract class ValuedCondition extends LinkageMatcher {
    protected String  regex;
    protected Pattern pattern;
    
    ValuedCondition(String _regex) {
    regex = _regex;
    pattern = Pattern.compile(regex);
    }

    public boolean matches(String value) {
    return pattern.matcher(value).matches();
    }
}

class ParentTypeCondition extends ValuedCondition {
    
    public ParentTypeCondition(String _regex) {
    super(_regex);
    }

    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (parent!=null && matches(parent.getType().getName()));
    }

    public String toString() {
    return "pt=" + regex;
    }
}

class ChildTypeCondition extends ValuedCondition {
    
    public ChildTypeCondition(String _regex) {
    super(_regex);
    }

    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (child!=null && matches(child.getType().getName()));
    }

    public String toString() {
    return "ct=" + regex;
    }
}

class LinkagePositionCondition extends ValuedCondition {
      
    public LinkagePositionCondition(String _regex) {
    super(_regex);
    }
    
    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (link!=null && matches(""+link.getParentPositionsString()));
    }

    public String toString() {
    return "lp=" + regex;
    }
}

class ChildAnomericStateCondition extends ValuedCondition {

    public ChildAnomericStateCondition(String _regex) {
    super(_regex);
    }

    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (link!=null && matches(""+child.getAnomericState()));
    }

    public String toString() {
    return "as=" + regex;
    }
}

class ChildAnomericCarbonCondition extends ValuedCondition {
      
    public ChildAnomericCarbonCondition(String _regex) {
    super(_regex);
    }

    public boolean matches(Residue parent, Linkage link, Residue child) {
    return (link!=null && matches(""+link.getChildPositionsString()));
    }

    public String toString() {
    return "ac=" + regex;
    }
}