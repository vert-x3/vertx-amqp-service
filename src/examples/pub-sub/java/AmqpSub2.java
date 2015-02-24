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

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

public class AmqpSub2
{
    public static void main(String[] args) throws Exception
    {
        Messenger mng = Proton.messenger();
        mng.start();
        mng.subscribe("amqp://localhost:5673/foo.bar.*");
        System.out.println("Subscribing to topic foo.bar.*");
        while (true)
        {
            mng.recv(1);
            while (mng.incoming() > 0)
            {
                Message m = mng.get();
                System.out.println("AMQP subscriber received message : " + ((AmqpValue) m.getBody()).getValue());
            }
        }
    }
}