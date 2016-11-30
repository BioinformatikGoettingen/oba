/*
 * Created on May 29, 2009
 */
package de.sybig.oba.server;

import de.sybig.oba.server.pluginManagment.OntologyLoader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import javax.validation.constraints.Null;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to configure and start the REST server. The HTTP requests are handled
 * by {@link Root.java} .
 */
public class RestServer {

    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private static Properties props;
    private static RestServer obaServer;
    private ResourceConfig resourceConfig;
    private static HttpServer server;

    RestServer() {
        //utility class with no public non-static methods.
    }

    /**
     * Start method of the OBA service.
     *
     * @param args The commandline arguments
     * @throws IOException
     */
    public static void main(String[] args) {
        obaServer = new RestServer();
        props = loadServerProperties(args, obaServer);
        obaServer.run();
    }

    public static void shutdown() {
        server.shutdown();
    }

    private void run() {
        try {
            props.setProperty("base_dir", getBaseDir().getAbsolutePath());
            resourceConfig = configureServer();
            OntologyHandler oh = OntologyHandler.getInstance();
            oh.setGeneralProperties(props);
            loadPlugins();
            loadOntologies();
            HttpServer httpServer = startServer();
            setServer(httpServer);
        } catch (IOException | IllegalArgumentException ex) {
            logger.error(
                    "There was a fatal error while running the server, will quit now\n",
                    ex);
            server.shutdownNow();
        }
    }

    private static void setServer(HttpServer httpServer) {
        server = httpServer;
    }

    private ResourceConfig configureServer() {
        //To debug the server use curl -H X-Jersey-Tracing-Accept:whatever -v http:...
        ResourceConfig rc = new ResourceConfig().packages("de.sybig.oba.server");
        rc.property("jersey.config.server.tracing.type", "ON_DEMAND");
        rc.property("jersey.config.server.tracing.threshold", "VERBOSE");
        rc.property("com.sun.jersey.config.feature.Trace", "true");

        String host = props.getProperty("bind_address");
        String port = props.getProperty("port", "9998");
        String base = props.getProperty("base", "/");
        String baseUri = String.format("http://%s:%s%s", host, port, base);
        props.put("base_uri", baseUri);

        return rc;
    }

