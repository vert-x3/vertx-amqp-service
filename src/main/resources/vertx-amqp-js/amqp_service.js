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

/** @module vertx-amqp-js/amqp_service */
var utils = require('vertx-js/util/utils');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JAmqpService = io.vertx.ext.amqp.AmqpService;
var IncomingLinkOptions = io.vertx.ext.amqp.IncomingLinkOptions;
var OutgoingLinkOptions = io.vertx.ext.amqp.OutgoingLinkOptions;
var ServiceOptions = io.vertx.ext.amqp.ServiceOptions;

/**
 AMQP service allows a Vert.x application to,
 <ul>
 <li>Establish and cancel incoming/outgoing AMQP links, and map the link it to
 an event-bus address.</li>
 <li>Configure the link behavior</li>
 <li>Control the flow of messages both incoming and outgoing to maintain QoS</li>
 <li>Send and Receive messages from AMQP peers with different reliability
 guarantees</li>
 </ul>
 
 For more information on AMQP visit www.amqp.org This service speaks AMQP 1.0
 and use QPid Proton(http://qpid.apache.org/proton) for protocol support.
 
 @class
*/
var AmqpService = function(j_val) {

  var j_amqpService = j_val;
  var that = this;

  /**
   Allows an application to establish a link to an AMQP message-source for
   receiving messages. The service will receive the messages on behalf of
   the application and forward it to the event-bus address specified in the
   consume method. The application will be listening on this address.

   @public
   @param amqpAddress {string} A link will be created to the the AMQP message-source identified by this address. . 
   @param eventbusAddress {string} The event-bus address to be mapped to the above link. The application should register a handler for this address on the event bus to receive the messages. 
   @param notificationAddress {string} The event-bus address to which notifications about the incoming link is sent. Ex. Errors. The application should register a handler with the event-bus to receive these updates. 
   @param options {Object} Options to configure the link behavior (Ex prefetch, reliability). {@link IncommingLinkOptions} 
   @param result {function} The AsyncResult contains a ref (string) to the mapping created. This is required when changing behavior or canceling the link and it' association. 
   @return {AmqpService} A reference to the service.
   */
  this.establishIncommingLink = function(amqpAddress, eventbusAddress, notificationAddress, options, result) {
    var __args = arguments;
    if (__args.length === 5 && typeof __args[0] === 'string' && typeof __args[1] === 'string' && typeof __args[2] === 'string' && typeof __args[3] === 'object' && typeof __args[4] === 'function') {
      j_amqpService.establishIncommingLink(amqpAddress, eventbusAddress, notificationAddress, options != null ? new IncomingLinkOptions(new JsonObject(JSON.stringify(options))) : null, function(ar) {
      if (ar.succeeded()) {
        result(ar.result(), null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   If prefetch was set to zero, this method allows the application to
   explicitly fetch a certain number of messages. If prefetch > 0, the AMQP
   service will prefetch messages for you automatically.

   @public
   @param incomingLinkRef {string} The String ref return by the establishIncommingLink method. This uniquely identifies the incoming link and it's mapping to an event-bus address. 
   @param messages {number} The number of message to fetch. 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.fetch = function(incomingLinkRef, messages, result) {
    var __args = arguments;
    if (__args.length === 3 && typeof __args[0] === 'string' && typeof __args[1] ==='number' && typeof __args[2] === 'function') {
      j_amqpService.fetch(incomingLinkRef, messages, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Allows an application to cancel an incoming link and remove it's mapping
   to an event-bus address.

   @public
   @param incomingLinkRef {string} The String ref return by the establishIncommingLink method. This uniquely identifies the incoming link and it's mapping to an event-bus address. 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.cancelIncommingLink = function(incomingLinkRef, result) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'string' && typeof __args[1] === 'function') {
      j_amqpService.cancelIncommingLink(incomingLinkRef, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Allows an application to establish a link to an AMQP message-sink for
   sending messages. The application will send the messages to the event-bus
   address. The AMQP service will receive these messages via the event-bus
   and forward it to the respective AMQP message sink.

   @public
   @param amqpAddress {string} A link will be created to the the AMQP message-sink identified by this address. 
   @param eventbusAddress {string} The event-bus address to be mapped to the above link. The application should send the messages using this address. 
   @param notificationAddress {string} The event-bus address to which notifications about the outgoing link is sent. Ex. Errors, Delivery Status, credit availability. The application should register a handler with the event-bus to receive these updates. 
   @param options {Object} Options to configure the link behavior (Ex reliability). {@link IncommingLinkOptions} 
   @param result {function} The AsyncResult contains a ref (string) to the mapping created. This is required when changing behavior or canceling the link and it' association. 
   @return {AmqpService} A reference to the service.
   */
  this.establishOutgoingLink = function(amqpAddress, eventbusAddress, notificationAddress, options, result) {
    var __args = arguments;
    if (__args.length === 5 && typeof __args[0] === 'string' && typeof __args[1] === 'string' && typeof __args[2] === 'string' && typeof __args[3] === 'object' && typeof __args[4] === 'function') {
      j_amqpService.establishOutgoingLink(amqpAddress, eventbusAddress, notificationAddress, options != null ? new OutgoingLinkOptions(new JsonObject(JSON.stringify(options))) : null, function(ar) {
      if (ar.succeeded()) {
        result(ar.result(), null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Allows an application to cancel an outgoing link and remove it's mapping
   to an event-bus address.

   @public
   @param outgoingLinkRef {string} The String ref return by the establishOutgoingLink method. This uniquely identifies the outgoing link and it's mapping to an event-bus address. 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.cancelOutgoingLink = function(outgoingLinkRef, result) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'string' && typeof __args[1] === 'function') {
      j_amqpService.cancelOutgoingLink(outgoingLinkRef, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Allows an application to accept a message it has received.

   @public
   @param msgRef {string} - The string ref. Use {@link AmqpMessage#getMsgRef()} 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.accept = function(msgRef, result) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'string' && typeof __args[1] === 'function') {
      j_amqpService.accept(msgRef, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Allows an application to reject a message it has received.

   @public
   @param msgRef {string} - The string ref. Use {@link AmqpMessage#getMsgRef()} 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.reject = function(msgRef, result) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'string' && typeof __args[1] === 'function') {
      j_amqpService.reject(msgRef, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Allows an application to release a message it has received.

   @public
   @param msgRef {string} - The string ref. Use {@link AmqpMessage#getMsgRef()} 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.release = function(msgRef, result) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'string' && typeof __args[1] === 'function') {
      j_amqpService.release(msgRef, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**

   @public
   @param eventbusAddress {string} 
   @param options {Object} 
   @param result {function} 
   @return {AmqpService}
   */
  this.registerService = function(eventbusAddress, options, result) {
    var __args = arguments;
    if (__args.length === 3 && typeof __args[0] === 'string' && typeof __args[1] === 'object' && typeof __args[2] === 'function') {
      j_amqpService.registerService(eventbusAddress, options != null ? new ServiceOptions(new JsonObject(JSON.stringify(options))) : null, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**

   @public
   @param eventbusAddress {string} 
   @param result {function} 
   @return {AmqpService}
   */
  this.unregisterService = function(eventbusAddress, result) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'string' && typeof __args[1] === 'function') {
      j_amqpService.unregisterService(eventbusAddress, function(ar) {
      if (ar.succeeded()) {
        result(null, null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Start the AMQP Service

   @public

   */
  this.start = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_amqpService.start();
    } else utils.invalidArgs();
  };

  /**
   Stop the AMQP Service

   @public

   */
  this.stop = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_amqpService.stop();
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_amqpService;
};

/**

 @memberof module:vertx-amqp-js/amqp_service
 @param vertx {Vertx} 
 @param address {string} 
 @return {AmqpService}
 */
AmqpService.createEventBusProxy = function(vertx, address) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'string') {
    return new AmqpService(JAmqpService.createEventBusProxy(vertx._jdel, address));
  } else utils.invalidArgs();
};

// We export the Constructor function
module.exports = AmqpService;