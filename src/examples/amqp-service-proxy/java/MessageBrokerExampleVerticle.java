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
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.OutgoingLinkOptions;

/**
 * Demonstrates how to use the AmqpService interface to send and receive from a
 * traditional message broker.
 * 
 * This example assumes an ActiveMQ broker (or another AMQP 1.0 broker) is
 * running at localhost:6672
 * 
 * 1. Create a vert.x consumer for address 'my-sub-queue'. <br>
 * 2. Setup an incoming-link to 'amqp://localhost:6672/my-queue' and map it to
 * 'my-sub-queue' using the Service API.<br>
 * 3. Setup an outgoing-link to 'amqp://localhost:6672/my-queue' and map it to
 * vert.x address 'my-pub-queue'.<br>
 * 4. Send a message to vert.x address 'my-pub-queue'. <br>
 * 5. The message should be received by the consumer created for 'my-sub-queue'.
 * 6. Accept the message. 7. Cancel the incoming and outgoing link mappings.
 * 
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 *
 */
public class MessageBrokerExampleVerticle extends AbstractVerticle
{
    private String incomingLinkRef = null;

    private String outgoingLinkRef = null;

    @Override
    public void start() throws Exception
    {
        final AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");

        // 1. Create a vert.x consumer for address 'my-sub-queue'
        System.out.println("Creating a vertx consumer for my-sub-queue");
        vertx.eventBus().consumer("my-sub-queue", message -> {
            // 5. The message should be received by the consumer created for
            // 'my-sub-queue'.
            JsonObject msg = (JsonObject) message.body();
            System.out.println("Received a message: " + msg);

            // 6. Accept the message.
            service.accept(msg.getString("vertx.msg-ref"), result -> {
                System.out.println("Handler accepting the message : " + result.failed());
                System.out.println("Handler (cause) accepting the message : " + result.cause());
                if (result.failed())
                {
                    System.out.println("Error accepting the message : " + result.cause());
                    result.cause().printStackTrace();
                }
                // 7. Cancel the incoming and outgoing link mappings.
                service.cancelIncommingLink(incomingLinkRef, r -> {
                });
                service.cancelOutgoingLink(outgoingLinkRef, r -> {
                });
                // Note we are not waiting for the results of step #7.
            });
        });

        // 2. Setup an incoming-link to 'amqp://localhost:6672/my-queue' and map
        // it to 'my-sub-queue' using the Service API.
        System.out
        .println("Attempting to establish an incoming link from 'amqp://localhost:6672/my-queue' to the bridge");
        service.establishIncommingLink("amqp://localhost:6672/my-queue", "my-sub-queue", "my-sub-notifications",
                new IncomingLinkOptions(), result -> {
                    if (result.succeeded())
                    {
                        incomingLinkRef = result.result();
                        System.out.println("Incoming link ref : " + incomingLinkRef);
                        // Incoming link was successfully established. Now setup
                        // outgoing link and send message.
                        sendMessage(service);
                    }
                    else
                    {
                        System.out.println("Error occured while setting up incoming link from AMQP peer to application: "
                                + result.cause());
                        result.cause().printStackTrace();
                        System.exit(-1);
                    }
                });
    }

    private void sendMessage(final AmqpService service)
    {
        // 3. Setup an outgoing-link to 'amqp://localhost:6672/my-queue' and map
        // it to vert.x address 'my-pub-queue'
        System.out
        .println("Attempting to establish an outgoing link from the bridge to 'amqp://localhost:5672/my-queue'");
        service.establishOutgoingLink("amqp://localhost:6672/my-queue", "my-pub-queue", "my-pub-notifications",
                new OutgoingLinkOptions(), result -> {
                    if (result.succeeded())
                    {
                        outgoingLinkRef = result.result();
                        System.out.println("Outgoing link ref : " + outgoingLinkRef);

                        // 4. Send a message to vert.x address 'my-pub-queue'
                        System.out.println("Sending a message to vertx address my-pub-queue");
                        vertx.eventBus().publish("my-pub-queue", new JsonObject().put("body", "rajith"));
                    }
                    else
                    {
                        System.out.println("Error occured while setting up outgoing link from application to AMQP peer: "
                                + result.cause());
                        result.cause().printStackTrace();
                    }
                });
    }
}