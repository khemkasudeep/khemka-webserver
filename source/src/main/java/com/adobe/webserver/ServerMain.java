package com.adobe.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.apache.log4j.Logger;

/** 
 * It helps you start the server. It contains the main method, so you can start
 * your server directly by this class.
 * 
 * you can also use the methods of this class to start the webserver from some
 * other class
 * 
 * In case you want to start the server from some other class, call following methods in sequence
 * 1. {@link #init(File) init} - Initializes Server with Properties.xml file
 * 2. {@link #start() start} - starts the server
 * 3. {@link #stop() stop} - stops the server
 * 
 * 
 * 
 * @author khemka
 * 
 */
public class ServerMain {

	private static Logger logger = Logger.getLogger(ServerMain.class.getName());
	Thread listenerThread;

	
	private String getProperty(Properties properties, String key,
			String defaultValue) {
		String property = properties.getProperty(key);
		if (property == null || property.length() == 0) {
			return defaultValue;
		}
		return property;
	}

	/**
	 * this method initializes the webserver properties. It does this
	 * initialization with properties file. If you don't provide some property
	 * value in properties.xml, it will take the corresponding default value
	 * 
	 * @param propertiesFile
	 *            File object denoting the properties file
	 * @throws FileNotFoundException
	 */
	public void init(File propertiesFile) throws FileNotFoundException,
			InvalidPropertiesFormatException, IOException {
		logger.trace("initializing server params from config file");

		Properties properties = new Properties();
		InputStream propSource = new FileInputStream(propertiesFile);
		properties.loadFromXML(propSource);

		ServerParams.PORT = Integer.parseInt(getProperty(properties, "port",
				Integer.toString(ServerParams.PORT)));
		ServerParams.WWW_PATH = getProperty(properties, "www_root_path",
				ServerParams.WWW_PATH);
		ServerParams.WWW_UPLOAD_PATH = getProperty(properties,
				"www_upload_path", ServerParams.WWW_UPLOAD_PATH);
		ServerParams.THREAD_POOL_SIZE = Integer.parseInt(getProperty(
				properties, "thread_pool_size",
				Integer.toString(ServerParams.THREAD_POOL_SIZE)));
		ServerParams.HOSTNAME = getProperty(properties, "hostname",
				ServerParams.HOSTNAME);

	}

	/**
	 * starts the server according to properties read in init method.
	 */
	public void start() {

		logger.trace("starting server");

		Listener listener = new Listener();
		listenerThread = new Thread(listener);
		listenerThread.start();

		logger.info("Server listening on port - " + ServerParams.PORT);
	}

	/**
	 * stops the server. waits till the server is stopped before returning. 
	 * 
	 * @throws {@link InterruptedException} if the thread is interrupted
	 */
	public void stop() throws InterruptedException {
		this.listenerThread.interrupt();
		this.listenerThread.join();

	}

	
	/**
	 * it initializes and then starts the server. and waits for input from
	 * console As soon as user enters 'e' , it stops the server
	 * @param args ignored
	 */
	public static void main(String[] args) {

		ServerMain server = new ServerMain();

		try {
			server.init(new File(ServerParams.PropertiesPath));
		} catch (FileNotFoundException e1) {
			logger.error("properties file could not be found .. - "
					+ e1.getMessage());
			return;
		} catch (InvalidPropertiesFormatException e1) {
			logger.error("property file format is wrong. It should satisfy the dtd - \'http://java.sun.com/dtd/properties.dtd\' - "
					+ e1.getMessage());
			return;
		} catch (IOException e1) {
			logger.error("error in reading of properties of file - "
					+ e1.getMessage());
			return;
		} catch (NumberFormatException e1) {
			logger.error("properties file has illegal value - "
					+ e1.getMessage());
			return;
		}

		server.start();
		logger.info("enter e for stopping the server");
		try {
			while ((char) System.in.read() != 'e')
				;
		} catch (IOException e) {

		}
		try {
			server.stop();
		} catch (InterruptedException e) {
			logger.error("server could not be stopped properly .. exiting thread");
		}

	}

}
