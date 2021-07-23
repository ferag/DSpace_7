<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:doc="http://www.lyncode.com/xoai"
        version="1.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:param name="node"/>
    <xsl:param name="sourceDatabase"/>
    <xsl:param name="date"/>
    <xsl:param name="source"/>
    <xsl:param name="apiVersion"/>
    <xsl:param name="page"/>
    <xsl:param name="size"/>
    <xsl:param name="resultsInPage"/>
    <xsl:param name="totalResults"/>
    <xsl:param name="searchQuery"/>

    <xsl:template match="Cerif">
        <Search-API>
            <Header>
                <source><xsl:value-of select="$source"/></source>
                <api-version><xsl:value-of select="$apiVersion"/></api-version>
                <page><xsl:value-of select="$page"/></page>
                <size><xsl:value-of select="$size"/></size>
                <resultsInPage><xsl:value-of select="$resultsInPage"/></resultsInPage>
                <totalResults><xsl:value-of select="$totalResults"/></totalResults>
                <query><xsl:value-of select="$searchQuery" disable-output-escaping="yes"/></query>
            </Header>
            <Payload>
                <xsl:element name="Cerif"
                             namespace="org:eurocris:cerif-1.6-2">
                    <xsl:attribute name="xsi:schemaLocation"
                                   namespace="http://www.w3.org/2001/XMLSchema-instance">urn:xmlns:org:eurocris:cerif-1.6-2http://www.eurocris.org/Uploads/Web%20pages/CERIF-1.6/CERIF_1.6_2.xsd</xsl:attribute>
                    <xsl:attribute name="sourceDatabase">
                        <xsl:value-of select="$sourceDatabase"/>
                    </xsl:attribute>
                    <xsl:attribute name="date">
                        <xsl:value-of select="$date"/>
                    </xsl:attribute>

                    <xsl:for-each select="//doc:metadata/doc:element[@name=substring-before($node, '.')]/doc:element[@name=substring-after($node, '.')]/doc:element[@name='none']/doc:field[@name='value']">
                        <xsl:value-of select="." disable-output-escaping="yes"/>
                    </xsl:for-each>
                </xsl:element>
            </Payload>
        </Search-API>
    </xsl:template>
</xsl:stylesheet>
