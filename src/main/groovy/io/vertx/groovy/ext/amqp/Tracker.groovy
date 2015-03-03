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

package io.vertx.groovy.ext.amqp;
import groovy.transform.CompileStatic
import io.vertx.lang.groovy.InternalHelper
import io.vertx.ext.amqp.MessageDisposition
import io.vertx.ext.amqp.DeliveryState
@CompileStatic
public class Tracker {
  final def io.vertx.ext.amqp.Tracker delegate;
  public Tracker(io.vertx.ext.amqp.Tracker delegate) {
    this.delegate = delegate;
  }
  public Object getDelegate() {
    return delegate;
  }
  public DeliveryState getState() {
    def ret = this.delegate.getState();
    return ret;
  }
  public MessageDisposition getDisposition() {
    def ret = this.delegate.getDisposition();
    return ret;
  }
  public boolean isSettled() {
    def ret = this.delegate.isSettled();
    return ret;
  }

  static final java.util.function.Function<io.vertx.ext.amqp.Tracker, Tracker> FACTORY = io.vertx.lang.groovy.Factories.createFactory() {
    io.vertx.ext.amqp.Tracker arg -> new Tracker(arg);
  };
}
