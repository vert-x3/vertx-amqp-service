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

/**
 AMQP service allows you to directly use API methods to subscribe, publish,
 issue credits and message acks without having to use control-messages via the
 event bus.
 
 @class
*/
var AmqpService = function(j_val) {

  var j_amqpService = j_val;
  var that = this;

  /**
   Allows an application to create a subscription to an AMQP message source.
   The service will receive the messages on behalf of the application and
   forward it to the event-bus address specified in the consume method. The
   application will be listening on this address.

   @public
   @param amqpAddress {string} The address that identifies the AMQP message source to subscribe from. 
   @param ebAddress {string} The event-bus address the application is listening on for the messages. 
   @param receiverMode {Object} Specified the reliability expected. 
   @param creditMode {Object} Specifies how credit is replenished. 
   @param result {function} AsyncResult that contains a String ref to the AMQP 'consumer', if successfully created. 
   @return {AmqpService} A reference to the service.
   */
  this.consume = function(amqpAddress, ebAddress, receiverMode, creditMode, result) {
    var __args = arguments;
    if (__args.length === 5 && typeof __args[0] === 'string' && typeof __args[1] === 'string' && typeof __args[2] === 'string' && typeof __args[3] === 'string' && typeof __args[4] === 'function') {
      j_amqpService.consume(amqpAddress, ebAddress, io.vertx.ext.amqp.ReceiverMode.valueOf(__args[2]), io.vertx.ext.amqp.CreditMode.valueOf(__args[3]), function(ar) {
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
   Allows an application to issue message credits for flow control purposes.

   @public
   @param consumerRef {string} The String ref return by the consume method. 
   @param credits {number} The message credits 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.issueCredit = function(consumerRef, credits, result) {
    var __args = arguments;
    if (__args.length === 3 && typeof __args[0] === 'string' && typeof __args[1] ==='number' && typeof __args[2] === 'function') {
      j_amqpService.issueCredit(consumerRef, credits, function(ar) {
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
   Allows an application to cancel a subscription it has previously created.

   @public
   @param consumerRef {string} The String ref return by the consume method. 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.unregisterConsume = function(consumerRef, result) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'string' && typeof __args[1] === 'function') {
      j_amqpService.unregisterConsume(consumerRef, function(ar) {
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
   Allows an application to acknowledge a message and set it's disposition.

   @public
   @param msgRef {string} - The string ref. Use {@link AmqpMessage#getMsgRef()} 
   @param disposition {Object} - One of ACCEPT, REJECT OR RELEASED. 
   @param result {function} Notifies if there is an error. 
   @return {AmqpService} A reference to the service.
   */
  this.acknowledge = function(msgRef, disposition, result) {
    var __args = arguments;
    if (__args.length === 3 && typeof __args[0] === 'string' && typeof __args[1] === 'string' && typeof __args[2] === 'function') {
      j_amqpService.acknowledge(msgRef, io.vertx.ext.amqp.MessageDisposition.valueOf(__args[1]), function(ar) {
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
   Allows an application to publish a message to an AMQP target.

   @public
   @param address {string} The AMQP target to which the messages should be sent. 
   @param msg {Object} The message to be sent. 
   @param result {function} A JsonObject containing the delivery state and disposition. 
   @return {AmqpService} 
   */
  this.publish = function(address, msg, result) {
    var __args = arguments;
    if (__args.length === 3 && typeof __args[0] === 'string' && typeof __args[1] === 'object' && typeof __args[2] === 'function') {
      j_amqpService.publish(address, utils.convParamJsonObject(msg), function(ar) {
      if (ar.succeeded()) {
        result(utils.convReturnJson(ar.result()), null);
      } else {
        result(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   Start the service

   @public

   */
  this.start = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_amqpService.start();
    } else utils.invalidArgs();
  };

  /**
   Stop the service

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
 @param config {Object} 
 @return {AmqpService}
 */
AmqpService.create = function(vertx, config) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'object') {
    return new AmqpService(JAmqpService.create(vertx._jdel, utils.convParamJsonObject(config)));
  } else utils.invalidArgs();
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