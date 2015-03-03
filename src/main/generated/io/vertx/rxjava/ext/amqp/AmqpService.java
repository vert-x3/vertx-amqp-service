/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.rxjava.ext.amqp;

import java.util.Map;
import io.vertx.lang.rxjava.InternalHelper;
import rx.Observable;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.ReceiverMode;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.rxjava.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * AMQP service allows you to directly use API methods to subscribe, publish,
 * issue credits and message acks without having to use control-messages via the
 * event bus.
 * 
 * @author <a href="mailto:rajith@redhat.com">Rajith Attapattu</a>
 *
 * NOTE: This class has been automatically generated from the original non RX-ified interface using Vert.x codegen.
 */

public class AmqpService {

  final io.vertx.ext.amqp.AmqpService delegate;

  public AmqpService(io.vertx.ext.amqp.AmqpService delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  public static AmqpService create(Vertx vertx, JsonObject config) {
    AmqpService ret= AmqpService.newInstance(io.vertx.ext.amqp.AmqpService.create((io.vertx.core.Vertx) vertx.getDelegate(), config));
    return ret;
  }

  public static AmqpService createEventBusProxy(Vertx vertx, String address) {
    AmqpService ret= AmqpService.newInstance(io.vertx.ext.amqp.AmqpService.createEventBusProxy((io.vertx.core.Vertx) vertx.getDelegate(), address));
    return ret;
  }

  /**
   * Allows an application to create a subscription to an AMQP message source.
   * The service will receive the messages on behalf of the application and
   * forward it to the event-bus address specified in the consume method. The
   * application will be listening on this address.
   * 
   * @param amqpAddress
   *            The address that identifies the AMQP message source to
   *            subscribe from.
   * @param ebAddress
   *            The event-bus address the application is listening on for the
   *            messages.
   * @param receiverMode
   *            Specified the reliability expected.
   * @param creditMode
   *            Specifies how credit is replenished.
   * @param result
   *            AsyncResult that contains a String ref to the AMQP 'consumer',
   *            if successfully created.
   * @return A reference to the service.
   */
  public AmqpService consume(String amqpAddress, String ebAddress, ReceiverMode receiverMode, CreditMode creditMode, Handler<AsyncResult<String>> result) {
    this.delegate.consume(amqpAddress, ebAddress, receiverMode, creditMode, result);
    return this;
  }

  public Observable<String> consumeObservable(String amqpAddress, String ebAddress, ReceiverMode receiverMode, CreditMode creditMode) {
    io.vertx.rx.java.ObservableFuture<String> result = io.vertx.rx.java.RxHelper.observableFuture();
    consume(amqpAddress, ebAddress, receiverMode, creditMode, result.toHandler());
    return result;
  }

  /**
   * Allows an application to issue message credits for flow control purposes.
   * 
   * @see CreditMode to understand how msg credits works.
   * 
   * @param consumerRef
   *            The String ref return by the consume method.
   * @param credits
   *            The message credits
   * @param result
   *            Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService issueCredit(String consumerRef, int credits, Handler<AsyncResult<Void>> result) {
    this.delegate.issueCredit(consumerRef, credits, result);
    return this;
  }

  public Observable<Void> issueCreditObservable(String consumerRef, int credits) {
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    issueCredit(consumerRef, credits, result.toHandler());
    return result;
  }

  /**
   * Allows an application to cancel a subscription it has previously created.
   * 
   * @param consumerRef
   *            The String ref return by the consume method.
   * @param result
   *            Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService unregisterConsume(String consumerRef, Handler<AsyncResult<Void>> result) {
    this.delegate.unregisterConsume(consumerRef, result);
    return this;
  }

  public Observable<Void> unregisterConsumeObservable(String consumerRef) {
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    unregisterConsume(consumerRef, result.toHandler());
    return result;
  }

  /**
   * Allows an application to acknowledge a message and set it's disposition.
   * 
   * @param msgRef
   *            - The string ref. Use {@link AmqpMessage#getMsgRef()}
   * @param disposition
   *            - One of ACCEPT, REJECT OR RELEASED.
   * @param result
   *            Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService acknowledge(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result) {
    this.delegate.acknowledge(msgRef, disposition, result);
    return this;
  }

  public Observable<Void> acknowledgeObservable(String msgRef, MessageDisposition disposition) {
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    acknowledge(msgRef, disposition, result.toHandler());
    return result;
  }

  /**
   * Allows an application to publish a message to an AMQP target.
   * 
   * @param address
   *            The AMQP target to which the messages should be sent.
   * @param msg
   *            The message to be sent.
   * @param result
   *            A JsonObject containing the delivery state and disposition.
   * @return
   */
  public AmqpService publish(String address, JsonObject msg, Handler<AsyncResult<JsonObject>> result) {
    this.delegate.publish(address, msg, result);
    return this;
  }

  public Observable<JsonObject> publishObservable(String address, JsonObject msg) {
    io.vertx.rx.java.ObservableFuture<JsonObject> result = io.vertx.rx.java.RxHelper.observableFuture();
    publish(address, msg, result.toHandler());
    return result;
  }

  /**
   * Start the service
   */
  public void start() {
    this.delegate.start();
  }

  /**
   * Stop the service
   */
  public void stop() {
    this.delegate.stop();
  }


  public static AmqpService newInstance(io.vertx.ext.amqp.AmqpService arg) {
    return new AmqpService(arg);
  }
}
