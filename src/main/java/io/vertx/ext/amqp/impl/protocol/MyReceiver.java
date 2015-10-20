package io.vertx.ext.amqp.impl.protocol;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.ReliabilityMode;
import io.vertx.ext.amqp.impl.CreditMode;
import io.vertx.ext.amqp.impl.util.LogManager;


public class MyReceiver extends AbstractAmqpEventListener 
{
    private static final LogManager LOG = LogManager.get("MyReceiver:", MyReceiver.class);
    private IncomingLinkImpl link;
    private ManagedConnection connection;
    
    public MyReceiver(String host, int port, String dest) throws MessagingException
    {
        NetClient netClient = Vertx.factory.vertx().createNetClient(new NetClientOptions());
        DefaultConnectionSettings settings = new DefaultConnectionSettings();
        settings.setHost(host);
        settings.setPort(port);
        connection = new ManagedConnection(settings, this, false);
        netClient.connect(settings.getPort(), settings.getHost(), result -> {
            if (result.succeeded())
            {
                connection.setNetSocket(result.result());
                connection.write();
                connection.addDisconnectHandler(c -> {
                    LOG.warn("Connection lost to peer at  %s:%s", connection.getSettings().getHost(), connection
                        .getSettings().getPort());
                });
                LOG.info("Connected to AMQP peer at %s:%s", connection.getSettings().getHost(), connection
                        .getSettings().getPort());
            }
            else
            {
                LOG.warn("Error {%s}, when connecting to AMQP peer at %s:%s", result.cause(), connection.getSettings()
                        .getHost(), connection.getSettings().getPort());
            }
        });
        link = connection.createInboundLink(dest, ReliabilityMode.AT_LEAST_ONCE, CreditMode.AUTO);
        link.setCredits(10);
        
    }

    @Override
    public void onMessage(IncomingLinkImpl link, InboundMessage msg)
    {
        System.out.println("Received message : " + msg.getAddress() + " content : " + msg.getContent());
    }
    
    public static void main(String[] args) throws Exception
    {
        MyReceiver MyReceiver = new MyReceiver("localhost", 6672, "mydest");
    }
}
