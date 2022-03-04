<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
	
	<xsl:template match="person">	
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
				
					<fo:table>
						<xsl:choose>
							<xsl:when test="personal-picture">
							   <fo:table-column column-width="33%"/>
							   <fo:table-column column-width="67%"/>
							</xsl:when>
							<xsl:otherwise>
								<fo:table-column />
							</xsl:otherwise>
						</xsl:choose>
					   <fo:table-body>
					      <fo:table-row>
					      	 <xsl:if test="personal-picture">
						         <fo:table-cell>
									<fo:block font-size="10pt" font-weight="bold" text-align="center" >	
										<xsl:variable name="picturePath" select="concat('file:',$imageDir,'/',personal-picture)" />
										<fo:external-graphic content-height="scale-to-fit" height="2.40in"  content-width="2.00in" scaling="non-uniform">
											<xsl:attribute name="src">
												<xsl:value-of select="$picturePath" />
											</xsl:attribute>
										</fo:external-graphic>
									</fo:block>
						         </fo:table-cell>
					         </xsl:if>
					         <fo:table-cell>
					         	<fo:block background-color="#8cc0db" margin-bottom="5mm" padding="2mm">
									<fo:block font-size="26pt" font-weight="bold" text-align="center" >
										<xsl:value-of select="names/preferred-name"  />
									</fo:block>
									<fo:block font-size="12pt" text-align="center">
										<xsl:value-of select="job-title"  />
										<xsl:if test="job-title and main-affiliation">
											<xsl:text> en </xsl:text>
										</xsl:if>
										<xsl:value-of select="main-affiliation"  />
									</fo:block>
								</fo:block>
								
								<xsl:call-template name="print-value">
							    	<xsl:with-param name="label" select="'Fecha de nacimiento'" />
							    	<xsl:with-param name="value" select="birth-date" />
							    </xsl:call-template>
								<xsl:call-template name="print-value">
							    	<xsl:with-param name="label" select="'Sexo'" />
							    	<xsl:with-param name="value" select="gender" />
							    </xsl:call-template>
								<xsl:call-template name="print-value">
							    	<xsl:with-param name="label" select="'País'" />
							    	<xsl:with-param name="value" select="country" />
							    </xsl:call-template>
								<xsl:call-template name="print-values">
							    	<xsl:with-param name="label" select="'Correo electrónico'" />
							    	<xsl:with-param name="values" select="email" />
							    </xsl:call-template>
                                <xsl:call-template name="print-value">
                                    <xsl:with-param name="label" select="'Teléfono'" />
                                    <xsl:with-param name="value" select="phone" />
                                </xsl:call-template>
                                <xsl:call-template name="print-value">
                                    <xsl:with-param name="label" select="'Teléfono móvil'" />
                                    <xsl:with-param name="value" select="mobile-phone" />
                                </xsl:call-template>
								 <xsl:call-template name="print-value">
                                    <xsl:with-param name="label" select="'Tipo de Perfil'" />
                                    <xsl:with-param name="value" select="profile-type" />
                                </xsl:call-template>
                                
								<xsl:call-template name="print-value">
							    	<xsl:with-param name="label" select="'ORCID'" />
							    	<xsl:with-param name="value" select="identifiers/orcid" />
							    </xsl:call-template>
                                <xsl:call-template name="print-value">
                                    <xsl:with-param name="label" select="'Dina'" />
                                    <xsl:with-param name="value" select="identifiers/dina" />
                                </xsl:call-template>
                                <xsl:call-template name="print-value">
                                    <xsl:with-param name="label" select="'Dni'" />
                                    <xsl:with-param name="value" select="identifiers/dni" />
                                </xsl:call-template>
                                <xsl:call-template name="print-value">
                                    <xsl:with-param name="label" select="'Pasaporte'" />
                                    <xsl:with-param name="value" select="identifiers/passport" />
                                </xsl:call-template>
                                <xsl:call-template name="print-value">
                                    <xsl:with-param name="label" select="'Tarjeta de inmigración'" />
                                    <xsl:with-param name="value" select="identifiers/immigration-card" />
                                </xsl:call-template>
                                <xsl:call-template name="print-values">
							    	<xsl:with-param name="label" select="'ID Scopus del autor'" />
							    	<xsl:with-param name="values" select="identifiers/scopus-author-ids/scopus-author-id" />
						    	</xsl:call-template>
						    	
					         </fo:table-cell>
					      </fo:table-row>
					   </fo:table-body>
					</fo:table>
			    	
					<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="biography" />
					</fo:block>
					
                    <xsl:if test="affiliations/affiliation">
			    	
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Afiliaciones'" />
				    	</xsl:call-template>        
						<xsl:for-each select="affiliations/affiliation">
							<fo:block font-size="10pt">
								<xsl:value-of select="role" /> en <xsl:value-of select="name" />
								desde <xsl:value-of select="start-date" />
								<xsl:if test="end-date/text()">
								 hasta <xsl:value-of select="end-date" />
								</xsl:if>
							</fo:block>
						</xsl:for-each>
					
					</xsl:if>

                    <xsl:if test="educations/education">
					
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Educación'" />
				    	</xsl:call-template>    
						<xsl:for-each select="educations/education">
							<fo:block font-size="10pt">
								<xsl:value-of select="role" /> en <xsl:value-of select="name" />
								desde <xsl:value-of select="start-date" />
								<xsl:if test="end-date/text()">
								 hasta <xsl:value-of select="end-date" />
								</xsl:if>
								<xsl:if test="grantor/text()">
								 Centro de estudios: <xsl:value-of select="grantor" />
								</xsl:if>
								<xsl:if test="country/text()">
									- <xsl:value-of select="country" />
								</xsl:if>

							</fo:block>
						</xsl:for-each>
					
					</xsl:if>
					
                    <xsl:if test="qualifications/qualification">
					
						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Cualificaciones'" />
				    	</xsl:call-template>
						<xsl:for-each select="qualifications/qualification">
							<fo:block font-size="10pt">
								<xsl:value-of select="name" /> desde <xsl:value-of select="start-date" />
								<xsl:if test="end-date/text()">
								 hasta <xsl:value-of select="end-date" />
								</xsl:if>
								<xsl:if test="renacyt/text()">
								 Código: <xsl:value-of select="renacyt" />
								</xsl:if>
								<xsl:if test="renacyt/text()">
								 Nivel: <xsl:value-of select="renacyt" />
								</xsl:if>
							</fo:block>
						</xsl:for-each>
						
					</xsl:if>
					
                    <xsl:if test="publications/publication">

						<xsl:call-template name="section-title">
					    	<xsl:with-param name="label" select="'Publicaciones'" />
				    	</xsl:call-template>
				    	<xsl:for-each select="publications/publication">
							<fo:block font-size="10pt">
								<xsl:for-each select="authors/author">
									<xsl:value-of select="current()" />
								    <xsl:if test="position() != last()"> y </xsl:if>
								</xsl:for-each>
								<xsl:text> </xsl:text>
								<xsl:if test="date-issued">
									<xsl:text>(</xsl:text>
									<xsl:value-of select="date-issued" />
									<xsl:text>)</xsl:text>
								</xsl:if>
								<xsl:text>. </xsl:text>
								<fo:inline font-style="italic" >
									<xsl:value-of select="title" /> 
								</fo:inline >
							</fo:block>
						</xsl:for-each>
					
                    </xsl:if>
                    
                    <xsl:if test="( count(address/street) &gt; 0 ) or 
                    ( count(address/postal-code) &gt; 0 ) or 
                    ( count(address/country) &gt; 0 ) or 
                    ( count(working-groups/working-group) &gt; 0 ) or 
                    ( count(interests/interest) &gt; 0 ) or 
                    ( count(ocde-subjects/ocde-subject) &gt; 0 ) or
                    ( count(minam-subjects/minam-subject) &gt; 0 ) or
                    ( count(ins-subjects/ins-subject) &gt; 0 ) or
                    ( count(knows-languages/language) &gt; 0 )">
                    
	                    <xsl:call-template name="section-title">
	                        <xsl:with-param name="label" select="'Otra información'" />
	                    </xsl:call-template>
	                    
	                    <xsl:call-template name="print-value">
	                        <xsl:with-param name="label" select="'Calle de residencia'" />
	                        <xsl:with-param name="value" select="address/street" />
	                    </xsl:call-template>
	                    <xsl:call-template name="print-value">
	                        <xsl:with-param name="label" select="'Código postal'" />
	                        <xsl:with-param name="value" select="address/postal-code" />
	                    </xsl:call-template>
	                    <xsl:call-template name="print-value">
	                        <xsl:with-param name="label" select="'País de residencia'" />
	                        <xsl:with-param name="value" select="address/country" />
	                    </xsl:call-template>
	                    <xsl:call-template name="print-values">
	                        <xsl:with-param name="label" select="'Intereses'" />
	                        <xsl:with-param name="values" select="interests/interest" />
	                    </xsl:call-template>
						<xsl:call-template name="print-values">
							<xsl:with-param name="label" select="'Campo de conocimiento OCDE'" />
							<xsl:with-param name="values" select="ocde-subjects/ocde-subject" />
						</xsl:call-template>
						<xsl:call-template name="print-values">
							<xsl:with-param name="label" select="'Áreas Temáticas Ambientales asociadas a las líneas de investigación del MINAM'" />
							<xsl:with-param name="values" select="minam-subjects/minam-subject" />
						</xsl:call-template>
	                    <xsl:call-template name="print-values">
							<xsl:with-param name="label" select="'Áreas Temáticas Médicas y de la Salud asociadas a las líneas de investigación del INS'" />
							<xsl:with-param name="values" select="ins-subjects/ins-subject" />
						</xsl:call-template>
	                    <xsl:call-template name="print-values">
	                        <xsl:with-param name="label" select="'Idiomas'" />
	                        <xsl:with-param name="values" select="knows-languages/language" />
	                    </xsl:call-template>
                    
                    </xsl:if>
			    	
	  				<xsl:if test="personal-sites/personal-site">
				    	<fo:block font-size="10pt" margin-top="2mm">
				    		<fo:inline font-weight="bold" text-align="right" >
								Web personal:
							</fo:inline>
							<fo:inline>
								<xsl:for-each select="personal-sites/personal-site">
									<xsl:value-of select="site-url" />
									<xsl:text> </xsl:text>
									<xsl:if test="site-title">
										( <xsl:value-of select="site-title" /> )
									</xsl:if>
									<xsl:if test="position() != last()">, </xsl:if>
								</xsl:for-each>
							</fo:inline >
						</fo:block>
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
		<fo:block>
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
