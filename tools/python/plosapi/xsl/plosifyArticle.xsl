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

  <!--PMC XML seems to have a slightly different structure for copy rights-->
  <xsl:template match="article-meta/permissions">
    <copyright-statement>
      <xsl:value-of select="copyright-statement"/>
    </copyright-statement>
  </xsl:template>

  <!--PMC XML doesn't seem to have a name-style attribute to their author nodes-->
  <xsl:template match="contrib[@contrib-type='author']/name">
    <name name-style="western">
      <xsl:copy-of select="node()|@*"/>
    </name>
  </xsl:template>

  <!--PMC XML structures supp infos and graphics a bit differently 
    Trim the id of the first five characters: ex "pone-" or "pone." or "pbio.".
      * Including these characters causes our file repo to create an extra folder
  -->
<!--   <xsl:template match="graphic">
    <graphic>
      <xsl:attribute name="xlink:href"><xsl:value-of select="substring(@xlink:href,6,string-length(@xlink:href))" /></xsl:attribute>
    </graphic>
  </xsl:template>
 -->
  <!--PMC XML structures supp infos and graphics a bit differently 
    Trim the id of the first five characters: ex "pone-" or "pone." or "pbio.".
      * Including these characters causes our file repo to create an extra folder
-->
<!--   <xsl:template match="supplementary-material[media]">
    <supplementary-material>
      <xsl:attribute name="id"><xsl:value-of select="substring(@id,6,string-length(@id))" /></xsl:attribute>
      <xsl:attribute name="content-type"><xsl:value-of select="@content-type" /></xsl:attribute>
      <xsl:attribute name="xlink:href">info:doi/10.1371/annotation/<xsl:value-of select="substring(@id,6,string-length(@id))" /></xsl:attribute>
      <media>
        <xsl:attribute name="xlink:href"><xsl:value-of select="substring(media/@xlink:href,6,string-length(media/@xlink:href))" /></xsl:attribute>
        <xsl:attribute name="mimetype"><xsl:value-of select="media/@mimetype" /></xsl:attribute>
        <xsl:attribute name="mime-subtype"><xsl:value-of select="media/@mime-subtype" /></xsl:attribute>
        <xsl:copy-of select="media/caption"/>
      </media>
    </supplementary-material>
  </xsl:template> -->
  <!--
     <graphic xlink:href="pone.024aa8f2-a9cb-46a7-93ce-5a193189dea9.g001"/>
     <graphic xlink:href="pgen.ff93eba8-9567-4f41-b90d-9cdfdf65f747.g001"/>
     <media xlink:href="ppat.088ea07b-d578-4586-9707-160143d4f1be.s001.pdf" mimetype="application" mime-subtype="pdf">

    <fig id="pone-26c3d7a9-80dc-4719-bf61-968e5f322983-g001" orientation="portrait" position="float">
      <graphic xlink:href="pone.26c3d7a9-80dc-4719-bf61-968e5f322983.g001"/>
    </fig>
    <fig id="pone-0031918-g008" position="float">
    <object-id pub-id-type="doi">10.1371/journal.pone.0031918.g008</object-id>


    <supplementary-material content-type="local-data" id="pbio-042f2803-a625-4b2a-bd08-ce50799e4cf6-s001">
      <media xlink:href="pbio.042f2803-a625-4b2a-bd08-ce50799e4cf6.s001.doc" mimetype="application" mime-subtype="msword">
        <caption>
          <p>Click here for additional data file.</p>
        </caption>
      </media>
    </supplementary-material>    
  -->


</xsl:stylesheet>

