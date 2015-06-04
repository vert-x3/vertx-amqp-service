/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 */
public class FortuneCookieServiceVerticle extends AbstractVerticle {
  final Map<Integer, String> fortuneCookies = new HashMap<Integer, String>();

  final Random random = new Random();

  int bound = 0;

  AmqpService service;

  String serviceAddress = "fortune-cookie-service";

  String noticeAddress = UUID.randomUUID().toString();

  int count = 0;

  @Override
  public void start() throws Exception {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(
      "/fortune-cookie.txt")))) {
      for (String line; (line = br.readLine()) != null; ) {
        fortuneCookies.put(++bound, line);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error setting up FortuneCookieServiceVerticle");
    }

    service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");

    ServiceOptions options = new ServiceOptions();
    service.registerService(
      serviceAddress,
      noticeAddress,
      options,
      result -> {
        if (result.succeeded()) {
          print("Service registered succesfully with the vertx-amqp-bridge using address : '%s'",
            serviceAddress);
        } else {
          print("Unable to register service");
        }
      });

    vertx.eventBus().<JsonObject>consumer(serviceAddress, msg -> {
      JsonObject request = msg.body();
      // print(request.encodePrettily());
      String linkId = request.getString(AmqpService.INCOMING_MSG_LINK_REF);
      print("Received a request for a fortune-cookie from client [%s]", linkId);
      print("reply-to %s", msg.replyAddress());
      service.accept(request.getString(AmqpService.INCOMING_MSG_REF), result -> {
      });

      JsonObject response = new JsonObject();
      response.put(AmqpService.OUTGOING_MSG_REF, linkId);
      response.put("body", fortuneCookies.get(random.nextInt(bound)));
      msg.reply(response);
    });

    vertx.eventBus()
      .<JsonObject>consumer(
        noticeAddress,
        msg -> {
          NotificationType type = NotificationHelper.getType(msg.body());
          if (type == NotificationType.DELIVERY_STATE) {
            DeliveryTracker tracker = NotificationHelper.getDeliveryTracker(msg.body());
            print("The the fortune-cookie is acknowledged by the client. Issuing another request credit after a 30s delay");
            print("=============================================================\n");
            vertx.setTimer(30 * 1000, timer -> {
              service.issueCredits(tracker.getMessageRef(), 1, result -> {
              });
            });
          } else if (type == NotificationType.INCOMING_LINK_OPENED) {
            String linkRef = NotificationHelper.getLinkRef(msg.body());
            print("A client [%s] contacted the fortune-cookie service, issueing a single request-credit to start with",
              linkRef);
            print("=============================================================");
            service.issueCredits(linkRef, 1, result -> {
            });
          }
        });
  }

  public static void print(String format, Object... args) {
    System.out.println(String.format(format, args));
  }
}