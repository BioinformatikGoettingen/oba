/*
 * Created on May 29, 2v009
 *
 */
package de.sybig.oba.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

/**
 * Wrapper to start the web server and redirects requests to the root classes.
 */
public class RestServer {

	private Logger logger = LoggerFactory
			.getLogger(RestServer.class.toString());
	private static Properties props;

	public static void main(String[] args) throws IOException {
		RestServer server = new RestServer();
		server.run(args);
	}

	public static Properties getProperties() {
		return props;
	}

	private void run(String[] args) {
		try {
			startServer(args);
		} catch (Exception ex) {
			logger.error(
					"There was a fatal error while running the server, will quit now\n {}",
					ex.toString());
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void startServer(String[] args) throws IllegalArgumentException,
			FileNotFoundException, IOException {
		props = loadServerProperties(args);
		OntologyHandler.getInstance().setGeneralProperties(props);
		loadUserData(props);

		String host = props.getProperty("host", InetAddress.getLocalHost()
				.getHostAddress());
		String port = props.getProperty("port", "9998");
		String base = props.getProperty("base", "/");

		String baseUri = String.format("http://%s:%s%s", host, port, base);
		logger.info("Starting server at {}", baseUri);

		Map<String, String> initParams = new HashMap<String, String>();
		initParams.put("com.sun.jersey.config.property.packages",
				"de.sybig.oba.server");
		SelectorThread threadSelector = GrizzlyWebContainerFactory.create(
				baseUri, initParams);

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
	 * @param args
	 *            The arguments from the program start.
	 * @return Properties for the server
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Properties loadServerProperties(String[] args)
			throws FileNotFoundException, IOException {
		Properties internalProps = new Properties();
		InputStream is = this.getClass().getResourceAsStream("/oba.properties");
		internalProps.load(is);
		if (args.length == 1) {
			File externalPropFile = new File(args[0]);
			if (!externalPropFile.exists() || !externalPropFile.canRead()) {
				logger.error(
						"The property file '{}' could not be open for reading, falling back to default properties.",
						args[0]);
			}
			Properties externalProps = new Properties(internalProps);
			externalProps.load(new FileReader(externalPropFile));
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
		loadFunctionClasses(properties, oh);
	}

	/**
	 * Loads the ontologies from the 'ontology_directory' as specified in the
	 * props and add them to the OntologyHandler.
	 * 
	 * @param properties
	 *            The properties
	 */
	private void loadOntologies(final Properties properties,
			final OntologyHandler oh) {
		File ontoDir = new File(properties.getProperty("ontology_directory",
				System.getProperty("java.io.tmpdir")));
		logger.debug("loading ontologies from {}", ontoDir);
		File[] files = ontoDir.listFiles();
		for (File f : files) {
			if (!f.getName().endsWith("properties")) {
				continue;
			}
			Properties p = new Properties();
			try {
				logger.info("load property file {}", f);
				p.load(new FileReader(f));
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

	private void loadFunctionClasses(final Properties properties,
			final OntologyHandler oh) {

		try {
			// TODD make generic
			oh.addFunctionClass("cytomer",
					"de.sybig.oba.cytomer.CytomerFunctions");
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
}
