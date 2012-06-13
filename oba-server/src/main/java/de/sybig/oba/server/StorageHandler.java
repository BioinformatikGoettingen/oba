/*
 * Created on May 10, 2010
 *
 */
package de.sybig.oba.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;
import java.io.*;

/**
 * Subresource to handle the storage and retrieval of list with ontology
 * classes. These list are stored in the file system and the user can refer to
 * them in other functions.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 *
 */
public class StorageHandler {

    private Logger logger = LoggerFactory.getLogger(StorageHandler.class.toString());
    private static String rootDir;

    public StorageHandler() {
        // System.out.println("new storage handler");
    }

    /**
     * Accepts a list in plain text format. The format is one class per line
     * with the full name of the class i.e. http://namespace#classname. If a
     * list was already saved under the same name in this partition the old list
     * is overwritten.
     *
     * Possible http codes are <ul> <li>201 if the list could be saved.</li>
     * <li>413 if the list exceeds the limit of 20kb</li> <li>500 for any other
     * error</li> </ul>
     *
     * @param partition The partition to store the list in
     * @param name The name to store the list.
     * @param is
     */
    @PUT
    @Path("{partition}/{name}")
    @Consumes("text/plain")
    public void putStorageText(@PathParam("partition") String partition,
            @PathParam("name") String name, InputStream is) {
        logger.info("PUT mit text");
        testNames(partition, name);
        saveInputStream(partition, name, is);
        StorageDatabase db = StorageDatabase.getInstance();
        db.logPut(partition, name, "text");
    }

    /**
     * Accepts a list in json format.
     *
     * If the name for the partition or the set does not confirm to the rules a
     * web exception with status code 400 is thrown.
     *
     * @see #putStorageText(String, String, InputStream)
     * @param partition
     * @param name
     * @param is
     */
    @PUT
    @Path("{partition}/{name}")
    @Consumes("application/json")
    // @Produces("application/json")
    public void putStorageJson(@PathParam("partition") String partition,
            @PathParam("name") String name, InputStream is) {
        logger.info("PUT mit json");
        testNames(partition, name);
        saveInputStream(partition, name, is);
        StorageDatabase db = StorageDatabase.getInstance();
        db.logPut(partition, name, "json");
    }

    /**
     * Lists the content of a partition. The content of the "tmp" partition will
     * not be listed.
     *
     * @param partition
     * @return
     */
    @GET
    @Path("{partition}")
    @Produces("text/plain")
    public Object listStorage(@PathParam("partition") String partition) {

        if (!testName(partition)) {
            throw new WebApplicationException(403);
        }
        File dir = new File(getRootDir(), partition);
        if (!dir.exists() || !dir.canRead() || !dir.isDirectory()) {
            throw new WebApplicationException(404);
        }
        StringBuffer out = new StringBuffer();
        for (File f : dir.listFiles()) {
            out.append(f.getName());
            out.append("\n");
        }
        return out.toString();
    }

    /**
     * Gets the content of the list stored under the given name in the given
     * partition. The classes listed in the list are fetched from the loaded
     * ontologies, and returned in the requested format. Classes not found in
     * the ontologies are omitted.
     *
     * If the list is not found the web exception with status code 404 is
     * thrown.
     *
     * @param partition
     * @param name
     * @return The stored set or web exception 404
     */
    @GET
    @Path("{partition}/{name}")
    @Produces("text/plain, text/html, application/json")
    public Set<ObaClass> getStorage(@PathParam("partition") String partition,
            @PathParam("name") String name) {
        StorageDatabase db = StorageDatabase.getInstance();
        db.logGet(partition, name);
        String mimetype = db.getMimetype(partition, name);
        if (mimetype == null) {
            logger.info("could not retrun storage list, because the MIME type is unknown.");
            throw new WebApplicationException(404);
        }
        return getStoredList(partition, name, mimetype);
    }

    /**
     * Generates a name for a partition and creates the directory for the
     * partition.
     *
     * @return
     */
    @GET
    @Path("uniqueID")
    @Produces("text/plain")
    public String getUniqueID() {
        File f = null;
        while (f == null) {
            String id = generateId(12);
            f = new File(getRootDir(), id);
            if (!f.exists()) {

                f.mkdir();
                return id;
            }
        }
        return null;
    }

    /**
     * Test if the name of the partition and file name are permitted . As
     * partition name "tmp" is also allowed.
     *
     * @see #testName(String).
     *
     * @param partition
     * @param name
     */
    private void testNames(String partition, String name) {
        if (!partition.equals("tmp") && !testName(partition)) {
            logger.warn("illegal space name '{}'", partition);
            throw new WebApplicationException(400);
        }
        if (!testName(name)) {
            logger.warn("illegal name for storage'{}'", name);
            throw new WebApplicationException(400);
        }
    }

