package de.sybig.oba.server;

import java.io.File;
import java.util.jar.Attributes;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

/**
 *
 * @author juergen.doenitz@biologie.uni-gettingen.de
 */
public class RestServerTest {

    TestLogger logger = TestLoggerFactory.getTestLogger(RestServer.class);
    private static RestServer server;

    @BeforeClass
    public static void setUpBeforeClass() {
        server = new RestServer();
    }

    //loadPlugins
    @Test
    public void testLoadPluginsFromMissingPluginDir() {
        RestServer mocked_server = mock(RestServer.class);
        when(mocked_server.getBaseDir()).thenReturn(new File(System.getProperty("java.io.tmpdir")));
        doCallRealMethod().when(mocked_server).loadPlugins();
        mocked_server.loadPlugins();
        Assert.assertEquals(1, logger.getLoggingEvents().size());
        logger.clear();
    }

    //getIdentifierFromPlugin
    @Test
    public void testMissingNameInPlugin() {
        Attributes entries = new Attributes();
        String name = server.getIdentifierFromPlugin(entries);
        Assert.assertNull(name);
    }

    @Test
    public void testOldNamePatternInPlugin() {

        Attributes entries = new Attributes();
        entries.put(new Attributes.Name("function-path-name"), "value");
        String name = server.getIdentifierFromPlugin(entries);
        Assert.assertEquals(1, logger.getLoggingEvents().size());
        Assert.assertEquals("value", name);
        logger.clear();
    }
}
