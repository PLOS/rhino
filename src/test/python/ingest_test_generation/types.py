import re
import string

import generate
from generate import cap, low, entity_getter

entity_types = {
    'Article': '''
  private String doi;

  //simple properties
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

  //simple collections
  private List<String> collaborativeAuthors;
  private Set<String> types;

  //collections of persistent objects
  private Set<Category> categories;
  private List<ArticleAsset> assets;
  private List<CitedArticle> citedArticles;
  private List<ArticleRelationship> relatedArticles;
  private List<ArticleAuthor> authors;
  private List<ArticleEditor> editors;
  private Set<Journal> journals;
''',

    'ArticleAsset': '''
  private String doi;
  private String contextElement;
  private String extension;
  private String contentType;
  private String title;
  private String description;
  private long size;
''',

    'ArticlePerson': '''
  private String fullName;
  private String givenNames;
  private String surnames;
  private String suffix;
''',

    'CitedArticle': '''
  private String key;
  private Integer year;
  private String displayYear;
  private String month;
  private String day;
  private Integer volumeNumber;
  private String volume;
  private String issue;
  private String title;
  private String publisherLocation;
  private String publisherName;
  private String pages;
  private String eLocationID;
  private String journal;
  private String note;
  private List<String> collaborativeAuthors;
  private String url;
  private String doi;
  private String summary;
  private String citationType;

  private List<CitedArticleAuthor> authors;
  private List<CitedArticleEditor> editors;
''',

    'CitedArticlePerson': '''
  private String fullName;
  private String givenNames;
  private String surnames;
  private String suffix;
''',

    }

def fields_for(java_field_decls):
    p = re.compile(r'private\s+([\w<>]+)\s+(\w+)\s*;')
    return p.findall(java_field_decls)

def print_assertion_classes():
    for (java_type, field_declaration_blob) in entity_types.items():
        fields = fields_for(field_declaration_blob)
        generate.subclass_for(java_type, fields)

def print_assertion_sets():
    fields = fields_for(entity_types['Article'])
    for (t, n) in fields:
        if ('<' not in t) or ('<String>' in t):
            print(('results.compare(Article.class, "{n}", '
                   'actual.{g}(), expected.{g}());')
                  .format(n=n, g=entity_getter(n)))

print_assertion_sets()
