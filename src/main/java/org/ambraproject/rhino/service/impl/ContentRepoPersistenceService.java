package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;

import java.util.Collection;

public interface ContentRepoPersistenceService {

  public ArticleItem createItem(ArticleItemInput itemInput, ArticleIngestion ingestion);

  public Collection<ArticleFile> persistAncillaryFiles(ArticlePackage articlePackage,
                                                       ArticleIngestion ingestion);
}
