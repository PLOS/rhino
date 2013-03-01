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

package org.ambraproject.rhino.service;

import org.ambraproject.queue.MessageSender;
import org.w3c.dom.Document;

/**
 * Implementation of {@link MessageSender} that does nothing.
 * <p/>
 * TODO: consider keeping track of what is sent for asserts in tests.
 */
public class DummyMessageSender implements MessageSender {

  @Override
  public void sendMessage(String destination, String body) {
  }

  @Override
  public void sendMessage(String destination, Document body) {
  }
}
