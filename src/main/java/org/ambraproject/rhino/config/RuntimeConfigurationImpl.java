package org.ambraproject.rhino.config;

import static com.google.common.base.Strings.isNullOrEmpty;
import java.net.URI;
import com.google.common.base.Preconditions;

public class RuntimeConfigurationImpl implements RuntimeConfiguration {
  private boolean prettyPrintJson = false;
  private String editorialBucket;
  private String corpusBucket;
  private URI taxonomyUrl;
  private String thesaurus;
  private String awsRoleArn;
  private String projectId;

  public RuntimeConfigurationImpl() {
    Preconditions.checkArgument(!isNullOrEmpty(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")),
        "Please set GOOGLE_APPLICATION_CREDENTIALS.");
  }

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
    Preconditions.checkState(!isNullOrEmpty(corpusBucket), "CORPUS_BUCKET is required");
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
    Preconditions.checkState(!isNullOrEmpty(thesaurus), "THESAURUS is required");
    this.thesaurus = thesaurus;
  }

  @Override
  public String getAwsRoleArn() {
    return awsRoleArn;
  }

  public void setAwsRoleArn(String awsRoleArn) {
    Preconditions.checkState(!isNullOrEmpty(awsRoleArn), "AWS_ROLE_ARN is required");
    this.awsRoleArn = awsRoleArn;
  }

  @Override
  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    Preconditions.checkState(!isNullOrEmpty(projectId), "PROJECT_ID is required");
    this.projectId = projectId;
  }
}
