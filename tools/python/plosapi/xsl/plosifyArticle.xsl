<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright (c) 2007-2013 by Public Library of Science

  http://plos.org
  http://ambraproject.org
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="no" media-type="text/xml" version="1.0"/>

  <xsl:template match="node()|@*">
    <xsl:copy>
       <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!--PMC XML seems to have a slightly different structure for copy rights (sometimes) -->
  <xsl:template match="article-meta/permissions/copyright-statement">
    <license><license-p><xsl:value-of select="."/></license-p></license>
  </xsl:template>

  <!--PMC XML doesn't seem to have a name-style attribute to their author nodes-->
  <xsl:template match="contrib[@contrib-type='author']/name">
    <name name-style="western">
      <xsl:copy-of select="node()|@*"/>
    </name>
  </xsl:template>

  <!-- fix up related article node -->
  <xsl:template match="@xlink:href[parent::related-article]">
    <xsl:attribute name="xlink:href">info:doi/<xsl:value-of select="../@xlink:href"/></xsl:attribute>
  </xsl:template>

  <!-- fix up PMC attribute value -->
  <xsl:template match="@pub-id-type">
    <xsl:attribute name="pub-id-type">
      <xsl:choose>
        <xsl:when test=". = 'pmc'">
          <xsl:text>pmcid</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="." />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
</xsl:stylesheet>

