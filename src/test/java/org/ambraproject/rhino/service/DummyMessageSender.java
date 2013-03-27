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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.ambraproject.queue.MessageSender;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * Implementation of {@link MessageSender} for testing.
 */
public class DummyMessageSender implements MessageSender {

  public ListMultimap<String, String> messagesSent = LinkedListMultimap.create();

  @Override
  public void sendMessage(String destination, String body) {
    messagesSent.put(destination, body);
  }

  @Override
  public void sendMessage(String destination, Document body) {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "no");
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      DOMSource source = new DOMSource(body);
      transformer.transform(source, result);
      messagesSent.put(destination, writer.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
