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
import io.vertx.ext.amqp.ServiceOptions;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.amqp.IncomingLinkOptions;

/**
 * AMQP service allows a Vert.x application to,
 * <ul>
 * <li>Establish and cancel incoming/outgoing AMQP links, and map the link it to
 * an event-bus address.</li>
 * <li>Configure the link behavior</li>
 * <li>Control the flow of messages both incoming and outgoing to maintain QoS</li>
 * <li>Send and Receive messages from AMQP peers with different reliability
 * guarantees</li>
 * </ul>
 * <p/>
 * For more information on AMQP visit www.amqp.org This service speaks AMQP 1.0
 * and use QPid Proton(http://qpid.apache.org/proton) for protocol support.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.amqp.AmqpService original} non RX-ified interface using Vert.x codegen.
 */

public class AmqpService {

  final io.vertx.ext.amqp.AmqpService delegate;

  public AmqpService(io.vertx.ext.amqp.AmqpService delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  public static AmqpService createEventBusProxy(Vertx vertx, String address) { 
    AmqpService ret= AmqpService.newInstance(io.vertx.ext.amqp.AmqpService.createEventBusProxy((io.vertx.core.Vertx) vertx.getDelegate(), address));
    return ret;
  }

  /**
   * Allows an application to establish a link to an AMQP message-source for
   * receiving messages. The vertx-amqp-service will receive the messages on
   * behalf of the application and forward it to the event-bus address
   * specified in the consume method. The application will be listening on
   * this address.
   * @param amqpAddress A link will be created to the the AMQP message-source identified by this address. .
   * @param eventbusAddress The event-bus address to be mapped to the above link. The application should register a handler for this address on the event bus to receive the messages.
   * @param notificationAddress The event-bus address to which notifications about the incoming link is sent. Ex. Errors. The application should register a handler with the event-bus to receive these updates. Please see {@link io.vertx.ext.amqp.NotificationType} and {@link io.vertx.ext.amqp.NotificationHelper} for more details.
   * @param options Options to configure the link behavior (Ex prefetch, reliability). {@link io.vertx.ext.amqp.IncomingLinkOptions}
   * @param result The AsyncResult contains a ref (string) to the mapping created. This is required when changing behavior or canceling the link and it' association.
   * @return A reference to the service.
   */
  public AmqpService establishIncomingLink(String amqpAddress, String eventbusAddress, String notificationAddress, IncomingLinkOptions options, Handler<AsyncResult<String>> result) { 
    this.delegate.establishIncomingLink(amqpAddress, eventbusAddress, notificationAddress, options, result);
    return this;
  }

  /**
   * Allows an application to establish a link to an AMQP message-source for
   * receiving messages. The vertx-amqp-service will receive the messages on
   * behalf of the application and forward it to the event-bus address
   * specified in the consume method. The application will be listening on
   * this address.
   * @param amqpAddress A link will be created to the the AMQP message-source identified by this address. .
   * @param eventbusAddress The event-bus address to be mapped to the above link. The application should register a handler for this address on the event bus to receive the messages.
   * @param notificationAddress The event-bus address to which notifications about the incoming link is sent. Ex. Errors. The application should register a handler with the event-bus to receive these updates. Please see {@link io.vertx.ext.amqp.NotificationType} and {@link io.vertx.ext.amqp.NotificationHelper} for more details.
   * @param options Options to configure the link behavior (Ex prefetch, reliability). {@link io.vertx.ext.amqp.IncomingLinkOptions}
   * @return 
   */
  public Observable<String> establishIncomingLinkObservable(String amqpAddress, String eventbusAddress, String notificationAddress, IncomingLinkOptions options) { 
    io.vertx.rx.java.ObservableFuture<String> result = io.vertx.rx.java.RxHelper.observableFuture();
    establishIncomingLink(amqpAddress, eventbusAddress, notificationAddress, options, result.toHandler());
    return result;
  }

  /**
   * If prefetch was set to zero, this method allows the application to
   * explicitly fetch a certain number of messages. If prefetch > 0, the AMQP
   * service will prefetch messages for you automatically.
   * @param incomingLinkRef The String ref return by the establishIncommingLink method. This uniquely identifies the incoming link and it's mapping to an event-bus address.
   * @param messages The number of message to fetch.
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService fetch(String incomingLinkRef, int messages, Handler<AsyncResult<Void>> result) { 
    this.delegate.fetch(incomingLinkRef, messages, result);
    return this;
  }

  /**
   * If prefetch was set to zero, this method allows the application to
   * explicitly fetch a certain number of messages. If prefetch > 0, the AMQP
   * service will prefetch messages for you automatically.
   * @param incomingLinkRef The String ref return by the establishIncommingLink method. This uniquely identifies the incoming link and it's mapping to an event-bus address.
   * @param messages The number of message to fetch.
   * @return 
   */
  public Observable<Void> fetchObservable(String incomingLinkRef, int messages) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    fetch(incomingLinkRef, messages, result.toHandler());
    return result;
  }

