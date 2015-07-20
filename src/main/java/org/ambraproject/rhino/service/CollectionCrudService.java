package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.ArticleCollection;
import org.ambraproject.rhino.util.response.Transceiver;

import java.util.Collection;
import java.util.Set;

public interface CollectionCrudService {

  ArticleCollection create(String slug, String journalKey, String title, Set<ArticleIdentity> articleIds);

  Transceiver read(String journalKey, String slug);

  Collection<ArticleCollection> findContainingCollections(ArticleIdentity articleId);

}
