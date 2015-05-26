package io.vertx.ext.amqp;

import static io.vertx.ext.amqp.impl.util.Functions.extractDestination;
import junit.framework.TestCase;

public class AddressParsingTest extends TestCase
{
    public void testDestinationExtraction()
    {
        assertEquals("my-reply-dest", extractDestination("localhost:5673/my-reply-dest"));
        assertEquals("localhost:5673", extractDestination("localhost:5673"));
        assertEquals("", extractDestination("localhost:5673/"));
        assertEquals("my-dest", extractDestination("my-dest"));
    }
}
