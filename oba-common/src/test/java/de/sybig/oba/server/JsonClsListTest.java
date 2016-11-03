package de.sybig.oba.server;

import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class JsonClsListTest {

    /**
     * Test if list is created if it does not exist yet, and the added element
     * can be get again.
     */
    @Test
    public void testAddNull() {

        JsonClsList instance = new JsonClsList();
        JsonCls entity = new JsonCls();
        assertTrue(instance.add(entity));
        assertSame(entity, instance.get(0));
    }

    /**
     * Test if an element is added to the end of an existing list.
     */
    @Test
    public void testAddNew() {

        JsonClsList instance = new JsonClsList();
        List list = new LinkedList();
        list.add(new JsonCls());
        instance.setEntities(list);
        JsonCls entity = new JsonCls();
        assertTrue(instance.add(entity));
        assertSame(entity, instance.get(1));
        assertNotSame(entity, instance.get(0));
    }

    /**
     * Test if NPE is thrown if list is null
     */
    @Test(expected = NullPointerException.class)
    public void testGetNull() {
        JsonClsList instance = new JsonClsList();
        instance.get(0);
    }

    /**
     * If the list is emtpy get should throw an Exception
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetEmpty() {
        JsonClsList instance = new JsonClsList();
        instance.setEntities(new LinkedList());
        instance.get(0);
    }

    /**
     * Test if the element added to the list is also retrieved.
     */
    @Test
    public void testGet() {
        JsonClsList instance = new JsonClsList();
        List<JsonCls> list = new LinkedList<>();
        JsonCls entity = new JsonCls();
        list.add(entity);
        instance.setEntities(list);
        assertSame(entity, instance.get(0));
    }

    /**
     * Test if NPE is thrown for an empty list
     */
    @Test(expected = NullPointerException.class)
    public void testSizeForNull() {
        JsonClsList instance = new JsonClsList();
        instance.size();
    }

    /**
     * Test if an empty list returns the size 0.
     */
    @Test
    public void testSizeEmpty() {
        JsonClsList instance = new JsonClsList();
        instance.setEntities(new LinkedList<JsonCls>());
        assertEquals(0, instance.size());
    }

    /**
     * Test if a not empty list returns the size >0.
     */
    @Test
    public void testSizeNotEmpty() {
        JsonClsList instance = new JsonClsList();
        List<JsonCls> list = new LinkedList<>();
        list.add(new JsonCls());
        instance.setEntities(list);
        assertEquals(1, instance.size());
    }

    /**
     * Test if clone creates a new object and the list of entities is copied.
     */
    @Test
    public void testClone() {
        JsonClsList instance = new JsonClsList();
        JsonCls c = new JsonCls();
        instance.add(c);
        JsonClsList clone = instance.clone();
        assertNotSame(clone, instance);
        assertEquals(clone.size(), instance.size());
    }

    /**
     * TTest if the element of a setted raw entity list, is returned as raw
     * entity as well as normal element.
     */
    @Test
    public void testSetRawEntities() {

        List<JsonCls> e = new LinkedList<>();
        JsonCls c = new JsonCls();
        e.add(c);
        JsonClsList instance = new JsonClsList();
        instance.setRawEntities(e);
        assertEquals(1, instance.getRawEntities().size());
        assertEquals(1, instance.getEntities().size());
        assertEquals(c, instance.get(0));
    }

    /**
     * Tests that, if raw entities are set to <code>null</code>,
     * <code>null</code> is returned as raw and normal entity.
     */
    @Test
    public void testSetRawEntitiesNull() {
        JsonClsList instance = new JsonClsList();
        instance.setRawEntities(null);
        assertEquals(null, instance.getRawEntities());
        assertEquals(null, instance.getEntities());
    }
}
