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

import io.vertx.ext.amqp.AmqpService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;import java.util.HashSet;import java.util.List;import java.util.Map;import java.util.Set;import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.ReceiverMode;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
public class AmqpServiceVertxEBProxy implements AmqpService {

  private Vertx _vertx;
  private String _address;
  private boolean closed;

  public AmqpServiceVertxEBProxy(Vertx vertx, String address) {
    this._vertx = vertx;
    this._address = address;
  }

  public AmqpService consume(String amqpAddress, String ebAddress, ReceiverMode receiverMode, CreditMode creditMode, Handler<AsyncResult<String>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("amqpAddress", amqpAddress);
    _json.put("ebAddress", ebAddress);
    _json.put("receiverMode", receiverMode.toString());
    _json.put("creditMode", creditMode.toString());
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "consume");
    _vertx.eventBus().<String>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AmqpService issueCredit(String consumerRef, int credits, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("consumerRef", consumerRef);
    _json.put("credits", credits);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "issueCredit");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AmqpService unregisterConsume(String consumerRef, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("consumerRef", consumerRef);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "unregisterConsume");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AmqpService acknowledge(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("msgRef", msgRef);
    _json.put("disposition", disposition.toString());
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "acknowledge");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AmqpService publish(String address, JsonObject msg, Handler<AsyncResult<JsonObject>> result) {
    if (closed) {
      result.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("address", address);
    _json.put("msg", msg);
    DeliveryOptions _deliveryOptions = new DeliveryOptions();
    _deliveryOptions.addHeader("action", "publish");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        result.handle(Future.failedFuture(res.cause()));
      } else {
        result.handle(Future.succeededFuture(res.result().body()));
      }
    });
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