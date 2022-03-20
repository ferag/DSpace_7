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

					<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="Description" />
					</fo:block>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Información basica'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Acrónimo'" />
				    	<xsl:with-param name="value" select="Acronym" />
			    	</xsl:call-template>

					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Nombre legal (Razón social)'" />
						<xsl:with-param name="value" select="LegalName" />
					</xsl:call-template>

					<xsl:call-template name="print-values">
						<xsl:with-param name="label" select="'Nombre(s) de la organización alternativo(s)'" />
						<xsl:with-param name="values" select="AlternativeName" />
					</xsl:call-template>
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Tipo'" />
				    	<xsl:with-param name="value" select="Type" />
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
                        <xsl:with-param name="value" select="PostAddress/AddressCountry" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Dirección Postal/Localidad'" />
                        <xsl:with-param name="value" select="PostAddress/AddressLocality" />
                    </xsl:call-template>
                    
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Ubigeo'" />
                        <xsl:with-param name="value" select="UbiGeo" />
                    </xsl:call-template>

					<xsl:call-template name="print-values">
						<xsl:with-param name="label" select="'Correo electronico'" />
						<xsl:with-param name="values" select="Email" />
					</xsl:call-template>

					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Sector Institucional'" />
						<xsl:with-param name="value" select="InstitutionalSector" />
					</xsl:call-template>

					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Tipo de dependencia'" />
						<xsl:with-param name="value" select="DependencyType" />
					</xsl:call-template>

			    	<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Clasificación Sunedu'" />
						<xsl:with-param name="value" select="SuneduClassification" />
					</xsl:call-template>

					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Naturaleza'" />
						<xsl:with-param name="value" select="-JuridicalNature" />
					</xsl:call-template>

					<xsl:call-template name="print-values">
						<xsl:with-param name="label" select="'CIIU - Clasificación Industrial Uniforme'" />
						<xsl:with-param name="values" select="CiiuClassification" />
					</xsl:call-template>

					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Acciones de vigilancia tecnológica'" />
						<xsl:with-param name="value" select="TechnologicalSurveillanceActions" />
					</xsl:call-template>

					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Unidad formal de vigilancia tecnológica'" />
						<xsl:with-param name="value" select="FormalTechnologicalSurveillanceUnit" />
					</xsl:call-template>

					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'166006 o equivalente certificada'" />
						<xsl:with-param name="value" select="QualityCertified" />
					</xsl:call-template>

					<xsl:if test="ConcytecRecords/Renacyt/Registration">

						<xsl:call-template name="section-title">
							<xsl:with-param name="label" select="'Registros Concytec'" />
						</xsl:call-template>
						<xsl:for-each select="ConcytecRecords/Renacyt">
							<fo:block font-size="10pt">
								<xsl:if test="Registration">
									<fo:inline font-weight="bold" text-align="right"  >
										<xsl:text>Registro: </xsl:text>
									</fo:inline >
									<xsl:value-of select="Registration" />,  Resolución: <xsl:value-of select="RegistrationNumber" />
								</xsl:if>
								<xsl:if test="Classification">
									<xsl:call-template name="print-value">
										<xsl:with-param name="label" select="'Categoría'" />
										<xsl:with-param name="value" select="Classification" />
									</xsl:call-template>
								</xsl:if>
								<xsl:if test="Ocde">
									<xsl:call-template name="print-values">
										<xsl:with-param name="label" select="'Disciplina autorizada'" />
										<xsl:with-param name="values" select="Ocde" />
									</xsl:call-template>
								</xsl:if>
								<xsl:if test="Strength">
									<xsl:call-template name="print-values">
										<xsl:with-param name="label" select="'Fortalezas autorizada'" />
										<xsl:with-param name="values" select="Strength" />
									</xsl:call-template>
								</xsl:if>
								<xsl:if test="DateOfQualification">
									<xsl:call-template name="print-value">
										<xsl:with-param name="label" select="'Vigencia'" />
										<xsl:with-param name="value" select="DateOfQualification" />
									</xsl:call-template>
								</xsl:if>
								<xsl:if test="Validity">
									<xsl:call-template name="print-value">
										<xsl:with-param name="label" select="'Fin de la Vigencia'" />
										<xsl:with-param name="value" select="Validity" />
									</xsl:call-template>
								</xsl:if>
								<xsl:if test="Contact">
									<xsl:call-template name="print-value">
										<xsl:with-param name="label" select="'Datos de contacto'" />
										<xsl:with-param name="value" select="Contact" />
									</xsl:call-template>
								</xsl:if>
							</fo:block>
						</xsl:for-each>

					</xsl:if>


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
