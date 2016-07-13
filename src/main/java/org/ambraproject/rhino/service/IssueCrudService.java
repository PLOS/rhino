/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.IssueArticle;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleIssue;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.VolumeNonAssocView;

import java.io.IOException;
import java.util.List;

public interface IssueCrudService {

  public abstract Transceiver read(IssueIdentifier id) throws IOException;

  public abstract DoiBasedIdentity create(DoiBasedIdentity volumeId, IssueInputView input);

  public abstract void update(IssueIdentifier issueId, IssueInputView input);

  public abstract List<ArticleIssue> getArticleIssues(ArticleIdentity articleIdentity);

  public abstract VolumeNonAssocView getParentVolumeView(Issue issue);

  public abstract Volume getParentVolume(Issue issue);

  public abstract Issue getIssue(IssueIdentifier issueId);

  public abstract List<IssueArticle> getIssueArticles(Issue issue);

  public abstract List<ArticleTable> getArticles(Issue issue);

  public abstract void delete(IssueIdentifier issueId);

}
