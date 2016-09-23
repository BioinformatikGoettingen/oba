package de.sybig.oba.server;

import java.util.jar.Attributes;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

/**
 *
 * @author juergen.doenitz@biologie.uni-gettingen.de
 */
public class RestServerTest {

    private static RestServer server;

    @BeforeClass
    public static void setUpBeforeClass() {
        server = new RestServer();
    }

    @Test
    public void testMissingNameInPlugin() {
        Attributes entries = new Attributes();
        String name = server.getIdentifierFromPlugin(entries);
        Assert.assertNull(name);
    }

    @Test
    public void testOldNamePatternInPlugin() {
        TestLogger logger = TestLoggerFactory.getTestLogger(RestServer.class);
        Attributes entries = new Attributes();
        entries.put(new Attributes.Name("function-path-name"), "value");
        String name = server.getIdentifierFromPlugin(entries);
        Assert.assertEquals(1, logger.getAllLoggingEvents().size());
        Assert.assertEquals("value", name);
        logger.clear();
    }
}
