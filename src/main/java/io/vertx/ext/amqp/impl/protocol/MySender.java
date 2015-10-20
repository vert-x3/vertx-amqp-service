package io.vertx.ext.amqp.impl.protocol;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.ext.amqp.MessageFormatException;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.ReliabilityMode;
import io.vertx.ext.amqp.impl.util.LogManager;


public class MySender extends AbstractAmqpEventListener 
{
    private static final LogManager LOG = LogManager.get("MySender:", MySender.class);
    private OutgoingLinkImpl link;
    private ManagedConnection connection;
    
    public MySender(String host, int port, String destination) throws MessagingException
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
        link = connection.createOutboundLink(destination, ReliabilityMode.AT_LEAST_ONCE);
        
    }

    public TrackerImpl send(AmqpMessage msg) throws MessageFormatException, MessagingException
    {
        return link.send(msg);
    }
    
    public static void main(String[] args) throws Exception
    {
        MySender sender = new MySender("localhost", 6672, "mydest");
        AmqpMessage msg = new AmqpMessageImpl();
        msg.setContent("hello-world");
        TrackerImpl t = sender.send(msg);
        t.awaitSettlement();
        
        System.out.println("Delivery state : " + t.getState());
    }
}
