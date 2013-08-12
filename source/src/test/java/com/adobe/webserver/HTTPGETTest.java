package com.adobe.webserver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class HTTPGETTest {

	static ServerMain server = new ServerMain();
	private static Logger logger = Logger
			.getLogger(HTTPGETTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		server.init(new File("src" + File.separator + "test" + File.separator
				+ "resources" + File.separator + "conf"
				+ File.separator + "properties.xml"));
		server.start();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}



	@Ignore
	public void testSimpleGETRequestEXE() throws IOException,
			MalformedURLException, SocketTimeoutException {

		logger.trace("testing GET request for EXE file");
		GetRequest.request("7z920.exe");
		logger.trace("test - GET request for EXE file - success");
	}

	@Test
	public void testSimpleGETRequestPDF() throws IOException,
			MalformedURLException, SocketTimeoutException {
		logger.trace("testing GET request for PDF file");
		String filename = "test.pdf";
		GetRequest.request(filename);
		logger.trace("test -  GET request for PDF file - success");

	}

	@Test
	public void testSimpleGETRequestHTML() throws IOException,
			MalformedURLException, SocketTimeoutException {
		logger.trace("testing GET request for html file");
		String filename = "index.html";
		GetRequest.request(filename);
		logger.trace("test -  GET request for html file - sucesss");

	}

}