    private HttpServer startServer() throws IOException {

        String baseUri = props.getProperty("base_uri", "http://localhost:9998/");
        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), resourceConfig);

        logger.info("Started server at {}", baseUri);
        return httpServer;
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
    private static Properties loadServerProperties(String[] args, Object o) {
        Properties internalProps = new Properties();
        try (InputStream is = o.getClass().getResourceAsStream("/oba.properties")) {
            internalProps.load(is);
        } catch (IOException ex) {
            logger.error("could not load default properties " + ex);
        }
        if (args.length == 1) {
            File externalPropFile = new File(args[0]);
            if (!externalPropFile.exists() || !externalPropFile.canRead()) {
                logger.error(
                        "The property file '{}' could not be open for reading, falling back to default properties.",
                        args[0]);
            }
            logger.info("reading global properties from external file {}",
                    args[0]);
            Properties externalProps = new Properties(internalProps);
            try (FileReader fr = new FileReader(externalPropFile)) {
                externalProps.load(fr);
            } catch (FileNotFoundException ex) {
                logger.error("Could not find the config file {} given on the commad line", externalPropFile, ex);
            } catch (IOException ex) {
                logger.error("Could not read the config file {} given on the commad line", externalPropFile, ex);
            }

            return externalProps;
        }
        return internalProps;
    }

    /**
     * Load the ontologies during startup.
     *
     * @param properties
     */
    private void loadOntologies() {
        Map<String, Properties> availableOntologies = getIdentifierFromOntologyProperties();
        for (String id : availableOntologies.keySet()) {
            loadOntology(id, availableOntologies);
        }
    }

    /**
     * Loads an ontology, if necessary including its dependencies.
     *
     * @param id The identifier of the ontology to load.
     * @param availableOntologies The ontologies available for the server.
     */
    private void loadOntology(String id, Map<String, Properties> availableOntologies) {
        Properties p = availableOntologies.get(id);
        loadDependenciesOntologies(id, availableOntologies);
        OntologyHandler.getInstance().addOntology(p);
    }

    /**
     * Load the dependencies for an ontology.
     *
     * @param id The identifier of the depending ontology.
     * @param availableOntologies All available ontologies.
     */
    private void loadDependenciesOntologies(String id, Map<String, Properties> availableOntologies) {
        Properties p = availableOntologies.get(id);
        if (p.containsKey("depends_on")) {
            String[] previousOntologies = p.getProperty("depends_on").split(";");
            for (String previousOntology : previousOntologies) {
                if (OntologyHandler.getInstance().containsOntology(previousOntology)) {
                    continue;
                }
                if (!availableOntologies.containsKey(previousOntology)) {
                    logger.error("the depending ontology {} for {} was not loaded", previousOntology, id);
                    //TODO stop loading of depending ontologies
                }
                loadOntology(previousOntology, availableOntologies);
            }
        }
    }

    /**
     * Gets for all ontologies in the ontology directory the identifier and the
     * properties. If no ontologies' property files are found, an empty map is
     * returned, never <code>null</code>
     *
     * @return A map of all identifier and ontology properties.
     */
    private Map<String, Properties> getIdentifierFromOntologyProperties() {
        Map<String, Properties> idPropertyMap = new HashMap<>();
        File ontoDir = getOntologyDir(props);

        logger.debug("scanning property files for ontologies from {}", ontoDir);
        File[] files = ontoDir.listFiles();
        for (File f : files) {
            if (!f.getName().endsWith("properties")) {
                continue;
            }
            Properties p = new Properties();

            try (FileReader fr = new FileReader(f)) {
                p.load(fr);
            } catch (FileNotFoundException ex) {
                logger.warn("Could not get the property file for the ontology", ex);
            } catch (IOException ex) {
                logger.warn("Could not read the property file for the ontoloyg", ex);
            }

            if (!p.containsKey("identifier")) {
                logger.warn("could not parse property file {} because the identifier is missing", f.getAbsoluteFile());
                continue;
            }
            idPropertyMap.put(p.getProperty("identifier"), p);
        }
        return idPropertyMap;
    }

    private File getOntologyDir(Properties properties) {
        File ontoDir;
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

    private void registerFunctionClassesToOntologyHandler(
            final String name, final String classname) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        OntologyHandler oh = OntologyHandler.getInstance();
        oh.addFunctionClass(name, classname);

    }

    protected void loadPlugins() {

        File pluginDir = new File(getBaseDir(), "plugins");
        if (!(pluginDir.exists() && pluginDir.isDirectory())) {
            logger.info("Plugin directory {} does not exists or is not a directory.", pluginDir);
            return;
        }

        try {
            registerFunctionClassesToOntologyHandler("basic", "de.sybig.oba.server.OntologyFunctions");
        } catch (ClassNotFoundException e) {
            logger.error("Could not find the class with the basic semantic functions", e);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Could not instanciate the class with the basic semantic functions", e);

        }
        String action = null;
        String filename = null;
        for (File f : pluginDir.listFiles()) {
            try {
                filename = f.getAbsolutePath();
                if (f.isFile() && f.getName().endsWith("jar")) {
                    Manifest manifest;
                    JarFile jar = null;
                    try {
                        jar = new JarFile(f);
                        manifest = jar.getManifest();
                    } catch (IOException e) {
                        logger.warn("File {} is not a valid jar file. The file is ignored", f);
                        continue;
                    } finally {
                        try {
                            if (jar != null) {
                                jar.close();
                            }
                        } catch (IOException ex) {
                            logger.error("Could not close jar file {}", f, ex);
                        }
                    }
                    Attributes entries = manifest.getMainAttributes();
                    String name = getIdentifierFromPlugin(entries);
                    if (name == null) {
                        logger.warn("Could not load plugin {} because no name is specified in the manifest", f);
                        continue;
                    }
                    URLClassLoader loader = new URLClassLoader(new URL[]{f.toURI().toURL()});
                    action = "semantic function";
                    loadFunctionClassFromPlugin(loader, entries, name);
                    action = "jersey providers";
                    loadProvidersFromPlugin(loader, entries);
                    action = "ontology loaders";
                    loadOntologyLoaderFromPlugin(loader, entries, name);
                }

            } catch (ClassNotFoundException e) {
                logger.error("While loading the {} the class was not found in the plugin {} because {}.", action, filename, e.getMessage());
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("While loading the {} the class could not be instantiated in the plugin {} because {}.", action, filename, e.getMessage());
            } catch (MalformedURLException e) {
                logger.error("While loading the {} the manifest entry was not valid in plugin {} because {}.", action, filename, e.getMessage());
            }
        }
    }

    /**
     * Get the (required) name of the plugin from the manifest of the plugin.
     *
     * @param entries The entries of the manifest file.
     * @return The name of the plugin, or <code>null</code>
     *
     */
    @Null
    protected String getIdentifierFromPlugin(final Attributes entries) {
        Attributes.Name pathAttribute = null;
        if (entries.containsKey(new Attributes.Name("function-path-name"))) {
            pathAttribute = new Attributes.Name("function-path-name");
            logger.warn("The usage of the manifest attribute 'function-path-name' is deprecated, use 'name' instead");
        }
        if (pathAttribute == null) {
            pathAttribute = new Attributes.Name("plugin-name");
        }
        return (String) entries.get(pathAttribute);
    }

    /**
     * Instanciate the class with the ontology function from a plugn, if any
     * specified in the manifest, and register them to the
     * {@link de.sybig.oba.server.OntologyHandler}.
     *
     * @param loader The classloader used to load the plugin
     * @param entries The entries of the manifest file.
     * @param name The name of the plugin, as specified in the manifest.
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void loadFunctionClassFromPlugin(final URLClassLoader loader,
            final Attributes entries, final String name)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Attributes.Name functionClassAttribute = new Attributes.Name("function-main-class");
        if (entries.containsKey(functionClassAttribute)) {
            String className = (String) entries.get(functionClassAttribute);

            OntologyFunction instance = (OntologyFunction) loader.loadClass(className).newInstance();
            OntologyHandler oh = OntologyHandler.getInstance();
            oh.addFunctionClass(name, instance);

            logger.info("registering plugin class {} in version {} under the name " + name, className, name);

        }
    }

    /**
     * Instanciate the jersey providers of a plugin, specified in the manifest,
     * and register them to the application.
     *
     * @param loader The classloader used to load the plugin
     * @param entries The entries of the manifest file.
     * @param name The name of the plugin, as specified in the manifest.
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void loadProvidersFromPlugin(URLClassLoader loader, Attributes entries) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Attributes.Name providerClassesAttribute = new Attributes.Name("provider-classes");
        if (entries.containsKey(providerClassesAttribute)) {

            String[] classes = entries.getValue(providerClassesAttribute).split(":");
            for (int i = 0; i < classes.length; i++) {
                Object marshaller = loader.loadClass(classes[i]).newInstance();
                resourceConfig.register(marshaller);
                logger.info("registering class {} for jersey", marshaller.getClass());
            }
        }
    }

    private void loadOntologyLoaderFromPlugin(final URLClassLoader loader,
            final Attributes entries, final String name)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Attributes.Name loaderName = new Attributes.Name("ontology-loader-class");
        if (!entries.containsKey(loaderName)) {
            return;
        }
        String className = (String) entries.get(loaderName);
        OntologyLoader instance = (OntologyLoader) loader.loadClass(className).newInstance();
        OntologyHandler.getInstance().addOntologyLoader(name, instance);
        logger.info("registered class {} for loading ontologies", instance.getClass());
    }

    /**
     * Get the directory where the OBA jar file is in, this could be different
     * from the current working directory. If the application is not started
     * from a jar file the current working directory is used. The base dir is
     * used e.g. to get the plugin directory.
     *
     * @return The base dir of the application.
     */
    protected File getBaseDir() {
        String url = getClass().
                getResource("/" + this.getClass().getName().
                        replaceAll("\\.", "/") + ".class").
                toString();
        url = url.substring(url.indexOf('/')).replaceFirst("/[^/]+\\.jar!.*$", "/");
        File baseDir = new File(url);
        if (!(baseDir.exists() && baseDir.isDirectory())) {
            baseDir = new File(System.getProperty("user.dir"));
        }
        return baseDir;
    }

    public static Properties getProperties() {
        return props;
    }

    // stuff for testing
    protected Logger getLogger() {
        return logger;
    }
}
