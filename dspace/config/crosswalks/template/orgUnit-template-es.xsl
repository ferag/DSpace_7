<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
	
	<xsl:template match="OrgUnit">	
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
							<xsl:value-of select="Name" />
						</fo:block>
					</fo:block>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Información baáica'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Acrónimo'" />
				    	<xsl:with-param name="value" select="Acronym" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Tipo'" />
				    	<xsl:with-param name="value" select="Type" />
			    	</xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Descripción'" />
                        <xsl:with-param name="value" select="Description" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Director'" />
                        <xsl:with-param name="value" select="Director" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Establecido'" />
                        <xsl:with-param name="value" select="Established" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Junta científica:'" />
                        <xsl:with-param name="values" select="ScientificBoard" />
                    </xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Organización padre'" />
				    	<xsl:with-param name="value" select="PartOf/OrgUnit/Name" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Identificador(es)'" />
				    	<xsl:with-param name="values" select="Identifier[not(@type)]" />
			    	</xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'URL(s)'" />
                        <xsl:with-param name="values" select="Url" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'RUC ID(s)'" />
                        <xsl:with-param name="values" select="Identifier[@type = 'RUC']" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Ringgold ID(s)'" />
                        <xsl:with-param name="values" select="Identifier[@type = 'Ringgold']" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Research Organization Registry ID(s)'" />
                        <xsl:with-param name="values" select="Identifier[@type = 'Research Organization Registry']" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'International Standard Name ID(s)'" />
                        <xsl:with-param name="values" select="Identifier[@type = 'International Standard Name']" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'ID Scopus de la afiliación'" />
                        <xsl:with-param name="values" select="Identifier[@type = 'Scopus Affiliation']" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'ID CrossRef del Fundandor'" />
                        <xsl:with-param name="values" select="Identifier[@type = 'CrossRef Funder']" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Organización padre'" />
                        <xsl:with-param name="values" select="ParentOrganization" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Materia(s)'" />
                        <xsl:with-param name="values" select="Subject" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Palabra(s) clave'" />
                        <xsl:with-param name="values" select="Keyword" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Direccion Postal/País'" />
                        <xsl:with-param name="value" select="Country" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Dirección Postal/Localidad'" />
                        <xsl:with-param name="value" select="Locality" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Ubigeo'" />
                        <xsl:with-param name="value" select="Ubigeo" />
                    </xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Personas'" />
				    	<xsl:with-param name="values" select="People/Person/@displayName" />
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
	
	<xsl:template name = "section-title" >
		<xsl:param name = "label" />
		<fo:block font-size="16pt" font-weight="bold" margin-top="8mm" >
			<xsl:value-of select="$label" /> 
		</fo:block>
		<fo:block margin-bottom="2mm" margin-top="-4mm">
			<fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />         
		</fo:block>
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
	
</xsl:stylesheet>
