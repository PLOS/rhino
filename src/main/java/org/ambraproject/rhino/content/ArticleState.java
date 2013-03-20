/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.content;

/**
 * Bean representing the state of an article: for example, whether it has been published, syndicated, re-ingested, etc.
 * <p/>
 * This is not a persistent entity, but rather a bean used in communicating over a ReST API to read and write the state
 * of an article.  Where these states are actually stored on the backend is not this class's concern.
 */
public class ArticleState {

  /**
   * Represents legal syndication targets.
   * <p/>
   * In the old codebase, this is loaded from ambra.xml.  That seems like overkill considering that we only have two of
   * these, and they change rarely.
   */
  public enum SyndicationTarget {
    CROSSREF,
    PMC;
  }

  /**
   * Represents legal states for a syndication attempt to be in.  This is an enum-duplication of the Syndication.status
   * property.
   */
  public enum SyndicationState {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILURE;
  }

  private boolean published;

  private SyndicationState crossRefSyndicationState;

  private SyndicationState pmcSyndicationState;

  public SyndicationState getSyndicationState(SyndicationTarget target) {
    switch (target) {
      case CROSSREF:
        return crossRefSyndicationState;

      case PMC:
        return pmcSyndicationState;

      default:
        throw new IllegalArgumentException("Unexpected syndication target " + target);
    }
  }

  public void setSyndicationState(SyndicationTarget target, SyndicationState state) {
    switch (target) {
      case CROSSREF:
        setCrossRefSyndicationState(state);
        break;

      case PMC:
        setPmcSyndicationState(state);
        break;

      default:
        throw new IllegalArgumentException("Unexpected syndication target " + target);
    }
  }

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  public SyndicationState getCrossRefSyndicationState() {
    return crossRefSyndicationState;
  }

  public void setCrossRefSyndicationState(SyndicationState crossRefSyndicationState) {
    this.crossRefSyndicationState = crossRefSyndicationState;
  }

  public SyndicationState getPmcSyndicationState() {
    return pmcSyndicationState;
  }

  public void setPmcSyndicationState(SyndicationState pmcSyndicationState) {
    this.pmcSyndicationState = pmcSyndicationState;
  }
}
