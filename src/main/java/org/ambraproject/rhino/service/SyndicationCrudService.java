/*
 * $HeadURL$
 * $Id$
 *
 * Copyright (c) 2006-2010 by Public Library of Science
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

import org.ambraproject.rhino.identity.ArticleVersionIdentifier;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;
import java.util.List;

/**
 * Manage the syndication process, including creating and updating Syndication objects, as well as pushing syndication
 * messages to a message queue.
 *
 * @author Scott Sterling
 * @author Alex Kudlick
 */
public interface SyndicationCrudService {

  /**
   * Get the list of Syndication objects for this <code>articleDoi</code>. If there are no Syndications for this
   * articleDoi, then return an empty List. `
   *
   * @param versionId The unique identifier for the Article which was (or is to be) syndicated
   * @return The List of Syndications for this <code>articleDoi</code>. If there are no Syndications for this
   * articleDoi, then return an empty List
   */
  public List<Syndication> getSyndications(ArticleVersionIdentifier versionId);

  /**
   * Return the syndication for the given article and the given target.  Return null if there is none.
   *
   * @param versionId the doi of the article to query
   * @param syndicationTarget     the syndication target
   * @return the matching syndication, if it exists, else null
   */
  public Syndication getSyndication(ArticleVersionIdentifier versionId, String syndicationTarget);

  /**
   * Update the Syndication object specified by the <code>articleDoi</code> and <code>syndicationTarget</code>
   * parameters.
   * <p/>
   * <ul> <li>For any <code>status</code> other than <i>pending</i> (e.g., failure, success, etc) the Syndication object
   * will be updated with the given values</li>
   * <p/>
   * <li>If the <code>status</code> is <i>pending</i> <strong>and</strong> a Syndication object already exists for this
   * <code>articleDoi</code> and <code>syndicationTarget</code>, then no action will be performed. The existing
   * Syndication object will be returned</li>
   *
   * @param versionId        The unique identifier for the Article which was (or is to be) syndicated
   * @param syndicationTarget The organization to which this Article was (or will be) syndicated
   * @param status            The current status of this syndication (e.g., pending, failure, success, etc)
   * @param errorMessage      Any failure during the process of updating this Syndication. A null in this field will
   *                          <strong>not</strong> update the errorMessage of this Syndication
   * @return The Syndication that matches the <code>articleDoi</code> and <code>syndicationTarget</code> parameters
   */
  public Syndication updateSyndication(ArticleVersionIdentifier versionId, String syndicationTarget, String status, String errorMessage);

  /**
   * For the Article indicated by <code>articleDoi</code>, create a new Syndication object for each possible syndication
   * target which does not already have a Syndication object. Return the complete list of Syndication objects for this
   * Article.
   * <p/>
   * If a Syndication object for a given syndication target already exists for this Article, then the datastore will not
   * be updated for that Syndication object. This silent failure mode is useful during the re-ingestion of any Article
   * which was previously published and syndicated.
   * <p/>
   *
   * @param versionId The unique identifier for the Article which was (or is to be) syndicated
   * @return The complete list of Syndication objects for this Article
   */
  public List<Syndication> createSyndications(ArticleVersionIdentifier versionId);

  public Syndication createSyndication(ArticleVersionIdentifier versionId, String target);

  /**
   * Get Syndications (from the current journal) that each have a <code>status</code> defined in statuses
   * and a <code>lastModified</code> within the past number of days defined by the configuration property
   * <code>ambra.virtualJournals.JOURN AL_KEY.syndications.display.numDaysInPast</code>, where <i>JOURNAL_KEY</i> is the
   * <code>journalKey</code> parameter.  By default, a syndication can be up to 30
   * days old and still appear in this list.
   *
   * @param journalKey Indicates which journal configuration is to be used when determining how many days in the past
   *                   the oldest Syndications can be.  This property is passed in because the Action class (which calls
   *                   this method) has easy access to this value, while this Service class does not
   * @return Syndications which have a <code>status</code> within the statuses list and a
   * <i>statusTimestamp</i> up to a certain number of days in the past.
   */
  public List<Syndication> getSyndications(String journalKey, List<String> statuses);

  public Transceiver readSyndications(String journalKey, List<String> statuses) throws IOException;

  /**
   * Send a message to the message queue indicating that the Article identified by <code>articleDoi</code> should be
   * syndicated to the syndication target identified by <code>syndicationTarget</code>.
   * <p/>
   * If the message is successfully pushed to the message queue, then the corresponding Syndication object will have its
   * status set to "in progress".  If the message cannot be pushed to the message queue, then the corresponding
   * Syndication object will have its status set to "failure".
   *
   * @param versionId        The ID for the Article which will be syndicated to the <code>syndicationTarget</code>
   * @param syndicationTarget The syndication target to which will be sent the Article designated by
   *                          <code>articleDoi</code>
   * @return The Syndication object which matches the <code>articleDoi</code> and <code>syndicationTarget</code>
   * parameters.  Contains the latest status information.
   */
  public Syndication syndicate(ArticleVersionIdentifier versionId, String syndicationTarget);

}