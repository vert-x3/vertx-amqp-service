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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

        vertx.eventBus().consumer("fortune-cookie-service", msg->
        {
            
        });
    }
}