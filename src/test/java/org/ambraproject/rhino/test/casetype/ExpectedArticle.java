package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableCollection;
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

  public ExpectedArticle() {
    super(Article.class);
  }

  @Override
  public ImmutableCollection<AssertionFailure<?>> test(Article article) {
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

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getEIssn() {
    return eIssn;
  }

  public void setEIssn(String eIssn) {
    this.eIssn = eIssn;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public String getArchiveName() {
    return archiveName;
  }

  public void setArchiveName(String archiveName) {
    this.archiveName = archiveName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRights() {
    return rights;
  }

  public void setRights(String rights) {
    this.rights = rights;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getPages() {
    return pages;
  }

  public void setPages(String pages) {
    this.pages = pages;
  }

  public String getELocationId() {
    return eLocationId;
  }

  public void setELocationId(String eLocationId) {
    this.eLocationId = eLocationId;
  }

  public String getStrkImgURI() {
    return strkImgURI;
  }

  public void setStrkImgURI(String strkImgURI) {
    this.strkImgURI = strkImgURI;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getVolume() {
    return volume;
  }

  public void setVolume(String volume) {
    this.volume = volume;
  }

  public String getIssue() {
    return issue;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  public String getJournal() {
    return journal;
  }

  public void setJournal(String journal) {
    this.journal = journal;
  }

  public String getPublisherLocation() {
    return publisherLocation;
  }

  public void setPublisherLocation(String publisherLocation) {
    this.publisherLocation = publisherLocation;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public void setPublisherName(String publisherName) {
    this.publisherName = publisherName;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<String> getCollaborativeAuthors() {
    return collaborativeAuthors;
  }

  public void setCollaborativeAuthors(List<String> collaborativeAuthors) {
    this.collaborativeAuthors = collaborativeAuthors;
  }

  public Set<String> getTypes() {
    return types;
  }

  public void setTypes(Set<String> types) {
    this.types = types;
  }

  public Set<Category> getCategories() {
    return categories;
  }

  public void setCategories(Set<Category> categories) {
    this.categories = categories;
  }

  public List<ArticleAsset> getAssets() {
    return assets;
  }

  public void setAssets(List<ArticleAsset> assets) {
    this.assets = assets;
  }

  public List<CitedArticle> getCitedArticles() {
    return citedArticles;
  }

  public void setCitedArticles(List<CitedArticle> citedArticles) {
    this.citedArticles = citedArticles;
  }

  public List<ArticleRelationship> getRelatedArticles() {
    return relatedArticles;
  }

  public void setRelatedArticles(List<ArticleRelationship> relatedArticles) {
    this.relatedArticles = relatedArticles;
  }

  public List<ArticleAuthor> getAuthors() {
    return authors;
  }

  public void setAuthors(List<ArticleAuthor> authors) {
    this.authors = authors;
  }

  public List<ArticleEditor> getEditors() {
    return editors;
  }

  public void setEditors(List<ArticleEditor> editors) {
    this.editors = editors;
  }

  public Set<Journal> getJournals() {
    return journals;
  }

  public void setJournals(Set<Journal> journals) {
    this.journals = journals;
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
