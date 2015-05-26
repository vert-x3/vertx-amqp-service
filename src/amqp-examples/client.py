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

class Client(MessagingHandler):
    def __init__(self, service_addr, response_addr):
        super(Client, self).__init__()
        self.service_addr = service_addr
        self.response_addr = response_addr
        self.reply_to = response_addr

    def on_start(self, event):
        self.receiver = event.container.create_receiver(self.response_addr) 
        self.receiver.flow(1)        
        self.sender = event.container.create_sender(self.service_addr)

    def on_sendable(self, event):
        print "\n===================================="
	request = u'rajith muditha attapattu'
	event.sender.send(Message(reply_to=self.reply_to, body=request));
        print "Sent request '%s' to Vert.x Hello Service" % request

    def on_message(self, event):
        print "Received response : '%s'" % event.message.body
        print "====================================\n" 

parser = optparse.OptionParser(usage="usage: %prog [options]",
                               description="Send a request to the supplied address and print response.")
parser.add_option("--service_addr", default="localhost:5673/hello-service-vertx",
                  help="AMQP address which points to Vert.x-AMQP-Service. (default %default)")
parser.add_option("--response_addr", default="localhost:5673/my-reply-dest",
                  help="The reply-to address. By default the Vert.x-AMQP-Service is used as the intermediary to create a temp reply-to destination. You could use any 3rd party AMQP intermediary (default %default)")
opts, args = parser.parse_args()

Container(Client(opts.service_addr, opts.response_addr)).run()

