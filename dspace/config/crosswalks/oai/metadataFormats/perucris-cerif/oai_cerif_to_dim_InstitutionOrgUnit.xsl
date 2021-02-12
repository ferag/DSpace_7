<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:cerif="https://purl.org/pe-repo/cerif-profile/1.0/"
    xmlns:pt="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"
    xmlns:ft="https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types">
    
    <xsl:param name="nestedMetadataPlaceholder" />
    <xsl:param name="converterSeparator" />
    <xsl:param name="idPrefix" />

    <xsl:template match="cerif:OrgUnit">
        <dim:dim>
            
            <xsl:for-each select="cerif:Name">
                <xsl:choose>
                    <xsl:when test="position() = 1">
                        <dim:field mdschema="dc" element="title" >
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <dim:field mdschema="organization" element="legalName">
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:otherwise>
                </xsl:choose>
                <dim:field mdschema="dc" element="title" >
                    <xsl:value-of select="cerif:Name" />
                </dim:field>
            </xsl:for-each>
            
            <xsl:for-each select="cerif:Acronym">
                <dim:field mdschema="oairecerif" element="acronym">
                    <xsl:value-of select="current()" />
                </dim:field>
            </xsl:for-each>
    
            <xsl:if test="cerif:Type">
                <dim:field mdschema="dc" element="type" >
                    <xsl:value-of select="concat('cerifToOrgUnitTypes',$converterSeparator,cerif:Type)" />
                </dim:field>
            </xsl:if>
            
            <dim:field mdschema="organization" element="parentOrganization">
                <xsl:if test="cerif:PartOf/cerif:OrgUnit/@id">
                    <xsl:attribute name="authority"><xsl:value-of select="concat($idPrefix,cerif:PartOf/cerif:OrgUnit/@id)"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="cerif:PartOf/cerif:OrgUnit/cerif:Name"/>
            </dim:field>
            
            <xsl:for-each select="cerif:Identifier">
                <xsl:choose>
                    <xsl:when test="not(@type)">
                        <dim:field mdschema="organization" element="identifier" >
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:when>
                    <xsl:when test="@type = 'URL'">
                        <dim:field mdschema="oairecerif" element="identifier" qualifier="url" >
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:when>
                    <xsl:when test="@type = 'https://purl.org/pe-repo/concytec/terminos#ruc'">
                        <dim:field mdschema="organization" element="identifier" qualifier="ruc">
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:when>
                    <xsl:when test="@type = 'https://w3id.org/cerif/vocab/IdentifierTypes#ISNI'">
                        <dim:field mdschema="organization" element="identifier" qualifier="isni">
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:when>
                    <xsl:when test="@type = 'https://w3id.org/cerif/vocab/IdentifierTypes#ScopusAffiliationID'">
                        <dim:field mdschema="organization" element="identifier" qualifier="scopusaffid">
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:when>
                    <xsl:when test="@type = 'https://w3id.org/cerif/vocab/IdentifierTypes#FundRefID'">
                        <dim:field mdschema="organization" element="identifier" qualifier="crossrefid">
                            <xsl:value-of select="current()" />
                        </dim:field>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            
            <xsl:for-each select="cerif:Subject">
                <dim:field mdschema="dc" element="subject" qualifier="ocde">
                    <xsl:value-of select="current()"/>
                </dim:field>
            </xsl:for-each>
            
            <xsl:for-each select="cerif:Keyword">
                <dim:field mdschema="dc" element="subject">
                    <xsl:value-of select="current()"/>
                </dim:field>
            </xsl:for-each>
                 
            <dim:field mdschema="organization" element="address" qualifier="addressLocality">
                <xsl:value-of select="cerif:PostAddress/cerif:AddressLocality"/>
            </dim:field>
                 
            <dim:field mdschema="organization" element="address" qualifier="addressCountry">
                <xsl:value-of select="cerif:PostAddress/cerif:AddressCountry"/>
            </dim:field>
                    
            <dim:field mdschema="perucris" element="ubigeo">
                <xsl:value-of select="cerif:UbiGeo"/>
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