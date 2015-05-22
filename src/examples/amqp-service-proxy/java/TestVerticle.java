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
import static io.vertx.ext.amqp.impl.util.Functions.print;
import io.vertx.core.AbstractVerticle;

/**
 * 
 * @author <a href="mailto:rajith77@gmail.com">Rajith Attapattu</a>
 *
 */
public class TestVerticle extends AbstractVerticle
{

    @Override
    public void start() throws Exception
    {

        vertx.setTimer(10 * 1000, timer -> {
            print("Issueing 1 request-credit");           
        });

    }
}