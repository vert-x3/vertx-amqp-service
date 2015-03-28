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
import io.vertx.ext.amqp.DeliveryTracker;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.NotificationHelper;
import io.vertx.ext.amqp.NotificationHelper.NotificationType;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;

/**
 * Demonstrates how to use the AmqpService interface for publishing to a queue
 * within a message broker.
 * 
 * This example assumes an ActiveMQ broker (or another AMQP 1.0 broker) is
 * running at localhost:6672
 * 
 * 1. Run ConsumeFromQueueVerticle or another Consumer app subscribing to my-queue on the above broker<br>
 * 2. Setup an outgoing-link to 'amqp://localhost:6672/my-queue' and map it to
 * vert.x address 'my-pub-queue'.<br>
 * 3. Send a message to vert.x address 'my-pub-queue'. <br>
 * 4. The message should be received by the consumer created for 'my-sub-queue'.<br>
 * 5. When the message is accepted by the broker, we cancel the outgoing link.
 * 
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 *
 */
public class PublishToQueueVerticle extends AbstractVerticle
{
    private String outgoingLinkRef = null;

    @Override
    public void start() throws Exception
    {
        final AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");
        registerForNotifications(service);

        // 1. Setup an outgoing-link to 'amqp://localhost:6672/my-queue' and map
        // it to vert.x address 'my-pub-queue'
        OutgoingLinkOptions options = new OutgoingLinkOptions();
        options.setReliability(ReliabilityMode.AT_LEAST_ONCE);
        
        System.out
        .println("Attempting to establish an outgoing link from the bridge to 'amqp://localhost:5672/my-queue'");
        service.establishOutgoingLink("amqp://localhost:6672/my-queue", "my-pub-queue", "my-pub-notifications",
                options, result -> {
                    if (result.succeeded())
                    {
                        outgoingLinkRef = result.result();
                        System.out.println("Outgoing link ref : " + outgoingLinkRef);
                        // 2. Send a message to vert.x address 'my-pub-queue'
                        // Use a unique id for outgoing msg ref. When a delivery
                        // notification is set, you could use this ref to
                        // correlate the tracker to the original message.
                        System.out.println("Sending a message to vertx address my-pub-queue");
                        vertx.eventBus().publish("my-pub-queue", new JsonObject().put("body", "rajith").put(AmqpService.OUTGOING_MSG_REF, "my-msg-ref"));
                    }
                    else
                    {
                        System.out.println("Error occured while setting up outgoing link from application to AMQP peer: "
                                + result.cause());
                        result.cause().printStackTrace();
                    }
                });
    }

    private void registerForNotifications(final AmqpService service)
    {
        vertx.eventBus().consumer("my-pub-notifications", msg -> {
            JsonObject json = (JsonObject)msg.body();
            NotificationType type = NotificationHelper.getType(json);
            if (type == NotificationType.DELIVERY_STATE)
            {
                DeliveryTracker tracker = NotificationHelper.getDeliveryTracker(json);
                System.out.println("Delivery State : " + tracker.getDeliveryState());
                System.out.println("Message State : " + tracker.getMessageState());
                // 5. When the message is accepted by the broker, we cancel the outgoing link.
                service.cancelOutgoingLink(outgoingLinkRef, result->{});
            }
            else if (type == NotificationType.LINK_CREDIT)
            {
                System.out.println("Received " + NotificationHelper.getCredits(json) + " message credits from broker");                
            }
        });
    }
}