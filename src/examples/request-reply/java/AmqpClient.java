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

public class AmqpClient
{
    public static void main(String[] args) throws Exception
    {
        Messenger mng = Proton.messenger();
        mng.start();
        mng.subscribe("amqp://~localhost:5672/my-reply-dest");

        Message requestMsg = Proton.message();
        requestMsg.setBody(new AmqpValue("rajith"));
        requestMsg.setAddress("amqp://localhost:5673/hello-service");
        requestMsg.setReplyTo("amqp://localhost:5672/my-reply-dest");
        mng.put(requestMsg);
        mng.send();
        System.out.println("AMQP client sent request : rajith");

        mng.recv(1);
        while (mng.incoming() > 0)
        {
            Message m = mng.get();
            System.out.println("AMQP client received response : " + ((AmqpValue) m.getBody()).getValue());
        }
        mng.stop();
    }
}