  /**
   * Allows an application to cancel an incoming link and remove it's mapping
   * to an event-bus address.
   * @param incomingLinkRef The String ref return by the establishIncommingLink method. This uniquely identifies the incoming link and it's mapping to an event-bus address.
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService cancelIncomingLink(String incomingLinkRef, Handler<AsyncResult<Void>> result) { 
    this.delegate.cancelIncomingLink(incomingLinkRef, result);
    return this;
  }

  /**
   * Allows an application to cancel an incoming link and remove it's mapping
   * to an event-bus address.
   * @param incomingLinkRef The String ref return by the establishIncommingLink method. This uniquely identifies the incoming link and it's mapping to an event-bus address.
   * @return 
   */
  public Observable<Void> cancelIncomingLinkObservable(String incomingLinkRef) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    cancelIncomingLink(incomingLinkRef, result.toHandler());
    return result;
  }

  /**
   * Allows an application to establish a link to an AMQP message-sink for
   * sending messages. The application will send the messages to the event-bus
   * address. The AMQP service will receive these messages via the event-bus
   * and forward it to the respective AMQP message sink.
   * @param amqpAddress A link will be created to the the AMQP message-sink identified by this address.
   * @param eventbusAddress The event-bus address to be mapped to the above link. The application should send the messages using this address.
   * @param notificationAddress The event-bus address to which notifications about the outgoing link is sent. Ex. Errors, Delivery Status, credit availability. The application should register a handler with the event-bus to receive these updates. Please see {@link io.vertx.ext.amqp.NotificationType} and {@link io.vertx.ext.amqp.NotificationHelper} for more details.
   * @param options Options to configure the link behavior (Ex reliability). {@link io.vertx.ext.amqp.IncomingLinkOptions}
   * @param result The AsyncResult contains a ref (string) to the mapping created. This is required when changing behavior or canceling the link and it' association.
   * @return A reference to the service.
   */
  public AmqpService establishOutgoingLink(String amqpAddress, String eventbusAddress, String notificationAddress, OutgoingLinkOptions options, Handler<AsyncResult<String>> result) { 
    this.delegate.establishOutgoingLink(amqpAddress, eventbusAddress, notificationAddress, options, result);
    return this;
  }

  /**
   * Allows an application to establish a link to an AMQP message-sink for
   * sending messages. The application will send the messages to the event-bus
   * address. The AMQP service will receive these messages via the event-bus
   * and forward it to the respective AMQP message sink.
   * @param amqpAddress A link will be created to the the AMQP message-sink identified by this address.
   * @param eventbusAddress The event-bus address to be mapped to the above link. The application should send the messages using this address.
   * @param notificationAddress The event-bus address to which notifications about the outgoing link is sent. Ex. Errors, Delivery Status, credit availability. The application should register a handler with the event-bus to receive these updates. Please see {@link io.vertx.ext.amqp.NotificationType} and {@link io.vertx.ext.amqp.NotificationHelper} for more details.
   * @param options Options to configure the link behavior (Ex reliability). {@link io.vertx.ext.amqp.IncomingLinkOptions}
   * @return 
   */
  public Observable<String> establishOutgoingLinkObservable(String amqpAddress, String eventbusAddress, String notificationAddress, OutgoingLinkOptions options) { 
    io.vertx.rx.java.ObservableFuture<String> result = io.vertx.rx.java.RxHelper.observableFuture();
    establishOutgoingLink(amqpAddress, eventbusAddress, notificationAddress, options, result.toHandler());
    return result;
  }

  /**
   * Allows an application to cancel an outgoing link and remove it's mapping
   * to an event-bus address.
   * @param outgoingLinkRef The String ref return by the establishOutgoingLink method. This uniquely identifies the outgoing link and it's mapping to an event-bus address.
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService cancelOutgoingLink(String outgoingLinkRef, Handler<AsyncResult<Void>> result) { 
    this.delegate.cancelOutgoingLink(outgoingLinkRef, result);
    return this;
  }

  /**
   * Allows an application to cancel an outgoing link and remove it's mapping
   * to an event-bus address.
   * @param outgoingLinkRef The String ref return by the establishOutgoingLink method. This uniquely identifies the outgoing link and it's mapping to an event-bus address.
   * @return 
   */
  public Observable<Void> cancelOutgoingLinkObservable(String outgoingLinkRef) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    cancelOutgoingLink(outgoingLinkRef, result.toHandler());
    return result;
  }

  /**
   * Allows an application to accept a message it has received.
   * @param msgRef The string ref. Use 
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService accept(String msgRef, Handler<AsyncResult<Void>> result) { 
    this.delegate.accept(msgRef, result);
    return this;
  }

  /**
   * Allows an application to accept a message it has received.
   * @param msgRef The string ref. Use 
   * @return 
   */
  public Observable<Void> acceptObservable(String msgRef) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    accept(msgRef, result.toHandler());
    return result;
  }

  /**
   * Allows an application to reject a message it has received.
   * @param msgRef The string ref. Use 
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService reject(String msgRef, Handler<AsyncResult<Void>> result) { 
    this.delegate.reject(msgRef, result);
    return this;
  }

  /**
   * Allows an application to reject a message it has received.
   * @param msgRef The string ref. Use 
   * @return 
   */
  public Observable<Void> rejectObservable(String msgRef) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    reject(msgRef, result.toHandler());
    return result;
  }

  /**
   * Allows an application to release a message it has received.
   * @param msgRef The string ref. Use 
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService release(String msgRef, Handler<AsyncResult<Void>> result) { 
    this.delegate.release(msgRef, result);
    return this;
  }

  /**
   * Allows an application to release a message it has received.
   * @param msgRef The string ref. Use 
   * @return 
   */
  public Observable<Void> releaseObservable(String msgRef) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    release(msgRef, result.toHandler());
    return result;
  }

  /**
   * Allows a vertx.application to register a Service it provides with the
   * vertx-amqp-service. This allows any AMQP peer to interact with this
   * service by sending (and receiving) messages with the service.
   * @param eventbusAddress The event-bus address the service is listening for incoming requests. The application needs to register a handler with the event-bus using this address to receive the above requests.
   * @param notificationAddres The event-bus address to which notifications about the service is sent. The application should register a handler with the event-bus to receive these updates. Ex notifies the application of an incoming link created by an AMQP peer to send requests. Please see {@link io.vertx.ext.amqp.NotificationType} and {@link io.vertx.ext.amqp.NotificationHelper} for more details.
   * @param options Options to configure the Service behavior (Ex initial capacity). {@link io.vertx.ext.amqp.ServiceOptions}
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService registerService(String eventbusAddress, String notificationAddres, ServiceOptions options, Handler<AsyncResult<Void>> result) { 
    this.delegate.registerService(eventbusAddress, notificationAddres, options, result);
    return this;
  }

  /**
   * Allows a vertx.application to register a Service it provides with the
   * vertx-amqp-service. This allows any AMQP peer to interact with this
   * service by sending (and receiving) messages with the service.
   * @param eventbusAddress The event-bus address the service is listening for incoming requests. The application needs to register a handler with the event-bus using this address to receive the above requests.
   * @param notificationAddres The event-bus address to which notifications about the service is sent. The application should register a handler with the event-bus to receive these updates. Ex notifies the application of an incoming link created by an AMQP peer to send requests. Please see {@link io.vertx.ext.amqp.NotificationType} and {@link io.vertx.ext.amqp.NotificationHelper} for more details.
   * @param options Options to configure the Service behavior (Ex initial capacity). {@link io.vertx.ext.amqp.ServiceOptions}
   * @return 
   */
  public Observable<Void> registerServiceObservable(String eventbusAddress, String notificationAddres, ServiceOptions options) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    registerService(eventbusAddress, notificationAddres, options, result.toHandler());
    return result;
  }

  /**
   * Allows an application to unregister a service with vertx-amqp-service.
   * @param eventbusAddress The event-bus address used when registering the service
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService unregisterService(String eventbusAddress, Handler<AsyncResult<Void>> result) { 
    this.delegate.unregisterService(eventbusAddress, result);
    return this;
  }

  /**
   * Allows an application to unregister a service with vertx-amqp-service.
   * @param eventbusAddress The event-bus address used when registering the service
   * @return 
   */
  public Observable<Void> unregisterServiceObservable(String eventbusAddress) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    unregisterService(eventbusAddress, result.toHandler());
    return result;
  }

  /**
   * Allows the service to issue credits to a particular incoming link
   * (created by a remote AMQP peer) for sending more service requests. This
   * allows the Service to always be in control of how many messages it
   * receives so it can maintain the required QoS requirements.
   * @param linkId The ref for the incoming link. The service gets notified of an incoming link by registering for notifications. Please  and {@link io.vertx.ext.amqp.NotificationHelper} for more details.
   * @param credits The number of message (requests) the AMQP peer is allowed to send.
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  public AmqpService issueCredits(String linkId, int credits, Handler<AsyncResult<Void>> result) { 
    this.delegate.issueCredits(linkId, credits, result);
    return this;
  }

  /**
   * Allows the service to issue credits to a particular incoming link
   * (created by a remote AMQP peer) for sending more service requests. This
   * allows the Service to always be in control of how many messages it
   * receives so it can maintain the required QoS requirements.
   * @param linkId The ref for the incoming link. The service gets notified of an incoming link by registering for notifications. Please  and {@link io.vertx.ext.amqp.NotificationHelper#getLinkRef} for more details.
   * @param credits The number of message (requests) the AMQP peer is allowed to send.
   * @return 
   */
  public Observable<Void> issueCreditsObservable(String linkId, int credits) { 
    io.vertx.rx.java.ObservableFuture<Void> result = io.vertx.rx.java.RxHelper.observableFuture();
    issueCredits(linkId, credits, result.toHandler());
    return result;
  }

  /**
   * Adds an entry to the inbound routing table. If an existing entry exists
   * under the same pattern, the event-bus address will be added to the list.
   * @param pattern The pattern to be matched against the chosen message-property from the incoming message.
   * @param eventBusAddress The Vert.x event-bus address the message should be sent to if matched.
   * @return A reference to the service.
   */
  public AmqpService addInboundRoute(String pattern, String eventBusAddress) { 
    this.delegate.addInboundRoute(pattern, eventBusAddress);
    return this;
  }

  /**
   * Removes the entry from the inbound routing table.
   * @param pattern The pattern (key) used when adding the entry to the table.
   * @param eventBusAddress The Vert.x event-bus address the message should be sent to if matched.
   * @return 
   */
  public AmqpService removeInboundRoute(String pattern, String eventBusAddress) { 
    this.delegate.removeInboundRoute(pattern, eventBusAddress);
    return this;
  }

  /**
   * Adds an entry to the outbound routing table. If an existing entry exists
   * under the same pattern, the amqp address will be added to the list.
   * @param pattern The pattern to be matched against the chosen message-property from the outgoing message.
   * @param amqpAddress The AMQP address the message should be sent to if matched.
   * @return A reference to the service.
   */
  public AmqpService addOutboundRoute(String pattern, String amqpAddress) { 
    this.delegate.addOutboundRoute(pattern, amqpAddress);
    return this;
  }

  /**
   * Removes the entry from the outbound routing table.
   * @param pattern The pattern (key) used when adding the entry to the table.
   * @param amqpAddress The AMQP address the message should be sent to if matched.
   * @return 
   */
  public AmqpService removeOutboundRoute(String pattern, String amqpAddress) { 
    this.delegate.removeOutboundRoute(pattern, amqpAddress);
    return this;
  }

  /**
   * Start the vertx-amqp-service
   */
  public void start() { 
    this.delegate.start();
  }

  /**
   * Stop the vertx-amqp-service
   */
  public void stop() { 
    this.delegate.stop();
  }


  public static AmqpService newInstance(io.vertx.ext.amqp.AmqpService arg) {
    return new AmqpService(arg);
  }
}
