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
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.ReliabilityMode;

import java.util.concurrent.CountDownLatch;

/**
 * Demonstrate how to use the AmqpService interface
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
public class ServiceExampleVerticle extends AbstractVerticle
{
    @Override
    public void start() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(2);
        final AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");

        // Setup the vertx consumer.
        vertx.eventBus().consumer("my-vertx-address", message -> {
            JsonObject msg = (JsonObject) message.body();
            System.out.println("I have received a message: " + msg);

            // Acknowledge the message.
                service.acknowledge(msg.getString("vertx.msg-ref"), MessageDisposition.ACCEPTED, result -> {
                });
                latch.countDown();
            });

        // Setup the AMQP subscription and pass on the vertx consumer address
        final Consumer consumer = new ServiceExampleVerticle.Consumer();
        service.consume("amqp://localhost:5672/my-queue", "my-vertx-address", ReliabilityMode.AT_LEAST_ONCE,
                CreditMode.AUTO, result -> {
                    if (result.succeeded())
                    {
                        consumer.ref = result.result();
                    }
                    else
                    {
                        System.out.println("Error occured while creating AMQP subscription: " + result.cause());
                        System.exit(-1);
                    }
                });

        service.issueCredit(consumer.ref, 10, result -> {
            if (result.failed())
            {
                System.out.println("Error occured when setting credits for AMQP subscription: " + result.cause());
                System.exit(-1);
            }
        });

        // Send a message to the AMQP peer
        service.publish("amqp://localhost:5672/my-queue", new JsonObject().put("name", "rajith"), result -> {
            if (result.succeeded())
            {
                System.out.println("Delivery state " + result.result().encodePrettily());
            }
            else
            {
                System.out.println("Error occured while sending message: " + result.cause());
                System.exit(-1);
            }
            latch.countDown();
        });

        latch.await();
    }

    private class Consumer
    {
        String ref;
    }
}