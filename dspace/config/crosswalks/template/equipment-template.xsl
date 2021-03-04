<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:cerif="https://purl.org/pe-repo/cerif-profile/1.0/"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
	
	<xsl:template match="cerif:Equipment">	
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="simpleA4"
					page-height="29.7cm" page-width="24cm" margin-top="2cm"
					margin-bottom="2cm" margin-left="1cm" margin-right="1cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="simpleA4">
				<fo:flow flow-name="xsl-region-body">
		         	<fo:block margin-bottom="5mm" padding="2mm">
						<fo:block font-size="26pt" font-weight="bold" text-align="center" >
							<xsl:value-of select="cerif:Name" />
						</fo:block>
					</fo:block>
					
			    	<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="cerif:Description" />
					</fo:block>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Basic informations'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Equipment Acronym'" />
				    	<xsl:with-param name="value" select="cerif:Acronym" />
			    	</xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Type'" />
                        <xsl:with-param name="value" select="cerif:Type" />
                    </xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Institution Unique Identifier'" />
				    	<xsl:with-param name="value" select="cerif:Identifier[@type = 'Institution assigned unique equipment identifier']" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Owner (Organization)'" />
				    	<xsl:with-param name="value" select="cerif:Owner/cerif:OrgUnit/cerif:Name" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Owner (Person)'" />
				    	<xsl:with-param name="value" select="cerif:Owner/cerif:Person/@displayName" />
			    	</xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Funding(s)'" />
                        <xsl:with-param name="values" select="cerif:Funder/cerif:As/cerif:Funding/cerif:Name" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Subject(s)'" />
                        <xsl:with-param name="values" select="cerif:Subject" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Research line'" />
                        <xsl:with-param name="value" select="cerif:ResearchLine" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Manufacturing country'" />
                        <xsl:with-param name="value" select="cerif:ManufacturingCountry" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Manufacturing date'" />
                        <xsl:with-param name="value" select="cerif:ManufacturingDate" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Acquisition date'" />
                        <xsl:with-param name="value" select="cerif:AcquisitionDate" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Amount'" />
                        <xsl:with-param name="value" select="cerif:AcquisitionAmount" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Amount currency'" />
                        <xsl:with-param name="value" select="cerif:AcquisitionAmount/@currency" />
                    </xsl:call-template>
			    	
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
	
	<xsl:template name = "print-value" >
	  	<xsl:param name = "label" />
	  	<xsl:param name = "value" />
	  	<xsl:if test="$value">
		  	<fo:block font-size="10pt" margin-top="2mm">
				<fo:inline font-weight="bold" text-align="right" >
					<xsl:value-of select="$label" /> 
				</fo:inline >
				<xsl:text>: </xsl:text>
				<fo:inline>
					<xsl:value-of select="$value" /> 
				</fo:inline >
			</fo:block>
	  	</xsl:if>
	</xsl:template>
	
	<xsl:template name = "print-values" >
        <xsl:param name = "label" />
        <xsl:param name = "values" />
        <xsl:if test="$values">
            <fo:block font-size="10pt" margin-top="2mm">
                <fo:inline font-weight="bold" text-align="right"  >
                    <xsl:value-of select="$label" /> 
                </fo:inline >
                <xsl:text>: </xsl:text>
                <fo:inline>
                    <xsl:for-each select="$values">
                        <xsl:value-of select="current()" />
                        <xsl:if test="position() != last()">, </xsl:if>
                    </xsl:for-each>
                </fo:inline >
            </fo:block>
        </xsl:if>
    </xsl:template>
	
	<xsl:template name = "section-title" >
		<xsl:param name = "label" />
		<fo:block font-size="16pt" font-weight="bold" margin-top="8mm" >
			<xsl:value-of select="$label" /> 
		</fo:block>
		<fo:block margin-bottom="2mm" margin-top="-4mm">
			<fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />         
		</fo:block>
	</xsl:template>
	
</xsl:stylesheet>