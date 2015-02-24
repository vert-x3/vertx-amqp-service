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
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class SubVerticle1 extends AbstractVerticle
{
    @Override
    public void start()
    {
        vertx.eventBus().consumer("foo-all", new Handler<Message<JsonObject>>()
        {
            @Override
            public void handle(Message<JsonObject> msg)
            {
                System.out.println("Subscriber verticle received msg : " + msg.body().encodePrettily());

            }
        });
    }
}