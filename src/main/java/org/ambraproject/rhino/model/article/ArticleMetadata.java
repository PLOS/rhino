package org.ambraproject.rhino.model.article;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ArticleMetadata {

  private final String doi;

  private final String title;
  private final String eIssn;
  private final String description;
  private final String rights;
  private final String language;
  private final String format;
  private final Integer pageCount;
  private final String eLocationId;

  private final LocalDate publicationDate;
  private final LocalDate revisionDate;

  private final String volume;
  private final String issue;

  private final String publisherLocation;
  private final String publisherName;
  private final String url;

  private final String nlmArticleType;
  private final String articleType;

  private final ImmutableList<String> collaborativeAuthors;

  private final ImmutableList<AssetMetadata> assets;
  private final ImmutableList<RelatedArticleLink> relatedArticles;
  private final ImmutableList<NlmPerson> authors;
  private final ImmutableList<NlmPerson> editors;

  private final ImmutableMap<String, ImmutableList<String>> customMeta;

  private ArticleMetadata(Builder builder) {
    this.doi = Objects.requireNonNull(builder.doi);
    this.title = builder.title;
    this.eIssn = builder.eIssn;
    this.description = builder.description;
    this.rights = builder.rights;
    this.language = builder.language;
    this.format = builder.format;
    this.pageCount = builder.pageCount;
    this.eLocationId = builder.eLocationId;
    this.publicationDate = builder.publicationDate;
    this.revisionDate = builder.revisionDate;
    this.volume = builder.volume;
    this.issue = builder.issue;
    this.publisherLocation = builder.publisherLocation;
    this.publisherName = builder.publisherName;
    this.url = builder.url;
    this.nlmArticleType = builder.nlmArticleType;
    this.articleType = builder.articleType;
    this.collaborativeAuthors = ImmutableList.copyOf(builder.collaborativeAuthors);
    this.assets = ImmutableList.copyOf(builder.assets);
    this.relatedArticles = ImmutableList.copyOf(builder.relatedArticles);
    this.authors = ImmutableList.copyOf(builder.authors);
    this.editors = ImmutableList.copyOf(builder.editors);
    this.customMeta = ImmutableMap.copyOf(Maps.transformValues(builder.customMeta.asMap(), ImmutableList::copyOf));
  }

  public String getDoi() {
    return doi;
  }

  public String getTitle() {
    return title;
  }

  public String geteIssn() {
    return eIssn;
  }

  public String getDescription() {
    return description;
  }

  public String getRights() {
    return rights;
  }

  public String getLanguage() {
    return language;
  }

  public String getFormat() {
    return format;
  }

  public Integer getPageCount() {
    return pageCount;
  }

  public String geteLocationId() {
    return eLocationId;
  }

  public LocalDate getPublicationDate() {
    return publicationDate;
  }

  public LocalDate getRevisionDate() {
    return revisionDate;
  }

  public String getVolume() {
    return volume;
  }

  public String getIssue() {
    return issue;
  }

  public String getPublisherLocation() {
    return publisherLocation;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public String getUrl() {
    return url;
  }

  public String getNlmArticleType() {
    return nlmArticleType;
  }

  public String getArticleType() {
    return articleType;
  }

  public ImmutableList<String> getCollaborativeAuthors() {
    return collaborativeAuthors;
  }

  public ImmutableList<AssetMetadata> getAssets() {
    return assets;
  }

  public ImmutableList<RelatedArticleLink> getRelatedArticles() {
    return relatedArticles;
  }

  public ImmutableList<NlmPerson> getAuthors() {
    return authors;
  }

  public ImmutableList<NlmPerson> getEditors() {
    return editors;
  }


  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {
    }

    private String doi;

    private String title;
    private String eIssn;
    private String description;
    private String rights;
    private String language;
    private String format;
    private Integer pageCount;
    private String eLocationId;

    private LocalDate publicationDate;
    private LocalDate revisionDate;

    private String volume;
    private String issue;

    private String publisherLocation;
    private String publisherName;
    private String url;

    private String nlmArticleType;
    private String articleType;

    private List<String> collaborativeAuthors;

    private List<AssetMetadata> assets;
    private List<RelatedArticleLink> relatedArticles;
    private List<NlmPerson> authors;
    private List<NlmPerson> editors;

    private ListMultimap<String, String> customMeta;

    public ArticleMetadata build() {
      return new ArticleMetadata(this);
    }

    public Builder setDoi(String doi) {
      this.doi = doi;
      return this;
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder seteIssn(String eIssn) {
      this.eIssn = eIssn;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setRights(String rights) {
      this.rights = rights;
      return this;
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Builder setFormat(String format) {
      this.format = format;
      return this;
    }

    public Builder setPageCount(Integer pageCount) {
      this.pageCount = pageCount;
      return this;
    }

    public Builder seteLocationId(String eLocationId) {
      this.eLocationId = eLocationId;
      return this;
    }

    public Builder setPublicationDate(LocalDate publicationDate) {
      this.publicationDate = publicationDate;
      return this;
    }

    public Builder setRevisionDate(LocalDate revisionDate) {
      this.revisionDate = revisionDate;
      return this;
    }

    public Builder setVolume(String volume) {
      this.volume = volume;
      return this;
    }

    public Builder setIssue(String issue) {
      this.issue = issue;
      return this;
    }

    public Builder setPublisherLocation(String publisherLocation) {
      this.publisherLocation = publisherLocation;
      return this;
    }

    public Builder setPublisherName(String publisherName) {
      this.publisherName = publisherName;
      return this;
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setNlmArticleType(String nlmArticleType) {
      this.nlmArticleType = nlmArticleType;
      return this;
    }

    public Builder setArticleType(String articleType) {
      this.articleType = articleType;
      return this;
    }

    public Builder setCollaborativeAuthors(List<String> collaborativeAuthors) {
      this.collaborativeAuthors = collaborativeAuthors;
      return this;
    }

    public Builder setAssets(List<AssetMetadata> assets) {
      this.assets = assets;
      return this;
    }

    public Builder setRelatedArticles(List<RelatedArticleLink> relatedArticles) {
      this.relatedArticles = relatedArticles;
      return this;
    }

    public Builder setAuthors(List<NlmPerson> authors) {
      this.authors = authors;
      return this;
    }

    public Builder setEditors(List<NlmPerson> editors) {
      this.editors = editors;
      return this;
    }

    public Builder setCustomMeta(ListMultimap<String, String> customMeta) {
      this.customMeta = customMeta;
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleMetadata that = (ArticleMetadata) o;

    if (doi != null ? !doi.equals(that.doi) : that.doi != null) return false;
    if (title != null ? !title.equals(that.title) : that.title != null) return false;
    if (eIssn != null ? !eIssn.equals(that.eIssn) : that.eIssn != null) return false;
    if (description != null ? !description.equals(that.description) : that.description != null) return false;
    if (rights != null ? !rights.equals(that.rights) : that.rights != null) return false;
    if (language != null ? !language.equals(that.language) : that.language != null) return false;
    if (format != null ? !format.equals(that.format) : that.format != null) return false;
    if (pageCount != null ? !pageCount.equals(that.pageCount) : that.pageCount != null) return false;
    if (eLocationId != null ? !eLocationId.equals(that.eLocationId) : that.eLocationId != null) return false;
    if (publicationDate != null ? !publicationDate.equals(that.publicationDate) : that.publicationDate != null) {
      return false;
    }
    if (revisionDate != null ? !revisionDate.equals(that.revisionDate) : that.revisionDate != null) return false;
    if (volume != null ? !volume.equals(that.volume) : that.volume != null) return false;
    if (issue != null ? !issue.equals(that.issue) : that.issue != null) return false;
    if (publisherLocation != null ? !publisherLocation.equals(that.publisherLocation) : that.publisherLocation != null) {
      return false;
    }
    if (publisherName != null ? !publisherName.equals(that.publisherName) : that.publisherName != null) return false;
    if (url != null ? !url.equals(that.url) : that.url != null) return false;
    if (nlmArticleType != null ? !nlmArticleType.equals(that.nlmArticleType) : that.nlmArticleType != null) {
      return false;
    }
    if (articleType != null ? !articleType.equals(that.articleType) : that.articleType != null) return false;
    if (collaborativeAuthors != null ? !collaborativeAuthors.equals(that.collaborativeAuthors) : that.collaborativeAuthors != null) {
      return false;
    }
    if (assets != null ? !assets.equals(that.assets) : that.assets != null) return false;
    if (relatedArticles != null ? !relatedArticles.equals(that.relatedArticles) : that.relatedArticles != null) {
      return false;
    }
    if (authors != null ? !authors.equals(that.authors) : that.authors != null) return false;
    if (editors != null ? !editors.equals(that.editors) : that.editors != null) return false;
    return customMeta != null ? customMeta.equals(that.customMeta) : that.customMeta == null;
  }

  @Override
  public int hashCode() {
    int result = doi != null ? doi.hashCode() : 0;
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (eIssn != null ? eIssn.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (rights != null ? rights.hashCode() : 0);
    result = 31 * result + (language != null ? language.hashCode() : 0);
    result = 31 * result + (format != null ? format.hashCode() : 0);
    result = 31 * result + (pageCount != null ? pageCount.hashCode() : 0);
    result = 31 * result + (eLocationId != null ? eLocationId.hashCode() : 0);
    result = 31 * result + (publicationDate != null ? publicationDate.hashCode() : 0);
    result = 31 * result + (revisionDate != null ? revisionDate.hashCode() : 0);
    result = 31 * result + (volume != null ? volume.hashCode() : 0);
    result = 31 * result + (issue != null ? issue.hashCode() : 0);
    result = 31 * result + (publisherLocation != null ? publisherLocation.hashCode() : 0);
    result = 31 * result + (publisherName != null ? publisherName.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (nlmArticleType != null ? nlmArticleType.hashCode() : 0);
    result = 31 * result + (articleType != null ? articleType.hashCode() : 0);
    result = 31 * result + (collaborativeAuthors != null ? collaborativeAuthors.hashCode() : 0);
    result = 31 * result + (assets != null ? assets.hashCode() : 0);
    result = 31 * result + (relatedArticles != null ? relatedArticles.hashCode() : 0);
    result = 31 * result + (authors != null ? authors.hashCode() : 0);
    result = 31 * result + (editors != null ? editors.hashCode() : 0);
    result = 31 * result + (customMeta != null ? customMeta.hashCode() : 0);
    return result;
  }
}
