package com.adobe.webserver;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

public class GetRequest //implements Callable<Boolean> {
{
	String filename;
	private static Logger logger = Logger.getLogger(GetRequest.class);

	public GetRequest(String filename) {
		this.filename = filename;
	}

	
	public static boolean request(String filename) throws IOException{
		String urlString = "http://" + "localhost:" + ServerParams.PORT
				+ ServerParams.URISeparator + filename;
		URL url = new URL(urlString);

		HttpURLConnection connection = null;
		InputStream connectionIS = null;
		InputStream fileIS = null;
		try {
			connection = (HttpURLConnection) url.openConnection();

			connection.connect();

			if (connection.getResponseCode() != 200) {
				fail("GET request failed status code - "
						+ connection.getResponseCode());
			}

			connectionIS = connection.getInputStream();
			File f = new File("src" + File.separator + "test"
					+ File.separator + "resources" + File.separator + "webapps"
					+ File.separator + filename);
			
			fileIS = new FileInputStream(f);

			byte[] fileBuffer = new byte[1024];
			byte[] connectionBuffer = new byte[1024];
			int FBlen = 0;
			int CBlen = 0;
			while ((CBlen = connectionIS.read(connectionBuffer)) > -1) {
				int cbIndex = -1;
				while (CBlen != 0) {
					FBlen = fileIS.read(fileBuffer, 0, CBlen);
					for (int i = 0; i < FBlen; i++) {
						cbIndex++;
						if (fileBuffer[i] != connectionBuffer[cbIndex]) {
							fail("the file received does not match with incoming file");
						}

					}
					CBlen -= FBlen;
				}

			}

		} finally {

			try {
				if (fileIS != null)
					fileIS.close();
			} catch (IOException e) {
				logger.error("file input stream could not be stopped - "
						+ e.getMessage());
			}

			try {
				if (connectionIS != null)
					connectionIS.close();
			} catch (IOException e) {
				logger.error("input stream could not be stopped - "
						+ e.getMessage());
			}
			if (connection != null)
				connection.disconnect();
		}

		return true;
		
	}
	
	


}
