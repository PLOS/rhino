package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.ArticleEditor;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Category;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.test.AssertionFailure;
import org.ambraproject.rhino.test.ExpectedEntity;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Generated code! See {@code /src/test/python/ingest_test_generation/generate.py}
 */
public class ExpectedArticle extends ExpectedEntity<Article> {
  private final String doi;
  private final String title;
  private final String eIssn;
  private final int state;
  private final String archiveName;
  private final String description;
  private final String rights;
  private final String language;
  private final String format;
  private final String pages;
  private final String eLocationId;
  private final String strkImgURI;
  private final Date date;
  private final String volume;
  private final String issue;
  private final String journal;
  private final String publisherLocation;
  private final String publisherName;
  private final String url;
  private final List<String> collaborativeAuthors;
  private final Set<String> types;
  private final Set<Category> categories;
  private final List<ArticleAsset> assets;
  private final List<CitedArticle> citedArticles;
  private final List<ArticleRelationship> relatedArticles;
  private final List<ArticleAuthor> authors;
  private final List<ArticleEditor> editors;
  private final Set<Journal> journals;

  private ExpectedArticle(Builder builder) {
    super(Article.class);
    this.doi = builder.doi;
    this.title = builder.title;
    this.eIssn = builder.eIssn;
    this.state = builder.state;
    this.archiveName = builder.archiveName;
    this.description = builder.description;
    this.rights = builder.rights;
    this.language = builder.language;
    this.format = builder.format;
    this.pages = builder.pages;
    this.eLocationId = builder.eLocationId;
    this.strkImgURI = builder.strkImgURI;
    this.date = builder.date;
    this.volume = builder.volume;
    this.issue = builder.issue;
    this.journal = builder.journal;
    this.publisherLocation = builder.publisherLocation;
    this.publisherName = builder.publisherName;
    this.url = builder.url;
    this.collaborativeAuthors = builder.collaborativeAuthors;
    this.types = builder.types;
    this.categories = builder.categories;
    this.assets = builder.assets;
    this.citedArticles = builder.citedArticles;
    this.relatedArticles = builder.relatedArticles;
    this.authors = builder.authors;
    this.editors = builder.editors;
    this.journals = builder.journals;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Collection<AssertionFailure<?>> test(Article article) {
    Collection<AssertionFailure<?>> failures = Lists.newArrayList();
    testField(failures, "doi", article.getDoi(), doi);
    testField(failures, "title", article.getTitle(), title);
    testField(failures, "eIssn", article.geteIssn(), eIssn);
    testField(failures, "state", article.getState(), state);
    testField(failures, "archiveName", article.getArchiveName(), archiveName);
    testField(failures, "description", article.getDescription(), description);
    testField(failures, "rights", article.getRights(), rights);
    testField(failures, "language", article.getLanguage(), language);
    testField(failures, "format", article.getFormat(), format);
    testField(failures, "pages", article.getPages(), pages);
    testField(failures, "eLocationId", article.geteLocationId(), eLocationId);
    testField(failures, "strkImgURI", article.getStrkImgURI(), strkImgURI);
    testField(failures, "date", article.getDate(), date);
    testField(failures, "volume", article.getVolume(), volume);
    testField(failures, "issue", article.getIssue(), issue);
    testField(failures, "journal", article.getJournal(), journal);
    testField(failures, "publisherLocation", article.getPublisherLocation(), publisherLocation);
    testField(failures, "publisherName", article.getPublisherName(), publisherName);
    testField(failures, "url", article.getUrl(), url);
    testField(failures, "collaborativeAuthors", article.getCollaborativeAuthors(), collaborativeAuthors);
    testField(failures, "types", article.getTypes(), types);
    testField(failures, "categories", article.getCategories(), categories);
    testField(failures, "assets", article.getAssets(), assets);
    testField(failures, "citedArticles", article.getCitedArticles(), citedArticles);
    testField(failures, "relatedArticles", article.getRelatedArticles(), relatedArticles);
    testField(failures, "authors", article.getAuthors(), authors);
    testField(failures, "editors", article.getEditors(), editors);
    testField(failures, "journals", article.getJournals(), journals);
    return ImmutableList.copyOf(failures);
  }

  public static class Builder {
    private String doi;
    private String title;
    private String eIssn;
    private int state;
    private String archiveName;
    private String description;
    private String rights;
    private String language;
    private String format;
    private String pages;
    private String eLocationId;
    private String strkImgURI;
    private Date date;
    private String volume;
    private String issue;
    private String journal;
    private String publisherLocation;
    private String publisherName;
    private String url;
    private List<String> collaborativeAuthors;
    private Set<String> types;
    private Set<Category> categories;
    private List<ArticleAsset> assets;
    private List<CitedArticle> citedArticles;
    private List<ArticleRelationship> relatedArticles;
    private List<ArticleAuthor> authors;
    private List<ArticleEditor> editors;
    private Set<Journal> journals;

    public Builder setDoi(String doi) {
      this.doi = doi;
      return this;
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder setEIssn(String eIssn) {
      this.eIssn = eIssn;
      return this;
    }

    public Builder setState(int state) {
      this.state = state;
      return this;
    }

    public Builder setArchiveName(String archiveName) {
      this.archiveName = archiveName;
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

    public Builder setPages(String pages) {
      this.pages = pages;
      return this;
    }

    public Builder setELocationId(String eLocationId) {
      this.eLocationId = eLocationId;
      return this;
    }

    public Builder setStrkImgURI(String strkImgURI) {
      this.strkImgURI = strkImgURI;
      return this;
    }

    public Builder setDate(Date date) {
      this.date = date;
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

    public Builder setJournal(String journal) {
      this.journal = journal;
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

    public Builder setCollaborativeAuthors(List<String> collaborativeAuthors) {
      this.collaborativeAuthors = collaborativeAuthors;
      return this;
    }

    public Builder setTypes(Set<String> types) {
      this.types = types;
      return this;
    }

    public Builder setCategories(Set<Category> categories) {
      this.categories = categories;
      return this;
    }

    public Builder setAssets(List<ArticleAsset> assets) {
      this.assets = assets;
      return this;
    }

    public Builder setCitedArticles(List<CitedArticle> citedArticles) {
      this.citedArticles = citedArticles;
      return this;
    }

    public Builder setRelatedArticles(List<ArticleRelationship> relatedArticles) {
      this.relatedArticles = relatedArticles;
      return this;
    }

    public Builder setAuthors(List<ArticleAuthor> authors) {
      this.authors = authors;
      return this;
    }

    public Builder setEditors(List<ArticleEditor> editors) {
      this.editors = editors;
      return this;
    }

    public Builder setJournals(Set<Journal> journals) {
      this.journals = journals;
      return this;
    }

    public ExpectedArticle build() {
      return new ExpectedArticle(this);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != getClass()) return false;
    ExpectedArticle that = (ExpectedArticle) obj;
    if (!Objects.equal(this.doi, that.doi)) return false;
    if (!Objects.equal(this.title, that.title)) return false;
    if (!Objects.equal(this.eIssn, that.eIssn)) return false;
    if (!Objects.equal(this.state, that.state)) return false;
    if (!Objects.equal(this.archiveName, that.archiveName)) return false;
    if (!Objects.equal(this.description, that.description)) return false;
    if (!Objects.equal(this.rights, that.rights)) return false;
    if (!Objects.equal(this.language, that.language)) return false;
    if (!Objects.equal(this.format, that.format)) return false;
    if (!Objects.equal(this.pages, that.pages)) return false;
    if (!Objects.equal(this.eLocationId, that.eLocationId)) return false;
    if (!Objects.equal(this.strkImgURI, that.strkImgURI)) return false;
    if (!Objects.equal(this.date, that.date)) return false;
    if (!Objects.equal(this.volume, that.volume)) return false;
    if (!Objects.equal(this.issue, that.issue)) return false;
    if (!Objects.equal(this.journal, that.journal)) return false;
    if (!Objects.equal(this.publisherLocation, that.publisherLocation)) return false;
    if (!Objects.equal(this.publisherName, that.publisherName)) return false;
    if (!Objects.equal(this.url, that.url)) return false;
    if (!Objects.equal(this.collaborativeAuthors, that.collaborativeAuthors)) return false;
    if (!Objects.equal(this.types, that.types)) return false;
    if (!Objects.equal(this.categories, that.categories)) return false;
    if (!Objects.equal(this.assets, that.assets)) return false;
    if (!Objects.equal(this.citedArticles, that.citedArticles)) return false;
    if (!Objects.equal(this.relatedArticles, that.relatedArticles)) return false;
    if (!Objects.equal(this.authors, that.authors)) return false;
    if (!Objects.equal(this.editors, that.editors)) return false;
    if (!Objects.equal(this.journals, that.journals)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int hash = 1;
    hash = prime * hash + ObjectUtils.hashCode(doi);
    hash = prime * hash + ObjectUtils.hashCode(title);
    hash = prime * hash + ObjectUtils.hashCode(eIssn);
    hash = prime * hash + ObjectUtils.hashCode(state);
    hash = prime * hash + ObjectUtils.hashCode(archiveName);
    hash = prime * hash + ObjectUtils.hashCode(description);
    hash = prime * hash + ObjectUtils.hashCode(rights);
    hash = prime * hash + ObjectUtils.hashCode(language);
    hash = prime * hash + ObjectUtils.hashCode(format);
    hash = prime * hash + ObjectUtils.hashCode(pages);
    hash = prime * hash + ObjectUtils.hashCode(eLocationId);
    hash = prime * hash + ObjectUtils.hashCode(strkImgURI);
    hash = prime * hash + ObjectUtils.hashCode(date);
    hash = prime * hash + ObjectUtils.hashCode(volume);
    hash = prime * hash + ObjectUtils.hashCode(issue);
    hash = prime * hash + ObjectUtils.hashCode(journal);
    hash = prime * hash + ObjectUtils.hashCode(publisherLocation);
    hash = prime * hash + ObjectUtils.hashCode(publisherName);
    hash = prime * hash + ObjectUtils.hashCode(url);
    hash = prime * hash + ObjectUtils.hashCode(collaborativeAuthors);
    hash = prime * hash + ObjectUtils.hashCode(types);
    hash = prime * hash + ObjectUtils.hashCode(categories);
    hash = prime * hash + ObjectUtils.hashCode(assets);
    hash = prime * hash + ObjectUtils.hashCode(citedArticles);
    hash = prime * hash + ObjectUtils.hashCode(relatedArticles);
    hash = prime * hash + ObjectUtils.hashCode(authors);
    hash = prime * hash + ObjectUtils.hashCode(editors);
    hash = prime * hash + ObjectUtils.hashCode(journals);
    return hash;
  }
}
