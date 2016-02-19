/*
 * Created on May 29, 2v009
 *
 */
package de.sybig.oba.server;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Wrapper to start the web server and redirects requests to the root classes.
 */
public class RestServer {
    
    private RestServer() {
        //utility class with no public non-static methods.
    }
    private static final Logger logger = LoggerFactory.getLogger(RestServer.class.toString());
    private static Properties props;
    
    public static void main(String[] args) throws IOException {
        RestServer server = new RestServer();
        props = loadServerProperties(args, server);        
        server.run();
    }
    
    public static Properties getProperties() {
        return props;
    }
    
    private void run() {
        try {
            props.setProperty("base_dir", getBaseDir().getAbsolutePath());
            startServer();
        } catch (Exception ex) {
            logger.error(
                    "There was a fatal error while running the server, will quit now\n {}",
                    ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
    private void startServer() throws IllegalArgumentException,
            IOException {
        OntologyHandler.getInstance().setGeneralProperties(props);
        loadUserData(props);
        
        String host = props.getProperty("host", InetAddress.getLocalHost().getHostAddress());
        String port = props.getProperty("port", "9998");
        String base = props.getProperty("base", "/");
        
        String baseUri = String.format("http://%s:%s%s", host, port, base);
        logger.info("Starting server at {}", baseUri);
        
        Map<String, String> initParams = new HashMap<String, String>();
//        initParams.put("com.sun.jersey.config.property.packages",
//                "de.sybig.oba.server;org.codehaus.jackson.jaxrs");
        initParams.put("com.sun.jersey.config.property.packages",
                "de.sybig.oba.server");
        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(
                baseUri, initParams);
        threadSelector.setCompression("force");
        threadSelector.setCompressionMinSize(0);
        threadSelector.setCompressableMimeTypes("text/html");

        // System.in.read();
        // threadSelector.stopEndpoint();
        // System.out.println("stopped");
    }

    /**
     * Loads the properties for the server. First the default property file
     * 'oba.properties' from the classpath is loaded. If a property file is
     * specified as first parameter. This file is loaded in addition. If a
     * property is declared in both files the value from the external file will
     * be used.
     *
     * @param args The arguments from the program start.
     * @param o A object to get ressources from the classpath.
     * @return Properties for the server
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static Properties loadServerProperties(String[] args, Object o)
            throws IOException {
        Properties internalProps = new Properties();
        InputStream is = null;
        try {
            is = o.getClass().getResourceAsStream("/oba.properties");
            internalProps.load(is);
        } catch (Exception ex) {
            logger.error("could not load default properties " + ex);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        if (args.length == 1) {
            File externalPropFile = new File(args[0]);
            if (!externalPropFile.exists() || !externalPropFile.canRead()) {
                logger.error(
                        "The property file '{}' could not be open for reading, falling back to default properties.",
                        args[0]);
            }
            Properties externalProps = new Properties(internalProps);
            FileReader fr = new FileReader(externalPropFile);
            externalProps.load(fr);
            fr.close();
            logger.info("reading global properties from external file {}",
                    args[0]);
            return externalProps;
        }
        return internalProps;
    }

    /**
     * Loads the ontologies and the classes with extra functions.
     *
     * @param properties
     */
    private void loadUserData(Properties properties) {
        OntologyHandler oh = OntologyHandler.getInstance();
        
        loadOntologies(properties, oh);
        loadFunctionClasses(oh);
        loadPlugins(oh);
    }

    /**
     * Loads the ontologies from the "ontology_directory" as specified in the
     * props and add them to the OntologyHandler.
     *
     * @param properties The properties
     */
    private void loadOntologies(final Properties properties,
            final OntologyHandler oh) {
        
        File ontoDir = getOntologyDir(properties);
        
        logger.debug("loading ontologies from {}", ontoDir);
        File[] files = ontoDir.listFiles();
        for (File f : files) {
            if (!f.getName().endsWith("properties")) {
                continue;
            }
            Properties p = new Properties();
            try {
                logger.info("load property file {}", f);
                FileReader fr = new FileReader(f);
                p.load(fr);
                fr.close();
                oh.addOntology(p);
                
            } catch (FileNotFoundException e) {
                logger.warn(
                        "could not load the ontology {} specified in property file {}. The file was not found.",
                        p, f);
            } catch (IOException e) {
                logger.warn(
                        "could not load the ontology {} specified in property file {}. The file could not be read.",
                        p, f);
                // e.printStackTrace();
            }
        }
    }
    
    private File getOntologyDir(Properties properties) {
        File ontoDir = null;
        String dirFromProps = properties.getProperty("ontology_directory");
        if (dirFromProps != null) {
            ontoDir = new File(dirFromProps);
            if (ontoDir.exists() && ontoDir.isDirectory()) {
                return ontoDir;
            }
            logger.debug("The ontology directory {} specified in the properties is not valid", dirFromProps);
        }
        ontoDir = new File(getBaseDir(), "ontologies");
        if (ontoDir.exists() && ontoDir.isDirectory()) {
            return ontoDir;
        }
        
        return new File(System.getProperty("java.io.tmpdir"));
    }
    
    private void loadFunctionClasses(
            final OntologyHandler oh) {
        
        try {
            // TODD make generic
//            oh.addFunctionClass("cytomer",
//                    "de.sybig.oba.cytomer.CytomerFunctions");
            oh.addFunctionClass("basic",
                    "de.sybig.oba.server.OntologyFunctions");
//            oh.addFunctionClass("tribolium",
//                    "de.sybig.oba.tribolium.TriboliumFunctions");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void loadPlugins(final OntologyHandler oh) {
        
        File pluginDir = new File(getBaseDir(), "plugins");
        
        if (!(pluginDir.exists() && pluginDir.isDirectory())) {
            logger.info("Plugin directory {} does not exists or is not a directory.", pluginDir);
            return;
        }
        
        for (File f : pluginDir.listFiles()) {
            if (f.isFile() && f.getName().endsWith("jar")) {
                Manifest manifest = null;
                try {
                    JarFile jar = new JarFile(f);
                    manifest = jar.getManifest();
                } catch (IOException e) {
                    logger.warn("File {} is not a valid jar file. The file is ignored", f);
                    continue;
                }
                
                Attributes entries = manifest.getMainAttributes();
                Attributes.Name pathAttribute = new Attributes.Name("function-path-name");
                Attributes.Name functionClassAttribute = new Attributes.Name("function-main-class");
                
                if (entries.containsKey(pathAttribute) && entries.containsKey(functionClassAttribute)) {
                    String name = (String) entries.get(pathAttribute);
                    String className = (String) entries.get(functionClassAttribute);
           
                    try {
                        URLClassLoader loader = new URLClassLoader(new URL[]{f.toURI().toURL()});
                        
                        OntologyFunction instance = (OntologyFunction) loader.loadClass(className).newInstance();
                        oh.addFunctionClass(name, instance);
                        logger.info("registering plugin class {} in version {} under the name "+ name, instance, instance.getVersion());
                    } catch (ClassNotFoundException e) {
                        logger.error("The class {} specified in the plugin {} is not found in the jar.", className, f);
                    } catch (InstantiationException e) {
                        logger.error("The class {} of the plugin {} could not be instantiated.\n" + e, className, f);
                    } catch (IllegalAccessException e) {
                        logger.error("The class {} of the plugin {} could not be instantiated.\n" + e, className, f);
                    } catch (MalformedURLException e) {
                        logger.error("The class name '{}' specified in the manifest of the file {} is not valid", className, f);
                    }
                    
                }
                
            }
        }
    }
    
    private File getBaseDir() {
        String url = getClass().getResource("/" + this.getClass().getName().replaceAll("\\.", "/") + ".class").toString();
        url = url.substring(url.indexOf("/")).replaceFirst("/[^/]+\\.jar!.*$", "/");
        File baseDir = new File(url);
        if (!(baseDir.exists() && baseDir.isDirectory())) {
            baseDir = new File(System.getProperty("user.dir"));
        }
        return baseDir;
    }
}
