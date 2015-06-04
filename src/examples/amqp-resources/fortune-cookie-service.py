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

from proton import Message
from proton.handlers import MessagingHandler
from proton.reactor import Container
from random import randint
from time import time
from threading import Timer

class FortuneCookieService(MessagingHandler):
    cookies = []
    upper = 0
    container = None
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
           print "A client [%s] contacted the fortune-cookie service, issueing a single request-credit to start with" % event.link.name
           print "\n=============================================================" 
	   event.receiver.flow(1)

    def on_message(self, event):
	print "Received a request for a fortune-cookie from client [%s]" % event.link.name
 	print "reply-to %s" % event.message.reply_to
        self.accept(event.delivery)
        sender = self.container.create_sender(event.message.reply_to)
        delivery = sender.send(Message(body=unicode(self.cookies[randint(0, self.upper)])))
	delivery.context = event.link

    def on_accepted(self, event):
        print "The the fortune-cookie is acknowledged by the client. Issuing another request credit after a 30s delay"
        print "=============================================================\n"    
        Timer(30, self.send_flow,[event.delivery.context, 1]).start()

    def send_flow(self, receiver, credits):
        print "\n=============================================================" 
        print "Issueing %s request-credit" % credits
	receiver.flow(credits)

    def on_link_closed(self, event):
        if(event.link.is_receiver):
           print "The client '%s' is no longer interested in fortune cookies", event.link.name

Container(FortuneCookieService("localhost:7777/fortune-cookie-service")).run()
