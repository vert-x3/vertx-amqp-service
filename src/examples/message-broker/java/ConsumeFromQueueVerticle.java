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
import io.vertx.ext.amqp.AMQPService;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;

/**
 * Demonstrates how to use the AmqpService interface for consuming from a queue
 * within a message broker.
 * <p/>
 * This example assumes an ActiveMQ broker (or another AMQP 1.0 broker) is
 * running at localhost:6672
 * <p/>
 * 1. Create a vert.x consumer for address 'my-sub-queue'. <br>
 * 2. Setup an incoming-link to 'amqp://localhost:6672/my-queue' and map it to
 * 'my-sub-queue' using the Service API.<br>
 * 3. Run PublishToQueueVerticle or use some other means to send a message to the above queue. <br>
 * 4. The message should be received by the consumer created for 'my-sub-queue'.<br>
 * 5. Accept the message. <br>
 * 6. Cancel the incoming link mapping.
 *
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 */
public class ConsumeFromQueueVerticle extends AbstractVerticle {
  private String incomingLinkRef = null;

  @Override
  public void start() throws Exception {
    final AMQPService service = AMQPService.createEventBusProxy(vertx, "vertx.service-amqp");

    // 1. Create a vert.x consumer for address 'my-sub-queue'
    System.out.println("Creating a vertx consumer for my-sub-queue");
    vertx.eventBus().consumer("my-sub-queue", message -> {
      // 4. The message should be received by the consumer created for
      // 'my-sub-queue'.
      JsonObject msg = (JsonObject) message.body();
      System.out.println("Received a message: " + msg);

      // 5. Accept the message.
      System.out.println("Accepting message: " + msg.getString(AMQPService.INCOMING_MSG_REF));
      service.accept(msg.getString(AMQPService.INCOMING_MSG_REF), result -> {
        if (result.failed()) {
          System.out.println("Error accepting the message : " + result.cause());
          result.cause().printStackTrace();
        }
        // 6. Cancel the incoming and outgoing link mappings.
        service.cancelIncomingLink(incomingLinkRef, r -> {
        });
        // Note we are not waiting for the results of step #6.
      });
    });

    // 2. Setup an incoming-link to 'amqp://localhost:6672/my-queue' and map
    // it to 'my-sub-queue' using the Service API.
    IncomingLinkOptions options = new IncomingLinkOptions();
    options.setReliability(ReliabilityMode.AT_LEAST_ONCE);
    options.setPrefetch(10);
    System.out
      .println("Attempting to establish an incoming link from 'amqp://localhost:6672/my-queue' to the bridge");
    service.establishIncomingLink("amqp://localhost:6672/my-queue", "my-sub-queue", "my-sub-notifications",
      options, result -> {
        if (result.succeeded()) {
          incomingLinkRef = result.result();
          System.out.println("Incoming link ref : " + incomingLinkRef);
          // Incoming link was successfully established. Now setup
          // outgoing link and send message.
        } else {
          System.out.println("Error occured while setting up incoming link from AMQP peer to application: "
            + result.cause());
          result.cause().printStackTrace();
          System.exit(-1);
        }
      });
  }
}