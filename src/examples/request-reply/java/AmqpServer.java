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

public class AmqpServer
{
    public static void main(String[] args) throws Exception
    {
        Messenger mng = Proton.messenger();
        mng.start();
        mng.subscribe("amqp://~localhost:5672/hello-service-amqp");

        while (true)
        {
            mng.recv(1);
            while (mng.incoming() > 0)
            {
                Message requestMsg = mng.get();
                mng.accept(mng.incomingTracker(), 0);
                String body = (String) ((AmqpValue) requestMsg.getBody()).getValue();
                System.out.println("AMQP server received request : " + body);

                Message replyMsg = Proton.message();
                replyMsg.setBody(new AmqpValue("HELLO " + body.toUpperCase()));
                replyMsg.setAddress(requestMsg.getReplyTo());
                mng.put(replyMsg);
                mng.send();
                System.out.println("AMQP server sent response : " + body.toUpperCase());
            }
        }
    }
}