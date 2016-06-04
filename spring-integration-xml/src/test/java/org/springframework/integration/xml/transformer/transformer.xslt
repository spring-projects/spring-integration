<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name="testParam"/>
	<xsl:param name="testParam2"/>
	<xsl:param name="testParam3"/>
	<xsl:output omit-xml-declaration="yes" />
	<xsl:template match="order">
		<bob>test</bob>
		<sampleElementA>
			<xsl:value-of select="$testParam" />
		</sampleElementA>
		<sampleElementB>
			<xsl:value-of select="$testParam2" />
		</sampleElementB>
		<sampleElementC>
			<xsl:value-of select="$testParam3" />
		</sampleElementC>
	</xsl:template>
</xsl:stylesheet>
