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
import io.vertx.ext.amqp.DeliveryState;


public class Tracker {

  final io.vertx.ext.amqp.Tracker delegate;

  public Tracker(io.vertx.ext.amqp.Tracker delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  public DeliveryState getState() {
    DeliveryState ret = this.delegate.getState();
    return ret;
  }

  public MessageDisposition getDisposition() {
    MessageDisposition ret = this.delegate.getDisposition();
    return ret;
  }

  public boolean isSettled() {
    boolean ret = this.delegate.isSettled();
    return ret;
  }


  public static Tracker newInstance(io.vertx.ext.amqp.Tracker arg) {
    return new Tracker(arg);
  }
}
