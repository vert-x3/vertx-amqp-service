#!/usr/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

import optparse
from proton import Message
from proton.handlers import MessagingHandler, Handler
from proton.reactor import Container
from random import randint
from time import time

class TaskHandler(Handler):
    def __init__(self, receiver, credit):	
        self.receiver = receiver
        self.credit = credit 
    def on_timer_task(self, event):
        self.receiver.flow(self.credit)     

class FortuneCookieService(MessagingHandler):
    def __init__(self, url):
        super(FortuneCookieService, self).__init__(prefetch=0)
        self.url = url
        with open("fortune-cookie.txt") as f:
    	   self.cookies = f.readlines()
           self.upper = len(self.cookies)-1  

    def on_start(self, event):
        self.container = event.container
        self.acceptor = event.container.listen(self.url)

    def on_link_opened(self, event):
	if(event.link.is_receiver):
	   event.receiver.flow(1)

    def on_message(self, event):
        request = event.message.body  
	print "Hello-Service-AMQP received request [%s]" % request
 	print "reply-to %s" % event.message.reply_to
        self.accept(event.delivery)
        sender = self.container.create_sender(event.message.reply_to)
        greeting = 'HELLO ' + request.upper()
        delivery = sender.send(Message(body=unicode(greeting)))
	delivery.context = event.link

    def on_accepted(self, event):
        event.delivery.link.close()
        event.delivery.context.flow(1)

parser = optparse.OptionParser(usage="usage: %prog",
                               description="Appends Hello to the request and sends the response in uppercase")
parser.add_option("-a","--address", default="localhost:5672/hello-service-amqp",
                  help="address for listening on client requests (default %default)")
opts, args = parser.parse_args()

Container(FortuneCookieService(opts.address)).run()
