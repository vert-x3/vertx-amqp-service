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
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

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
}
