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

package org.ambraproject.rhino.config;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Comment;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

public class HibernateLoggingInterceptor extends EmptyInterceptor {

  private enum KafkaEventType {
    ADD, DELETE
  }

  private class KafkaJsonOutput {

    private Date timestamp;
    private double version = 1.0;
    private String hostname;
    private String message;
    private KafkaEventType event;

    public KafkaJsonOutput(String message, KafkaEventType event) {
      this.timestamp = new Date();
      this.event = event;
      this.message = message;
      try {
        this.hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }
    }
  }

  final private Gson entityGson;

  final private Producer<String, Object> kafkaEventProducer;

  public HibernateLoggingInterceptor(RuntimeConfiguration runtimeConfiguration, Gson entityGson) {
    super();
    this.entityGson = entityGson;
    Properties props = new Properties();
    Set<String> servers = runtimeConfiguration.getKafkaConfiguration().getServers();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Joiner.on(',').join(servers));
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "RhinoEventProducerService");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    kafkaEventProducer = new KafkaProducer<>(props);
  }

  @Override
  public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
    if (entity instanceof Comment) {
      Comment comment = (Comment) entity;
      sendMessage("ambra-comment", comment.getCommentUri(), KafkaEventType.ADD);
    } else if (entity instanceof ArticleRevision) {
      ArticleRevision revision = (ArticleRevision) entity;
      sendMessage("ambra-article", revision.getIngestion().getArticle().getDoi(), KafkaEventType.ADD);
    }
    return super.onSave(entity, id, state, propertyNames, types);
  }

  private void sendMessage(String topic, String message, KafkaEventType eventType) {
    final KafkaJsonOutput output = new KafkaJsonOutput(message, eventType);
    kafkaEventProducer.send(new ProducerRecord<>(topic, entityGson.toJson(output)));
  }
}
