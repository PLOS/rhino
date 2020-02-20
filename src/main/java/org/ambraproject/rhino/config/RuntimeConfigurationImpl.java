package org.ambraproject.rhino.config;

import java.net.URI;

public class RuntimeConfigurationImpl implements RuntimeConfiguration {
  private boolean prettyPrintJson = false;
  private URI contentRepoServer;
  private String editorialBucket;
  private String corpusBucket;
  private URI taxonomyServer;
  private String thesaurus;
  
  @Override
  public boolean getPrettyPrintJson() {
    return this.prettyPrintJson;
  }

  public void setPrettyPrintJson(boolean prettyPrintJson) {
    this.prettyPrintJson = prettyPrintJson;
  }

  @Override
  public URI getContentRepoServer() {
    return this.contentRepoServer;
  }

  public void setContentRepoServer(URI contentRepoServer) {
    this.contentRepoServer = contentRepoServer;
  }

  @Override
  public String getCorpusBucket() {
    return this.corpusBucket;
  }

  public void setCorpusBucket(String corpusBucket) {
    this.corpusBucket = corpusBucket;
  }

  @Override
  public String getEditorialBucket() {
    return this.editorialBucket;
  }

  public void setEditorialBucket(String editorialBucket) {
    this.editorialBucket = editorialBucket;
  }

  @Override
  public URI getTaxonomyServer() {
    return this.taxonomyServer;
  }

  public void setTaxonomyServer(URI taxonomyServer) {
    this.taxonomyServer = taxonomyServer;
  }

  @Override
  public String getThesaurus() {
    return this.thesaurus;
  }

  public void setThesaurus(String thesaurus) {
    this.thesaurus = thesaurus;
  }
}
