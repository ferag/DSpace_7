<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:cerif="https://purl.org/pe-repo/cerif-profile/1.0/">
	
	<xsl:param name="nestedMetadataPlaceholder" />
	<xsl:param name="converterSeparator" />
	<xsl:param name="idPrefix" />
	
	<xsl:template match="cerif:Equipment">
		<dim:dim>
		
		    <dim:field mdschema="dc" element="type" >
                <xsl:value-of select="cerif:Type" />
            </dim:field>
			
			<dim:field mdschema="dc" element="title" >
				<xsl:value-of select="cerif:Name" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="acronym">
				<xsl:value-of select="cerif:Acronym" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="internalid">
				<xsl:value-of select="cerif:Identifier[@type = 'Institution assigned unique equipment identifier']" />
			</dim:field>
			
			<dim:field mdschema="dc" element="description">
				<xsl:value-of select="cerif:Description" />
			</dim:field>
			
			<dim:field mdschema="crisequipment" element="ownerou">
				<xsl:value-of select="cerif:Owner/cerif:OrgUnit/cerif:Name" />
			</dim:field>
			
			<dim:field mdschema="crisequipment" element="ownerrp">
				<xsl:value-of select="cerif:Owner/cerif:Person/@displayName" />
			</dim:field>
			
			<xsl:for-each select="cerif:Subject">
                <dim:field mdschema="dc" element="subject" qualifier="ocde">
                    <xsl:value-of select="current()"/>
                </dim:field>
            </xsl:for-each>
            
            <dim:field mdschema="perucris" element="researchLine">
                <xsl:value-of select="cerif:ResearchLine" />
            </dim:field>
            
            <dim:field mdschema="perucris" element="manufacturingCountry">
                <xsl:value-of select="cerif:ManufacturingCountry" />
            </dim:field>
            
            <dim:field mdschema="perucris" element="date" qualifier="manufacturing">
                <xsl:value-of select="cerif:ManufacturingDate" />
            </dim:field>
            
            <dim:field mdschema="perucris" element="date" qualifier="acquisition">
                <xsl:value-of select="cerif:AcquisitionDate" />
            </dim:field>
            
            <dim:field mdschema="oairecerif" element="amount">
                <xsl:value-of select="cerif:AcquisitionAmount" />
            </dim:field>
            
            <dim:field mdschema="oairecerif" element="amount" qualifier="currency">
                <xsl:value-of select="cerif:AcquisitionAmount/@currency" />
            </dim:field>
            
		</dim:dim>
	</xsl:template>
	
	
	<xsl:template name="nestedMetadataValue">
		<xsl:param name = "value" />
		<xsl:choose>
			<xsl:when test="$value">
				<xsl:value-of select="$value" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$nestedMetadataPlaceholder"/> 
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>