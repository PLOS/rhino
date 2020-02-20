package org.ambraproject.rhino.config;

import java.net.URI;

public class RuntimeConfigurationImpl implements RuntimeConfiguration {
  private boolean prettyPrintJson = false;
  private URI contentRepoUrl;
  private String editorialBucket;
  private String corpusBucket;
  private URI taxonomyUrl;
  private String thesaurus;
  
  @Override
  public boolean getPrettyPrintJson() {
    return this.prettyPrintJson;
  }

  public void setPrettyPrintJson(boolean prettyPrintJson) {
    this.prettyPrintJson = prettyPrintJson;
  }

  @Override
  public URI getContentRepoUrl() {
    return this.contentRepoUrl;
  }

  public void setContentRepoUrl(URI contentRepoUrl) {
    this.contentRepoUrl = contentRepoUrl;
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
  public URI getTaxonomyUrl() {
    return this.taxonomyUrl;
  }

  public void setTaxonomyUrl(URI taxonomyUrl) {
    this.taxonomyUrl = taxonomyUrl;
  }

  @Override
  public String getThesaurus() {
    return this.thesaurus;
  }

  public void setThesaurus(String thesaurus) {
    this.thesaurus = thesaurus;
  }
}
