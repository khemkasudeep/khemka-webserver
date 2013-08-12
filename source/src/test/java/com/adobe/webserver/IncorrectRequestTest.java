package com.adobe.webserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class IncorrectRequestTest {

	static ServerMain server = new ServerMain();
	private static Logger logger = Logger.getLogger(IncorrectRequestTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		server.init(new File("src" + File.separator + "test" + File.separator
				+ "resources" + File.separator + "conf" + File.separator
				+ "properties.xml"));
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

	/**
	 * read request line from client
	 * 
	 * @param clientInpStream
	 *            client Reader
	 * @return the request line without trailing \r\n
	 * @throws IOException
	 *             when there is error in read
	 */
	public static String readRequestLineFromClient(InputStream clientInpStream)
			throws IOException {
		StringBuffer requestLine = new StringBuffer(20);
		int inp = 0;
		while ((inp = clientInpStream.read()) > -1) {
			if ((char) inp != '\r') {
				requestLine.append((char) inp);
			} else {
				inp = clientInpStream.read();
				if ((char) inp != '\n') {
					logger.error("the client passed a ill formed header");
					fail("while parsing a request line a \r was found. But the character following it was not \n");
				}
				return requestLine.toString();
			}
		}

		fail("client socket closed connection .. request line could not be read completely");
		return null;

	}

	
	/**
	 * sending a request method for a file which is not present
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws SocketTimeoutException
	 */
	@Test
	public void testRequestNonExistantFile() throws IOException,
			MalformedURLException, SocketTimeoutException {

		File file = new File(ServerParams.WWW_PATH + File.separator
				+ "keep_this_folder_empty" + File.separator + "test.html");
		if (file.isFile()) {
			file.delete();
		}

		StringBuffer headerBuilder = new StringBuffer(50);

		headerBuilder
				.append("GET /keep_this_folder_empty/test.html HTTP/1.1\r\n");
		headerBuilder.append("Server: " + "Java HTTP Server 1.1\r\n");
		headerBuilder.append("Date: "
				+ new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
						.format(new Date()) + "\r\n");
		headerBuilder.append("Connection: close\r\n\r\n");

		String header = headerBuilder.toString();

		Socket client = null;
		BufferedWriter out = null;
		InputStream is = null;
		String responseCode = null;
		try {
			client = new Socket(ServerParams.HOSTNAME, ServerParams.PORT);
			client.setSoTimeout(ServerParams.SocketSoTimeout);
			out = new BufferedWriter(new OutputStreamWriter(
					client.getOutputStream(), ServerParams.HTTPHeadersEncoding));
			out.write(header);
			out.flush();
			is = client.getInputStream();
			String requestLine = readRequestLineFromClient(is);
			String[] requestLineSplitedStrings = requestLine.split(" ");
			responseCode = requestLineSplitedStrings[1];
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				logger.error("stream could not be closed - " + e.getMessage());
			}

			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				logger.error("stream could not be closed - " + e.getMessage());
			}

			try {
				if (client != null)
					client.close();
			} catch (IOException e) {
				logger.error("stream could not be closed - " + e.getMessage());
			}
		}

		assertEquals(
				"the file we are looking for does not exist so should return 404 Not Found ",
				responseCode, "404");

	}

	/**
	 * sending a request method which is not valid
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws SocketTimeoutException
	 */
	@Test
	public void testRequestNonMethod() throws IOException,
			MalformedURLException, SocketTimeoutException {

		StringBuffer headerBuilder = new StringBuffer(50);

		headerBuilder
				.append("FAKE /keep_this_folder_empty/test.html HTTP/1.1\r\n");
		headerBuilder.append("Server: " + "Java HTTP Server 1.1\r\n");
		headerBuilder.append("Date: "
				+ new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
						.format(new Date()) + "\r\n");
		headerBuilder.append("Connection: close\r\n\r\n");

		String header = headerBuilder.toString();

		Socket client = null;
		BufferedWriter out = null;
		InputStream is = null;
		String responseCode = null;
		try {
			client = new Socket(ServerParams.HOSTNAME, ServerParams.PORT);
			client.setSoTimeout(ServerParams.SocketSoTimeout);
			out = new BufferedWriter(new OutputStreamWriter(
					client.getOutputStream(), ServerParams.HTTPHeadersEncoding));
			out.write(header);
			out.flush();
			is = client.getInputStream();
			String requestLine = readRequestLineFromClient(is);
			String[] requestLineSplitedStrings = requestLine.split(" ");
			responseCode = requestLineSplitedStrings[1];
		} finally {

			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				logger.error("stream could not be closed - " + e.getMessage());
			}

			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				logger.error("stream could not be closed - " + e.getMessage());
			}

			try {
				if (client != null)
					client.close();
			} catch (IOException e) {
				logger.error("stream could not be closed - " + e.getMessage());
			}
		}

		assertEquals("the method is not supported", responseCode, "405");
	}

}
