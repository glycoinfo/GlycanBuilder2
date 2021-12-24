<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"/>

  <xsl:template match="/">
    <xsl:text>%Residues&#x9;AnomC&#x9;Ring&#x9;Type&#x9;First&#x9;Last&#x9;NoMe&#x9;NoAc&#x9;noLinks&#x9;LinkPos&#x9;ChargesPos&#x9;Composition&#10;</xsl:text>

    <xsl:for-each select="residues/class/sugar/ring/fragment">
      <!-- residues -->      
      <xsl:choose>
	<xsl:when test="count(../../isomer) = 0"> 
	  <xsl:text>-</xsl:text> 
	</xsl:when>
	<xsl:when test="count(../../isomer) &gt; 0"> 
	  <xsl:for-each select="../../isomer">
	    <xsl:value-of select="@abbr"/>	   
	    <xsl:if test="position() != last()">
	      <xsl:text>,</xsl:text> 
	    </xsl:if>
	  </xsl:for-each>
	</xsl:when>
      </xsl:choose>
      <xsl:text>&#x9;</xsl:text> 

      <!-- AnomC -->
      <xsl:value-of select="../@anomeric_center"/> 
      <xsl:text>&#x9;</xsl:text> 

      <!-- RingSize -->
      <xsl:choose>
	<xsl:when test="../@size = 6" > 
	  <xsl:text>p</xsl:text> 
	</xsl:when>
	<xsl:when test="../@size = 5" >
	  <xsl:text>f</xsl:text> 
	</xsl:when>
      </xsl:choose>
      <xsl:text>&#x9;</xsl:text> 

      <!-- Type -->
      <xsl:value-of select="@type"/> 
      <xsl:text>&#x9;</xsl:text> 

      <!-- First pos -->
      <xsl:value-of select="@cleav1"/> 
      <xsl:text>&#x9;</xsl:text> 

      <!-- Last pos -->
      <xsl:value-of select="@cleav2"/> 
      <xsl:text>&#x9;</xsl:text>      

      <!-- No methyls -->
      <xsl:value-of select="@pm"/>
      <xsl:text>&#x9;</xsl:text> 

      <!-- No acethyls -->
      <xsl:value-of select="@pac"/>
      <xsl:text>&#x9;</xsl:text> 

      <!-- No pos -->
      <xsl:value-of select="@pos"/>
      <xsl:text>&#x9;</xsl:text> 
      
      <!-- Positions -->
      <xsl:choose>
	<xsl:when test="count(position) = 0"> 
	  <xsl:text>-</xsl:text> 
	</xsl:when>
	<xsl:when test="count(position) &gt; 0"> 
	  <xsl:for-each select="position">
	    <xsl:choose>
	      <xsl:when test="@type = 'C'" > 
		<xsl:value-of select="@pos"/>
	      </xsl:when>
	      <xsl:when test="@type = 'N'" > 
		<xsl:text>N</xsl:text> 
	      </xsl:when>
	    </xsl:choose>
	    <xsl:if test="position() != last()">
	      <xsl:text>,</xsl:text> 
	    </xsl:if>
	  </xsl:for-each>
	</xsl:when>
      </xsl:choose>
      <xsl:text>&#x9;</xsl:text> 

      <!-- Positions -->
      <xsl:choose>
	<xsl:when test="count(charge) = 0"> 
	  <xsl:text>-</xsl:text> 
	</xsl:when>
	<xsl:when test="count(charge) &gt; 0"> 
	  <xsl:for-each select="charge">
	    <xsl:value-of select="@pos"/>
	    <xsl:if test="position() != last()">
	      <xsl:text>,</xsl:text> 
	    </xsl:if>
	  </xsl:for-each>
	</xsl:when>
      </xsl:choose>
      <xsl:text>&#x9;</xsl:text> 

      <!-- Composition -->
      <xsl:for-each select="composition">
	<xsl:choose>
	  <xsl:when test="@count &gt; 0"> 
	    <xsl:value-of select="@element"/> 
	    <xsl:value-of select="@count"/> 
	  </xsl:when>
	</xsl:choose>
      </xsl:for-each>
      <xsl:text>&#10;</xsl:text> 

    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
