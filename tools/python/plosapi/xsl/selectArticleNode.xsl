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
<xsl:stylesheet version="1.0" xmlns="http://dtd.nlm.nih.gov/2.0/xsd/archivearticle" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
  xmlns:oai="http://www.openarchives.org/OAI/2.0/" xmlns:art="http://dtd.nlm.nih.gov/2.0/xsd/archivearticle">
  <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="no" media-type="text/xml" version="1.0"/>

  <!-- Select the article node, and retun that tree -->
  <xsl:template match="/">
    <xsl:copy-of select="oai:OAI-PMH/oai:GetRecord/oai:record/oai:metadata/art:article"/>
  </xsl:template>

</xsl:stylesheet>


