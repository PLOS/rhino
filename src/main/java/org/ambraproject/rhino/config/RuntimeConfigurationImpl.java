package org.ambraproject.rhino.config;

import java.net.URI;
import com.google.common.base.Preconditions;

public class RuntimeConfigurationImpl implements RuntimeConfiguration {
  private boolean prettyPrintJson = false;
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
  public String getCorpusBucket() {
    return this.corpusBucket;
  }

  public void setCorpusBucket(String corpusBucket) {
    Preconditions.checkNotNull(corpusBucket, "CORPUS_BUCKET is required");
    Preconditions.checkState(!corpusBucket.equals(""), "CORPUS_BUCKET is required");
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
    Preconditions.checkNotNull(taxonomyUrl, "TAXONOMY_URL is required and must be a valid URL");
    Preconditions.checkState(taxonomyUrl.isAbsolute(), "TAXONOMY_URL is required and must be a valid URL");
    this.taxonomyUrl = taxonomyUrl;
  }

  @Override
  public String getThesaurus() {
    return this.thesaurus;
  }

  public void setThesaurus(String thesaurus) {
    Preconditions.checkNotNull(thesaurus, "THESAURUS is required");
    Preconditions.checkState(!thesaurus.equals(""), "THESAURUS is required");
    this.thesaurus = thesaurus;
  }
}
