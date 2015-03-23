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
 * 
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 *
 */
public class PublishToQueueVerticle extends AbstractVerticle
{
    @Override
    public void start() throws Exception
    {
        final AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");

        // 1. Setup an outgoing-link to 'amqp://localhost:6672/my-queue' and map
        // it to vert.x address 'my-pub-queue'
        System.out
        .println("Attempting to establish an outgoing link from the bridge to 'amqp://localhost:5672/my-queue'");
        service.establishOutgoingLink("amqp://localhost:6672/my-queue", "my-pub-queue", "my-pub-notifications",
                new OutgoingLinkOptions(), result -> {
                    if (result.succeeded())
                    {
                        String outgoingLinkRef = result.result();
                        System.out.println("Outgoing link ref : " + outgoingLinkRef);

                        // 2. Send a message to vert.x address 'my-pub-queue'
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