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

public class AmqpPub
{
    public static void main(String[] args) throws Exception
    {
        Messenger mng = Proton.messenger();
        mng.start();

        Message msg = Proton.message();
        msg.setBody(new AmqpValue("hello world from foo bar"));
        msg.setAddress("amqp://localhost:5673/foo.bar");
        mng.put(msg);
        mng.send();
        System.out.println("AMQP publisher sent message 'hello world from foo bar'");
        
        msg.setBody(new AmqpValue("hello world from foo baz"));
        msg.setAddress("amqp://localhost:5673/foo.baz");
        mng.put(msg);
        mng.send();
        System.out.println("AMQP publisher sent message 'hello world from foo baz'");
        mng.stop();
    }
}