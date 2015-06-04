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
import uuid
from proton import Message
from proton.handlers import MessagingHandler
from proton.reactor import Container, DynamicNodeProperties

class FortuneCookieClient(MessagingHandler):
    def __init__(self, service_addr, resp_addr):
        super(FortuneCookieClient, self).__init__()
        self.service_addr = service_addr
        self.reply_to = resp_addr + '/' + str(uuid.uuid1())

    def on_start(self, event):
        self.sender = event.container.create_sender(self.service_addr)
        self.receiver = event.container.create_receiver(self.reply_to) 
        self.receiver.flow(1)        
 
    def on_sendable(self, event):
        print "\n===================================="
	print "fortune-cookie-service has granted a single request credit"
        event.sender.send(Message(reply_to=self.reply_to));
        print "Sent a request for a fortune cookie"

    def on_accept(self, event):
	print "fortune-cookie-service has received my request and has accepted it"

    def on_message(self, event):
        print "Received my fortune cookie : '%s'" % event.message.body
        self.accept(event.delivery)
        print "Accepted the cookie"
        print "====================================\n" 

parser = optparse.OptionParser(usage="usage: %prog [options]",
                               description="Send requests to the supplied address and print responses.")
parser.add_option("--service_addr", default="localhost:5673/fortune-cookie-service",
                  help="AMQP address for fortune-cookie-service(default %default)")
parser.add_option("--response_addr", default="localhost:5673",
                  help="address to which responses are sent by the service (default %default)")
opts, args = parser.parse_args()

Container(FortuneCookieClient(opts.service_addr, opts.response_addr)).run()

