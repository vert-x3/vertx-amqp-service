/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/

package io.vertx.ext.amqp;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;import java.util.HashSet;import java.util.List;import java.util.Map;import java.util.Set;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
public class AmqpServiceVertxEBProxy implements AMQPService {

  private Vertx _vertx;
  private String _address;
  private boolean closed;

  public AmqpServiceVertxEBProxy(Vertx vertx, String address) {
    this._vertx = vertx;
    this._address = address;
  }

  public AMQPService establishIncomingLink(String amqpAddress, String eventbusAddress, String notificationAddress, IncomingLinkOptions options, Handler<AsyncResult<String>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("amqpAddress", amqpAddress);
    _json.put("eventbusAddress", eventbusAddress);
    _json.put("notificationAddress", notificationAddress);
    _json.put("options", options == null ? null : options.toJson());
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "establishIncomingLink");
    _vertx.eventBus().<String>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService fetch(String incomingLinkRef, int messages, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("incomingLinkRef", incomingLinkRef);
    _json.put("messages", messages);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "fetch");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService cancelIncomingLink(String incomingLinkRef, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("incomingLinkRef", incomingLinkRef);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "cancelIncomingLink");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService establishOutgoingLink(String amqpAddress, String eventbusAddress, String notificationAddress, OutgoingLinkOptions options, Handler<AsyncResult<String>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("amqpAddress", amqpAddress);
    _json.put("eventbusAddress", eventbusAddress);
    _json.put("notificationAddress", notificationAddress);
    _json.put("options", options == null ? null : options.toJson());
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "establishOutgoingLink");
    _vertx.eventBus().<String>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService cancelOutgoingLink(String outgoingLinkRef, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("outgoingLinkRef", outgoingLinkRef);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "cancelOutgoingLink");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService accept(String msgRef, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("msgRef", msgRef);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "accept");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService reject(String msgRef, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("msgRef", msgRef);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "reject");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService release(String msgRef, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("msgRef", msgRef);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "release");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService registerService(String eventbusAddress, String notificationAddres, ServiceOptions options, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("eventbusAddress", eventbusAddress);
    _json.put("notificationAddres", notificationAddres);
    _json.put("options", options == null ? null : options.toJson());
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "registerService");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService unregisterService(String eventbusAddress, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("eventbusAddress", eventbusAddress);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "unregisterService");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService issueCredits(String linkId, int credits, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("linkId", linkId);
    _json.put("credits", credits);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "issueCredits");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AMQPService addInboundRoute(String pattern, String eventBusAddress) {
    if (closed) {
      throw new IllegalStateException("Proxy is closed");
    }
    JsonObject _json = new JsonObject();
    _json.put("pattern", pattern);
    _json.put("eventBusAddress", eventBusAddress);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "addInboundRoute");
    _vertx.eventBus().send(_address, _json, _deliveryOptions);
    return this;
  }

  public AMQPService removeInboundRoute(String pattern, String eventBusAddress) {
    if (closed) {
      throw new IllegalStateException("Proxy is closed");
    }
    JsonObject _json = new JsonObject();
    _json.put("pattern", pattern);
    _json.put("eventBusAddress", eventBusAddress);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "removeInboundRoute");
    _vertx.eventBus().send(_address, _json, _deliveryOptions);
    return this;
  }

  public AMQPService addOutboundRoute(String pattern, String amqpAddress) {
    if (closed) {
      throw new IllegalStateException("Proxy is closed");
    }
    JsonObject _json = new JsonObject();
    _json.put("pattern", pattern);
    _json.put("amqpAddress", amqpAddress);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "addOutboundRoute");
    _vertx.eventBus().send(_address, _json, _deliveryOptions);
    return this;
  }

  public AMQPService removeOutboundRoute(String pattern, String amqpAddress) {
    if (closed) {
      throw new IllegalStateException("Proxy is closed");
    }
    JsonObject _json = new JsonObject();
    _json.put("pattern", pattern);
    _json.put("amqpAddress", amqpAddress);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "removeOutboundRoute");
    _vertx.eventBus().send(_address, _json, _deliveryOptions);
    return this;
  }

  public void start() {
  }

  public void stop() {
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)jobj.intValue());
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)jobj.intValue());
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }
  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}