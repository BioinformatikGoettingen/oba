/*
 * Created on May 29, 2009
 */
package de.sybig.oba.server;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
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

    private static final Logger logger = LoggerFactory.getLogger(RestServer.class.toString());
    private static Properties props;
    private ResourceConfig resourceConfig;

    private RestServer() {
        //utility class with no public non-static methods.
    }

    public static void main(String[] args) throws IOException {
        RestServer server = new RestServer();
        props = loadServerProperties(args, server);
        server.run();
    }

    private void run() {
        try {
            props.setProperty("base_dir", getBaseDir().getAbsolutePath());
            resourceConfig = configureServer();
            OntologyHandler oh = OntologyHandler.getInstance();
            oh.setGeneralProperties(props);
            loadOntologies(props, oh);
            loadFunctionClasses(oh);
            loadPlugins(oh);
            startServer();
        } catch (Exception ex) {
            logger.error(
                    "There was a fatal error while running the server, will quit now\n {}",
                    ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private ResourceConfig configureServer() {
        ResourceConfig rc = new ResourceConfig().packages("de.sybig.oba.server");
        rc.property("jersey.config.server.tracing.type", "ALL");
        rc.property("jersey.config.server.tracing.threshold", "VERBOSE");
        rc.property("com.sun.jersey.config.feature.Trace", "true");
        String host = "0.0.0.0";

        String port = props.getProperty("port", "9998");
        String base = props.getProperty("base", "/");
        String baseUri = String.format("http://%s:%s%s", host, port, base);
        props.put("base_uri", baseUri);

        return rc;
    }

    private void startServer() throws IllegalArgumentException,
            IOException {

        String baseUri = props.getProperty("base_uri", "http://localhost:9998/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), resourceConfig);

        logger.info("Started server at {}", baseUri);
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

            oh.addFunctionClass("basic",
                    "de.sybig.oba.server.OntologyFunctions");
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
                String name = getIdentifierFromPlugin(entries);
                if (name == null) {
                    logger.warn("Could not load plugin {} because no name is specified in the manifest", f);
                    continue;
                }
                String action = null;
                try {
                    URLClassLoader loader = new URLClassLoader(new URL[]{f.toURI().toURL()});
                    action = "semantic function";
                    loadFunctionClassFromPlugin(loader, entries, name);
                    action = "jersey providers";
                    loadProvidersFromPlugin(loader, entries);
                } catch (ClassNotFoundException e) {
                    logger.error("While loading the {} the class was not find in the plugin {}.", action, f);
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("While loading the {} the class could not be instantiated in the plugin {}." + e, action, f);
                } catch (MalformedURLException e) {
                    logger.error("While loading the {} the manifest entry was not valid in plugin {}", action, f);
                }

            }
        }
    }

    private String getIdentifierFromPlugin(Attributes entries) {
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

    private void loadFunctionClassFromPlugin(URLClassLoader loader, Attributes entries, String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Attributes.Name functionClassAttribute = new Attributes.Name("function-main-class");
        if (entries.containsKey(functionClassAttribute)) {
            String className = (String) entries.get(functionClassAttribute);
            OntologyFunction instance = (OntologyFunction) loader.loadClass(className).newInstance();
            OntologyHandler oh = OntologyHandler.getInstance();
            oh.addFunctionClass(name, instance);
            logger.info("registering plugin class {} in version {} under the name " + name, instance, instance.getVersion());

        }
    }

    private void loadProvidersFromPlugin(URLClassLoader loader, Attributes entries) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Attributes.Name providerClassesAttribute = new Attributes.Name("provider-classes");
        if (entries.containsKey(providerClassesAttribute)) {

            String[] classes = (String[]) entries.getValue(providerClassesAttribute).split(":");
            for (int i = 0; i < classes.length; i++) {
                Object marshaller = loader.loadClass(classes[i]).newInstance();
                resourceConfig.register(marshaller);
                logger.info("registering class {} for jersey", marshaller.getClass());
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

    public static Properties getProperties() {
        return props;
    }

}
