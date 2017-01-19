/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.view.article.SyndicationOutputView;

import java.io.IOException;
import java.util.Collection;
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
   * @param revisionId The unique identifier for the Article which was (or is to be) syndicated
   * @return The List of Syndications for this <code>articleDoi</code>. If there are no Syndications for this
   * articleDoi, then return an empty List
   */
  public List<Syndication> getSyndications(ArticleRevisionIdentifier revisionId);

  /**
   * Return the syndication for the given article and the given target.  Return null if there is none.
   *
   * @param revisionId             the doi of the article to query
   * @param syndicationTargetQueue the syndication target
   * @return the matching syndication, if it exists, else null
   */
  public Syndication getSyndication(ArticleRevisionIdentifier revisionId, String syndicationTargetQueue);

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
   * @param revisionId        The unique identifier for the Article which was (or is to be) syndicated
   * @param syndicationTargetQueue The organization to which this Article was (or will be) syndicated
   * @param status            The current status of this syndication (e.g., pending, failure, success, etc)
   * @param errorMessage      Any failure during the process of updating this Syndication. A null in this field will
   *                          <strong>not</strong> update the errorMessage of this Syndication
   * @return The Syndication that matches the <code>articleDoi</code> and <code>syndicationTarget</code> parameters
   */
  public Syndication updateSyndication(ArticleRevisionIdentifier revisionId, String syndicationTargetQueue, String status, String errorMessage);

  /**
   * For the Article indicated by <code>articleDoi</code>, create a new Syndication object specified by the input
   * target. Return the Syndication object for this Article.
   * <p/>
   * If a Syndication object for a given syndication target already exists for this Article, then the datastore will not
   * be updated for that Syndication object. This silent failure mode is useful during the re-ingestion of any Article
   * which was previously published and syndicated.
   * <p/>
   *
   * @param revisionId             The unique identifier for the Article which was (or is to be) syndicated
   * @param syndicationTargetQueue The syndication target to which will be sent the Article designated by
   *                               <code>articleDoi</code>
   * @return The complete list of Syndication objects for this Article
   */
  public Syndication createSyndication(ArticleRevisionIdentifier revisionId, String syndicationTargetQueue);

  /**
   * Get Syndications (from the current journal) that each have a <code>status</code> defined in statuses and a
   * <code>lastModified</code> within the past number of days defined by the configuration property {@link
   * RuntimeConfiguration.QueueConfiguration#getSyndicationRange()}. By default, a syndication can be up to 30 days old
   * and still appear in this list.
   *
   * @param journalKey Indicates which journal configuration is to be used when determining how many days in the past
   *                   the oldest Syndications can be.  This property is passed in because the Action class (which calls
   *                   this method) has easy access to this value, while this Service class does not
   * @return Syndications which have a <code>status</code> within the statuses list and a <i>statusTimestamp</i> up to a
   * certain number of days in the past.
   */
  public Collection<Syndication> getSyndications(String journalKey, List<String> statuses);

  public ServiceResponse<Collection<SyndicationOutputView>> readSyndications(String journalKey, List<String> statuses) throws IOException;

  /**
   * Send a message to the message queue indicating that the Article identified by <code>articleDoi</code> should be
   * syndicated to the syndication target identified by <code>syndicationTarget</code>.
   * <p/>
   * If the message is successfully pushed to the message queue, then the corresponding Syndication object will have its
   * status set to "in progress".  If the message cannot be pushed to the message queue, then the corresponding
   * Syndication object will have its status set to "failure".
   *
   * @param revisionId             The ID for the Article which will be syndicated to the <code>syndicationTarget</code>
   * @param syndicationTargetQueue String indicating which Queue endpoint to send the syndication message to
   * @return The Syndication object which matches the <code>articleDoi</code> and <code>syndicationTarget</code>
   * parameters.  Contains the latest status information.
   */
  public Syndication syndicate(ArticleRevisionIdentifier revisionId, String syndicationTargetQueue);

}
