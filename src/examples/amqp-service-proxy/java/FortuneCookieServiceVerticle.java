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
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.NotificationHelper;
import io.vertx.ext.amqp.ServiceOptions;
import io.vertx.ext.amqp.NotificationHelper.NotificationType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Demonstrates how to use the AmqpService interface to send/receive from a
 * traditional message broker.
 * 
 * This example assumes an ActiveMQ broker (or another AMQP 1.0 broker) is
 * running at localhost:5672
 * 
 * 1. First create a Vertx consumer. <br>
 * 2. Then you create an AMQP subscription via the Service interface and tie it
 * to the Vertx consumer.<br>
 * 3. Set message credits for AMQP consumer.<br>
 * 4. Send a message to the AMQP peer which forwards the message the AMQP
 * subscription, which in turn forwards it to the vertx consumer via the event
 * bus. <br>
 * 5. Acknowledge the message.
 * 
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 *
 */
public class FortuneCookieServiceVerticle extends AbstractVerticle
{
    final Map<Integer, String> fortuneCookies = new HashMap<Integer, String>();

    final Random random = new Random();

    int bound = 0;

    AmqpService service;

    String serviceAddress = "fortune-cookie-service";

    String noticeAddress = UUID.randomUUID().toString();

    int count = 0;
    
    @Override
    public void start() throws Exception
    {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(
                "/fortune-cookie.txt"))))
        {
            for (String line; (line = br.readLine()) != null;)
            {
                fortuneCookies.put(++bound, line);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error setting up FortuneCookieServiceVerticle");
        }

        service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");

        ServiceOptions options = new ServiceOptions();
        options.setInitialCapacity(1);
        service.registerService(
                serviceAddress,
                noticeAddress,
                options,
                result -> {
                    if (result.succeeded())
                    {
                        print("Service registered succesfully with the vertx-amqp-bridge using address : '%s'",
                                serviceAddress);
                    }
                    else
                    {
                        print("Unable to register service");
                    }
                });

        vertx.eventBus().<JsonObject> consumer(serviceAddress, msg -> {
            print("Received a request for a fortune-cookie from client");
            service.accept(msg.body().getString(AmqpService.INCOMING_MSG_REF), result -> {
            });
            
            JsonObject out = new JsonObject();
            out.put(AmqpService.OUTGOING_MSG_REF, "msg-ref".concat(String.valueOf(count++)));
            out.put("body", fortuneCookies.get(random.nextInt(bound)));
            msg.reply(out);
            vertx.setTimer(30 * 1000, timer -> {
                print("Issueing 1 request-credit");
                service.issueCredits(serviceAddress, 1, result -> {
                });
            });
        });

        vertx.eventBus()
                .<JsonObject> consumer(
                        noticeAddress,
                        msg -> {
                            // print("Notification msg %s", msg.body());
                            NotificationType type = NotificationHelper.getType(msg.body());
                            if (type == NotificationType.DELIVERY_STATE)
                            {
                                print("The the fortune-cookie is acknowledged by the client. Issuing another request credit after a 30s delay",
                                        NotificationHelper.getDeliveryTracker(msg.body()).getMessageState());
                                vertx.setTimer(30 * 1000, timer -> {
                                    print("Issueing 1 request-credit");
                                    service.issueCredits(serviceAddress, 1, result -> {
                                    });
                                });
                            }
                        });
    }

    private void print(String format, Object... args)
    {
        System.out.println(String.format(format, args));
    }
}