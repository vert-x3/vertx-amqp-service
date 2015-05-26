/**
 * = Vert.x AMQP Service
 * :toc: left
 * 
 * Vert.x AMQP Service allows AMQP 1.0 applications and Vert.x applications to communicate with each other by acting as a bridge which
 * translates between the message formats and address spaces.
 * It facilitates reliable communication via message passing, while maintaining QoS by allowing control of the message flow in both directions.
 *
 * _Supported interaction patterns include *request-reply* and *publish-subscribe*._
 *
 * image::http://people.apache.org/~rajith/vert.x/vertx-amqp.jpeg[]
 * 
 * == Key Features:
 *
 * * Static and dynamic configuration for routing between the two address spaces.
 * * Communicate with different levels of reliability (unreliable, at-least-once).
 * * Flow control to main QoS service in both directions.
 * * Service API to allow Vert.x applications to communicate with the Vert.x-AMQP-Service.
 * * Existing Vert.x & AMQP applications could work together without extra code in bridge managed mode 
 *   (Reliability and flow control will be managed by the bridge. If explicit control is required the Vert.x application needs to use the Service interface).
 * 
 * 
 * == Features planned for future releases:
 * * Statistics generation.
 * * AMQP 1.0 management support to allow the management of the Vert.x-AMQP-Service (bridge).
 * * Hawt.io pluggin to allow the management of the Vert.x-AMQP-Service (bridge).
 * 
 * 
 * == Prerequisites.
 * This documentation assumes that you are familiar with Vert.x, basic messaging concepts & AMQP 1.0 concepts.
 * The AMQP examples require Apache QPid Proton installed.
 * 
 * * Vert.x https://vertx.ci.cloudbees.com/view/vert.x-3/job/vert.x3-website/ws/target/site/docs.html:[manual]
 * * AMQP https://amqp.org[AMQP Specification Group]
 * * Apache QPid Proton http://qpid.apache.org/proton[Install] | http://qpid.apache.org/proton[Python Manual]
 *
 * 
 * == Setting up the Service.
 * The first step is to deploy the AMQP Service (bridge) as a standalone service or programmatically.
 *
 * === Deploy it as a standalone service
 * Assuming the Vert.x AMQP Service jar and it's dependencies are located within the _lib directory_ of the vert.x installation
 * [source]
 * ----
 * vertx run service:io.vertx.vertx-amqp-service -cluster 
 * ----
 * [NOTE]
 * ====
 * If you run it standalone, you need to make sure the +++<u>cluster</u>+++ option is used. Please refer to the main vert.x manual on how to configure clustering.
 * ====
 * 
 * === Deploy it programmatically
 * [source,$lang]
 * ----
 * {@link examples.Examples#exampleDeployServiceVerticle}
 * ----
 * 
 * == Running your first example
 * Lets start with a simple request-reply example involving a Vert.x app and an AMQP app.
 * We will first run the examples before going through the concepts and the code.
 * 
 * === AMQP Client using a Vert.x Service
 * image::http://people.apache.org/~rajith/vert.x/vertx-service-amqp-client.jpeg[]
 * 
 * * Step 1. Start the Vert.x-AMQP-Service. +
 * [source] 
 * ----
 * vertx run service:io.vertx.vertx-amqp-service -cluster 
 * ----
 * [NOTE]
 * ====
 * Wait until you see the following message before you proceed with Step 2.
 * [source]
 * ----
 * AmqpService is now available via the address : vertx.service-amqp 
 * Succeeded in deploying verticle
 * ----
 * ====
 * 
 * * Step 2. Run the HelloServiceVerticle as follows. +
 * The above class is located in _src/main/examples/request-reply folder_ within the source tree.
 * [source]
 * ----
 * vertx run HelloServiceVerticle.java -cluster
 * ----
 * [NOTE]
 * ====
 * Wait until you see the following message before you proceed with Step 3.
 * [source]
 * ----
 * Succeeded in deploying verticle
 * ----
 * ====
 * 
 * * Step 3. Run the AMQP Client as follows.
 * [NOTE]
 * ====
 * * You need Apache QPid Proton installed and the PYTHON_PATH set properly before executing the AMQP examples.
 * See <<running_amqp_examples>> for more information.
 * * The scripts are located under src/amqp-examples.
 * * Use -h or --help to get list of all options.
 * ====
 * [source]
 * ----
 * ./client.py
 * ----
 *
 * If you plan to use a 3rd party intermediary for setting up the reply-to destination.
 * [source]
 * ----
 * ./client.py --response_addr <ip>:<port>/<dest-name>
 * ----
 * 
 * ==== How it all works
 * * If you take a closer look at the AMQP client and the Vert.x Service you would see that it is no different from an ordinary AMQP app or Vert.x app.
 * __i.e no extra code is required on either side for basic communication__
 * 
 * * The AMQP Client creates a request message with a reply-to address set and sends to the Vert.x-AMQP-Service.
 * [source,python]
 * ----
 * self.sender = event.container.create_sender(self.service_addr)
 * ...
 * event.sender.send(Message(reply_to=self.reply_to, body=request));
 * ----
 * * The Vert.x-AMQP-Service then translates the message into the json format and puts it into the Vert.x event-bus
 * * By default the AMQP Target is used as the event-bus address. You could configure a different mapping. See <<config_deployment_time>> for more details.
 * * The Vert.x Service (HelloServiceVerticle) listens on this address and receives this message.
 * [source,java]
 * ----
 * vertx.eventBus().consumer("hello-service-vertx", this);
 * ----
 * * Once received, it prepares the response (in this case appends hello to the request msg and uppercase the string) and replies on the message.
 * * The reply is received by the Vert.x-AMQP-Service which then forwards it to the AMQP client.
 * 
 * === Vert.x Client using an AMQP Service
 * image::http://people.apache.org/~rajith/vert.x/amqp-service-vertx-client.jpeg[]
 * 
 * * Step 1. Start the Vert.x-AMQP-Service. +
 * ** Start the Vert.x AMQP Service with the correct configuration. For this example some config is required.
 * ** The config required for this example is located in _src/main/examples/request-reply folder_ within the source tree.
 * [source] 
 * ----
 * vertx run service:io.vertx.vertx-amqp-service -conf ./request-reply.json -cluster 
 * ----
 * [NOTE]
 * ====
 * Wait until you see the following message before you proceed with Step 2.
 * [source]
 * ----
 * AmqpService is now available via the address : vertx.service-amqp 
 * Succeeded in deploying verticle
 * ----
 * ====
 *
 * * Step 2. Run the AMQP Service as follows.
 * [NOTE]
 * ====
 * * You need Apache QPid Proton installed and the PYTHON_PATH set properly before executing the AMQP examples.
 * See <<running_amqp_examples>> for more information.
 * * The scripts are located under src/amqp-examples.
 * * Use -h or --help to get list of all options.
 * ====
 * [source]
 * ----
 * ./hello-service.py
 * ---- 
 *  
 * * Step 3. Run the ClientVerticle as follows. +
 * The above class is located in _src/main/examples/request-reply folder_ within the source tree.
 * [source]
 * ----
 * vertx run ClientVerticle.java -cluster 
 * ----
 * ==== How it all works
 * * If you take a closer look at the AMQP Service and the Vert.x Client you would see that it is no different from an ordinary AMQP app or Vert.x app.
 * __i.e no extra code is required on either side for basic communication__. A little bit of configuration is required though.
 * 
 * * The Vert.x clients creates a request message and sends it to the Vert.x event-bus using 'hello-service-amqp' as the address. It also registers a reply-to handler.
 * [source, java]
 * ----
 * vertx.eventBus().send("hello-service-amqp", requestMsg, this);
 * ----
 * * The Vert.x-AMQP-Service is configured to listen on the Vert.x event-bus for any messages sent to 'hello-service-amqp' and then forward it to the correct AMQP endpoint. +
 *   The reply-to address in the AMQP message is set to point to the Vert.x-AMQP-Service and it keeps a mapping to the Vert.x reply-to.
 * [source, JSON]
 * ----
 * "vertx.handlers" : ["hello-service-amqp"]
 * "vertx.routing-outbound" : {
            "routes" :{
                     "hello-service-amqp" : "amqp://localhost:5672/hello-service-amqp"
                      }
             
         }
 * ----
 * * The AMQP Service receives the request, appends hello, upper case the string and sends it to reply-to address.
 * [source, python]
 * ----
 * sender = self.container.create_sender(event.message.reply_to)
 * greeting = 'HELLO ' + request.upper()
 * delivery = sender.send(Message(body=unicode(greeting)))
 * ----
 * * The Vert.x-AMQP-Service which receives the response, looks up the mapping and forwards it to the ClientVerticle via the event-bus.
 * 
 * [[config_deployment_time]]
 * == Configuring Vert.x AMQP Service.
 * 
 * [[running_amqp_examples]]
 * == Running the AMQP examples.
 * The AMQP examples require Apache QPid Proton installed.
 * 
 * === Setting up the env
 * For ease of use, the AMQP examples are written using the Proton Python API.
 * Use the links below to setup the environment.
 * * Apache QPid Proton http://qpid.apache.org/proton[Install]
 * * http://qpid.apache.org/proton[Python Manual]
 *  
 * === Using an AMQP intermediary (Ex. Message Broker or Router)
 * The examples are using the Vert.x-AMQP-Service (bridge) as an intermediary when required.
 * Ex. for setting up a temp destination for replies. 
 * But you could use a 3rd part AMQP service just as well.
 * * Apache QPid Dispatch Router http://qpid.apache.org/components/dispatch-router[Manual]
 * * Apache ActiveMQ http://activemq.apache.org[Website] | http://activemq.apache.org/amqp.html[AMQP config]
 * 
 * @author <a href="mailto:rajith@rajith.lk">Rajith Muditha Attapattu</a>
 */
@Document(fileName = "index.adoc")
@GenModule(name = "vertx-amqp")
package io.vertx.ext.amqp;

import io.vertx.codegen.annotations.GenModule;
import io.vertx.docgen.Document;

