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
import io.vertx.ext.amqp.NotificationHelper.NotificationType;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;

/**
 * Demonstrates how a vertx application could use the Service API to communicate
 * with an AMQP Service in a reliable way while respecting it's QoS requirements
 * (flow control in this case).
 * 
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 *
 */
public class FortuneCookieClientVerticle extends AbstractVerticle
{
    String serviceAddr = "amqp://localhost:7777/fortune-cookie-service";

    AmqpService service;

    @Override
    public void start() throws Exception
    {
        service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");

        OutgoingLinkOptions outOps = new OutgoingLinkOptions();
        outOps.setReliability(ReliabilityMode.AT_LEAST_ONCE);
        service.establishOutgoingLink(
                serviceAddr,
                "my-fortune-req",
                "notifications",
                outOps,
                result -> {
                    if (result.succeeded())
                    {
                        print("Outboundlink to AMQP Service '%s' established succesfully. Ref : '%s'", serviceAddr,
                                result.result());
                    }
                    else
                    {
                        print("Unable to setup outbgoing link btw amqp-vertx-bridge and the AMQP FortunueCookieService");
                    }
                });

        vertx.eventBus().<JsonObject> consumer(
                "notifications",
                msg -> {
                    // print("Notifiation msg %s", msg.body());
                    NotificationType type = NotificationHelper.getType(msg.body());
                    if (type == NotificationType.DELIVERY_STATE)
                    {
                        print("fortune-cookie-service has received my request and has %s it", NotificationHelper
                                .getDeliveryTracker(msg.body()).getMessageState());
                    }
                    else if (type == NotificationType.LINK_CREDIT && NotificationHelper.getCredits(msg.body()) > 0)
                    {
                        print("====================================");
                        print("fortune-cookie-service has granted a single request credit");
                        sendRequest();
                    }
                });
    }

    private void sendRequest()
    {
        print("Sent a request for a fortune cookie");
        vertx.eventBus().<JsonObject> send("my-fortune-req", new JsonObject(), resp -> {
            JsonObject msg = resp.result().body();
            print("Received my fortune cookie : '%s'", msg.getString("body"));
            service.accept(msg.getString(AmqpService.INCOMING_MSG_REF), result -> {
            });
            print("Accepted the cookie");
            print("====================================");
        });
    }

    private void print(String format, Object... args)
    {
        System.out.println(String.format(format, args));
    }
}