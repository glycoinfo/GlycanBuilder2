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

/**
   Object used to identify the position of a residue around its
   parent. The positions are identified by the rotation angle
   expressed in degrees.
   
   @see PositionManager
   @see GlycanRendererAWT
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class ResAngle {

    protected int angle;
    
    /**
       Create a ResAngle object identifying position 0.
     */
    public ResAngle() {
    angle = 0;
    }
    
    /**
       Create a ResAngle object identifying the position at the given angle
     */
    public ResAngle(int _angle) {
    angle = normalize(_angle);   
    angle -= ((angle+180)%45);
    }

    /**
       Create a ResAngle object identifying the position at the given angle
     */
    public ResAngle(String init) {
    angle = normalize(Integer.parseInt(init));   
    angle -= ((angle+180)%45);
    }    

    static private int normalize(int angle) {
    while( angle<=-180 )
        angle += 360;
    while( angle>180 )
        angle -= 360;
    return angle;
    }

    public boolean equals(Object other) {
    if( !(other instanceof ResAngle) )
        return false;
    return (angle==((ResAngle)other).angle);
    }

    public int hashCode() {
    return Integer.valueOf(angle).hashCode();
    }

    public boolean equals(int other) {
    return (angle==normalize(other));
    }

    /**
       Combine the two objects by summing the position angles.
       @return a new object with the resulting position
     */    
    public ResAngle combine(ResAngle other) {
    ResAngle ret = new ResAngle();
    ret.angle = normalize(this.angle + other.angle);
    return ret;
    }

    /**
       Combine this object with a speficied angle by summing the
       position angles.
       @return a new object with the resulting position
     */  
    public ResAngle combine(int other_angle) {
    ResAngle ret = new ResAngle();
    ret.angle = normalize(this.angle + other_angle);
    return ret;
    }
    
    /**
       Return <code>true</code> if the two positions differs by 180
       degrees.
     */
    public boolean isOpposite(ResAngle other) {
    return (Math.abs(this.angle - other.angle)==180);
    }

    /**
       Return the position that is at 180 degrees from the current
       one.
    */
    public ResAngle opposite() {
    ResAngle ret = new ResAngle();
    ret.angle = normalize(this.angle+180);
    return ret;
    }

    /**
       Return the angle representing this position in radiants.
     */
    public double getAngle() {
    return Math.PI*(double)angle/180.;
    }

    /**
       Return the angle representing this position in degrees.
     */
    public int getIntAngle() {
    return angle;
    }

    public String toString() {
    return Integer.toString(angle);
    }

}