    /**
     * Tests if a name is permitted. A name may contain lower and upper case
     * letters and the numbers 0-9.
     *
     * @param name The name to test.
     * @return
     */
    private boolean testName(String name) {
        if (name.length() < 6) {
            return false;
        }
        if (name.length() > 12) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char b = name.charAt(i);
            // 0=48 9= 59 A=65 Z=90 a=97 z=122
            if (96 < b && b < 123) {
                continue;
            }
            if (64 < b && b < 91) {
                continue;
            }
            if (47 < b && b < 60) {
                continue;
            }
            return false;
        }
        return true;
    }

    File getStorageFile(String space, String name) {
        File dir = new File(rootDir, space);
        File file = new File(dir, name);
        return file;
    }

    private Set<ObaClass> getStoredList(String space, String name,
            String mimetype) {
        if (mimetype.equals("json")) {
            return getStoredListFromJson(space, name);
        } else if (mimetype.equals("text")) {
            return getStoredListFromText(space, name);
        }

        logger.error("could not convert stored list with mimetype {}", mimetype);
        return null;
    }

    private Set<ObaClass> getStoredListFromJson(String space, String name) {
        JSONUnmarshaller m;
        try {
            HashSet<ObaClass> out = new HashSet<ObaClass>();
            File f = new File(new File(getRootDir(), space), name);
            FileInputStream is = new FileInputStream(f);
            JAXBContext ctx = JAXBContext.newInstance(JsonClsList.class);
            m = JSONJAXBContext.getJSONUnmarshaller(ctx.createUnmarshaller());
            JsonClsList<JsonCls> list = m.unmarshalFromJSON(is,
                    JsonClsList.class);
            OntologyHandler oh = OntologyHandler.getInstance();
            for (JsonCls jc : list.getEntities()) {
                ObaClass c = oh.getClass(jc.getName(), jc.getNamespace());
                if (c != null) {
                    out.add(c);
                }
            }
            return out;
        } catch (JAXBException e2) {
            logger.warn(
                    "could not unmarshall the list {} from space {} to json",
                    name, space);
        } catch (FileNotFoundException e) {
            logger.warn(
                    "could not unmarshall the list {} from space {}, file not found",
                    name, space);
            throw new WebApplicationException(404);
        } catch (IllegalStateException e) {
            // list is empty
            return null;
        }
        return null;
    }

    private Set<ObaClass> getStoredListFromText(String space, String name) {
        HashSet<ObaClass> out = new HashSet<ObaClass>();
        File f = new File(new File(getRootDir(), space), name);
        try {
            OntologyHandler oh = OntologyHandler.getInstance();
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitLine = line.split("#");
                if (splitLine.length != 2) {
                    continue;
                }
                ObaClass c = oh.getClass(splitLine[1], splitLine[0]);
                if (c != null) {
                    out.add(c);
                }else{
                    logger.warn("could not get class for stored line {}", line);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            logger.warn(
                    "could not convert the list {} from space {} to text, file not found",
                    name, space);
            throw new WebApplicationException(404);
        } catch (IOException e) {
            logger.warn(
                    "could not convert the list {} from space {} to text, could not read file",
                    name, space);
        }
        return out;
    }

    private String generateId(int length) {
        StringBuffer id = new StringBuffer();
        Random rand = new Random();
        for (int i = 0; i < length; i++) {

            id.append((char) (rand.nextBoolean() ? rand.nextInt(9) + 48 : rand.nextInt(25) + 97));
        }
        return id.toString();
    }

    /**
     * Saves the input stream in a file with the given name in the given
     * partition (directory).<br />
     *
     * @param partition
     * @param name
     * @param is
     * @throws WebApplicationException 413 if the file exceeds 200kb
     * @throws WebApplicationException 500 for any other IO-exception
     */
    private void saveInputStream(String partition, String name, InputStream is) {
        try {
            Properties props = RestServer.getProperties();
            int maxUploadSize = Integer.parseInt(props.getProperty(
                    "max_upload_size", "1000"));
            File dir = new File(getRootDir(), partition);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, name);
            FileOutputStream os = new FileOutputStream(file);
            byte buf[] = new byte[1024];
            int len;
            int kb = 0;
            while ((len = is.read(buf)) > 0) {
                kb++;
                if (kb > maxUploadSize) {
                    logger.warn(
                            "The uploaded file {} in space {}, exceeds the limit of "
                            + maxUploadSize + "kb", name, partition);
                    throw new WebApplicationException(413);
                }
                os.write(buf, 0, len);
            }
            os.close();
            is.close();

        } catch (IOException e) {
            logger.warn("could not save list file {} in space {}", name,
                    partition);
            throw new WebApplicationException(500);
        }
    }

    private String getRootDir() {
        if (rootDir == null) {
            Properties props = RestServer.getProperties();
            rootDir = props.getProperty("storage_root");
            if (rootDir == null) {
                String baseDir = props.getProperty("base_dir"); // is set upon startup in RestServer
                rootDir = baseDir + File.separatorChar + "storage";
            }

            File r = new File(rootDir);
            if (!r.exists() || !r.canWrite() || !r.isDirectory()) {
                logger.error(
                        "the directory '{}' could not be used as root directory for the storage, will use the tmp directory of the system",
                        rootDir);
                rootDir = System.getProperty("java.io.tmpdir", "/tmp");
            }
        }
        return rootDir;
    }
}