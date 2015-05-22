/**
 * = Vert.x AMQP Service
 * :toc: left
 * 
 * Vert.x AMQP Service allows AMQP applications and Vert.x applications to communicate with each other by acting as a bridge which
 * translates between the message formats and address spaces.
 * It allows this communication to happen reliably (if required), while maintaining QoS by allowing flow control of this interaction.
 *
 * _Supported interaction patterns include request-reply and publish-subscribe._
 *
 * http://people.apache.org/~rajith/vert.x/vertx-amqp.jpeg
 * 
 * == Some of the key features of Vert.x-AMQP-Service include:
 *
 * * Static and dynamic configuration for routing between the two address spaces.
 * * Communicate with different levels of reliability (unreliable, at-least-once).
 * * Flow control to main QoS service in both directions.
 * * Service interface to allow Vert.x applications to communicate with the Vert.x-AMQP-Service.
 * * Existing Vert.x & AMQP applications could work together without extra code in bridge managed mode 
 *   (Reliability and flow control will be managed by the bridge. If explicit control is required the Vert.x application needs to use the Service interface).
 * 
 * == Features planned for future releases include:
 * * Statistics generation.
 * * AMQP 1.0 management support to allow the management of the Vert.x-AMQP-Service (bridge).
 * * Hawt.io pluggin to allow the management of the Vert.x-AMQP-Service (bridge).
 *  
 *
 * == xxx
 *
 * In summary the Vert.x-AMQP-Service attempts to f
 * 
 *
 * [source,java]
 * ----
 * {@link examples.Examples#example1}
 * ----
 *
 * We create an HTTP server instance, and we set a request handler on it. The request handler will be called whenever
 * a request arrives on the server.
 *
 * When that happens we are just going to set the content type to `text/plain`, and write `Hello World!` and end the
 * response.
 *
 * We then tell the server to listen at port `8080` (default host is `localhost`).
 *
 * You can run this, and point your browser at `http://localhost:8080` to verify that it works as expected.
 * 
 * @author <a href="mailto:rajith@rajith.lk">Rajith Muditha Attapattu</a>
 */
@Document(fileName = "index.adoc")
@GenModule(name = "vertx-amqp")
package io.vertx.ext.amqp;

import io.vertx.codegen.annotations.GenModule;
import io.vertx.docgen.Document;

