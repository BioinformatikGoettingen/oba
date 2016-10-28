package de.sybig.oba.server;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class StorageHandlerTest {

    private static ObaOntology testOntology;
    private static final String PARTITION = "testspace";

    /**
     * Loads the test ontology and load it to the ontology handler
     *
     * @throws URISyntaxException Thrown when the path the the ontology is not
     * correct
     * @throws OWLOntologyCreationException Thrwon when the ontology could not
     * be loaded
     */
    @BeforeClass
    public static void setUpBeforeClass() throws URISyntaxException, OWLOntologyCreationException {
        testOntology = new ObaOntology();
        URL url = testOntology.getClass().getResource("/oba_test.owl");
        testOntology.setOwlURI(IRI.create(url));
        testOntology.init();
        OntologyResource or = new OntologyResource();
        or.setOntology(testOntology);
        OntologyHandler.getInstance().addOntology("oba-test", or);
    }

    /**
     * Test to load all ontology classes from a text file.
     */
    @Test
    public void getStoredListTest() {
        String fileName = "textlist";
        StorageHandler handler = getMockedStorageHandler(fileName);
        Mockito.doCallRealMethod().when(handler).getStorage(PARTITION, fileName);
        Set<ObaClass> list = handler.getStorage(PARTITION, fileName);
        Assert.assertEquals("2 ontology classes should be restored from the text file", 2, list.size());
    }
    /**
     * Test to load all existing ontology classes from a text file. One not
     * existing class is not loaded, no exception is thrown, no warning is logged.
     */
    @Test
    public void getStoredListWrongClassTest() {
        String fileName = "textlist2";
        StorageHandler handler = getMockedStorageHandler(fileName);
        Mockito.doCallRealMethod().when(handler).getStorage(PARTITION, fileName);
        Set<ObaClass> list = handler.getStorage(PARTITION,fileName);
        Assert.assertEquals("One, non-existing ontology classes can not be restored from the text file", 1, list.size());
    }

    @Test(expected = WebApplicationException.class)
    public void getStoredNotExistingListTest() {
        String fileName = "notthere";
        StorageHandler handler = getMockedStorageHandler(fileName);
        Mockito.doCallRealMethod().when(handler).getStorage(PARTITION, fileName);
        handler.getStorage(PARTITION, fileName);
    }

    private StorageHandler getMockedStorageHandler(String fileName) {
        StorageHandler handler = Mockito.mock(StorageHandler.class);
        Mockito.when(handler.getMimeTypeForDataFile("testspace", fileName)).thenReturn("text");
        Mockito.when(handler.getRootDir()).thenReturn(handler.getClass().getResource("/").getFile());
        return handler;
    }
}
