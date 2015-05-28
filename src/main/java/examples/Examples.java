/**
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
package examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.DeliveryTracker;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.NotificationHelper;
import io.vertx.ext.amqp.NotificationType;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;
import io.vertx.ext.amqp.ServiceOptions;

/**
 * Code snippets used for documentation.
 * 
 * Please note this class is NOT meant to be run as an example. Instead it's
 * used for illustration purposes within the documentation.
 * 
 * @author <a href="mailto:rajith@rajith.lk">Rajith Muditha Attapattu</a>
 *
 */
public class Examples extends AbstractVerticle
{
    private int counter;
    private String outgoingLinkRef = null;
    private String incomingLinkRef = null;
    private AmqpService service;
    private JsonObject msg;

    public void exampleDeployServiceVerticle()
    {
        JsonObject config = new JsonObject().put("amqp.inbound-host", "10.10.52.86");
        config.put("amqp.inbound-port", 5672);
        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle("service:io.vertx.vertx-amqp-service", options, res -> {
            if (res.succeeded())
            {
                // Deployed ok
            }
            else
            {
                // Failed to deploy
            }
        });
    }

    // ----- Service interface

    public void obtainingRefToServiceProxy()
    {
        AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");
    }

    // ------ Sending messages

    public void establishOutgoingLink()
    {
        AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");
        OutgoingLinkOptions options = new OutgoingLinkOptions();
        options.setReliability(ReliabilityMode.AT_LEAST_ONCE); //<4>
        service.establishOutgoingLink("amqp://localhost:6672/my-queue", // <1>
                "my-pub-queue", // <2>
                "my-pub-notifications", // <3>
                options, // <4>
                result -> {
                    if (result.succeeded())
                    {
                        outgoingLinkRef = result.result(); //<5>
                        // Link successfully established.
                    }
                    else
                    {
                        // handle error
                    }
                });

        //.....

        service.cancelOutgoingLink(outgoingLinkRef, result->{}); // <6>
    }

    public void sendingMessagesReliably()
    {
        JsonObject msg = new JsonObject();
        msg.put("body", "rajith");
        String msgRef = "msg-ref".concat(String.valueOf(counter++));
        msg.put(AmqpService.OUTGOING_MSG_REF, msgRef); // <1>
        vertx.eventBus().publish("my-pub-queue", msg); // <2>

        // ....

        vertx.eventBus().<JsonObject>consumer("my-pub-notifications", noticeMsg -> { // <3>
            NotificationType type = NotificationHelper.getType(noticeMsg.body()); // <4>
            if (type == NotificationType.DELIVERY_STATE)
            {
                DeliveryTracker tracker = NotificationHelper.getDeliveryTracker(noticeMsg.body());
                System.out.println("Delivery State : " + tracker.getMessageRef());  // <1>
                System.out.println("Delivery State : " + tracker.getDeliveryState()); // <5>
                System.out.println("Message State : " + tracker.getMessageState());   // <6>             
            }
        });
    }

    public void respectingFlowControlRequirements()
    {   
        vertx.eventBus().<JsonObject>consumer("my-pub-notifications", msg -> { // <1>
            NotificationType type = NotificationHelper.getType(msg.body()); // <2>
            if (type == NotificationType.LINK_CREDIT)
            {
                int msgsWeCanSend = NotificationHelper.getCredits(msg.body()); //<3>              
            }
        });
    }

    public void settingAMQPMessageProperties()
    {
        JsonObject msg = new JsonObject();
        msg.put("body", "rajith"); // <1>

        JsonObject properties = new JsonObject();
        msg.put("properties", properties); // <2>
        properties.put("subject", "<message-subject>"); // <3>
        properties.put("reply-to", "<reply-to-address>"); // <4>
        properties.put("message_id", "<message_id>"); // <5>
        properties.put("correlation_id", "<correlation_id>"); // <6>

        JsonObject appProps = new JsonObject();
        msg.put("application-properties", appProps); // <7>
        appProps.put("key_1", "val_1"); // <8>
        appProps.put("key_n", "val_n"); // <8>
    }

    // ---- Receive messages
    public void establishIncomingLink()
    {
        AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");
        IncomingLinkOptions options = new IncomingLinkOptions();
        options.setReliability(ReliabilityMode.AT_LEAST_ONCE); // <4>
        options.setPrefetch(10); // <5>
        service.establishIncomingLink("amqp://localhost:6672/my-queue", // <1>
                "my-sub-queue",  // <2>
                "my-sub-notifications", // <3>
                options,
                result -> {
                    if (result.succeeded())
                    {
                        incomingLinkRef = result.result(); // <6>
                    }
                    else
                    {
                        //handle error
                    }
                });

        //..... 

        service.cancelIncomingLink(incomingLinkRef, result->{}); // <7>
    }

    public void fetchingMessages()
    {
        service.fetch(incomingLinkRef, // <1>
                10, // <2>
                result -> {
                    if (result.succeeded())
                    {
                        // operation successfull.
                    }
                    else
                    {
                        //handle error
                    }
                });
    }

    public void receivingMessagesReliably()
    {
        service.accept(msg.getString(AmqpService.INCOMING_MSG_REF), result -> { // <1>
            if (result.failed())
            {
                // handle error
            }
        });
    }

    public void retrievingAMQPMessageProperties()
    {
        JsonObject msg = new JsonObject();
        msg.getJsonObject("body"); // <1>

        JsonObject properties = msg.getJsonObject("properties"); // <2>
        if (properties != null)
        {
            properties.getString("subject", "<message-subject>"); // <3>
            properties.getString("reply-to", "<reply-to-address>"); // <4>
            properties.getString("message_id", "<message_id>"); // <5>
            properties.getString("correlation_id", "<correlation_id>"); // <6>
        }
                
        JsonObject appProps = msg.getJsonObject("application-properties"); // <7>
        if (appProps != null)
        {
            // retrieve key value pairs.
        }
    }

    // ----- Services
    public void registerService()
    {
        AmqpService service = AmqpService.createEventBusProxy(vertx, "vertx.service-amqp");
        ServiceOptions options = new ServiceOptions();
        options.setInitialCapacity(1); // <3>
        service.registerService(
                "fortune-cookie-service", // <1>
                "notice-address", // <2>
                options, // <3>
                result -> {
                    if (result.succeeded())
                    {
                        // Service was registered successfully.
                    }
                    else
                    {
                        // handle error
                    }
                });

        //.....

        service.unregisterService("fortune-cookie-service", result -> {
            if (result.failed())
            {
                // error
            }
        });
    }

    public void manageClients()
    {   
        vertx.eventBus().<JsonObject>consumer("notice-address", msg -> { // <1>
            NotificationType type = NotificationHelper.getType(msg.body()); // <2>
            if (type == NotificationType.INCOMING_LINK_OPENED)
            {
                String linkRef = NotificationHelper.getLinkRef(msg.body()); // <3>
                service.issueCredits(linkRef, 1, result -> {   // <4>
                });
            }
        });        
    }    

}