package com.adobe.webserver;

import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HTTPPOSTTest {

	static ServerMain server = new ServerMain();
	private static Logger logger = Logger
			.getLogger(HTTPPOSTTest.class);

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

	
	public static void compareTwoFiles(String file1 , String file2) throws IOException{
		//compare file1
				BufferedInputStream original = null;
				BufferedInputStream uploaded = null;
				try{
				original = new BufferedInputStream(new FileInputStream(file1 ));
				uploaded = new BufferedInputStream(new FileInputStream( file2));
				byte[] originalBuffer = new byte[1024];
				byte[] uploadedBuffer = new byte[1024];
				int originalLen = 0;
				int uploadedLen = 0;
				while ((originalLen = original.read(originalBuffer)) > -1) {
					int originalBufferIndex = -1;
					while (originalLen != 0) {
						uploadedLen = uploaded.read(uploadedBuffer, 0, originalLen);
						for (int i = 0; i < uploadedLen; i++) {
							originalBufferIndex++;
							if (uploadedBuffer[i] != originalBuffer[originalBufferIndex]) {
								fail("the file received does not match with incoming file");
							}

						}
						originalLen -= uploadedLen;
					}

				}
				
				}finally{
					original.close();
					uploaded.close();
				}
				
	}
	
	
	@Test
	public void testMultiPartPOST() throws IOException {

		String fileName1 = "test.pdf";
		String fileName2 = "Koala.jpg";
		
		String pathToFile = "src" + File.separator + "test" + File.separator
				+ "resources" + File.separator + "files_to_upload";

		String pathToUploadDir = ServerParams.WWW_PATH + ServerParams.URISeparator + ServerParams.WWW_UPLOAD_PATH;
		
		
		
		
		String url1 = "http://localhost:" + ServerParams.PORT + ServerParams.URISeparator 
				+ ServerParams.WWW_UPLOAD_PATH.replace(File.separator, ServerParams.URISeparator);

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url1);

		FileBody file1 = new FileBody(new File(pathToFile + File.separator
				+ fileName1));
		FileBody file2 = new FileBody(new File(pathToFile + File.separator
				+ fileName2));
		

		MultipartEntity reqEntity = new MultipartEntity();
		reqEntity.addPart("file1", file1);
		reqEntity.addPart("file1", file2);
		

		httppost.setEntity(reqEntity);

		HttpResponse response = null;
		try {
			response = httpclient.execute(httppost);

			if (response.getStatusLine().getStatusCode() != 201) {
				fail("post failed - response code "
						+ response.getStatusLine().getStatusCode());
			}

		} finally {
			if (response != null) {
				HttpEntity resEntity1 = response.getEntity();
				if (resEntity1 != null)
					resEntity1.getContent().close();
			}

		}
		
		
		compareTwoFiles(pathToFile + File.separator + fileName1, pathToUploadDir + File.separator + fileName1);
		compareTwoFiles(pathToFile + File.separator + fileName2, pathToUploadDir + File.separator + fileName2);
		
		
		
		File f1 = new File(pathToUploadDir+File.separator+fileName1);
		File f2 = new File(pathToUploadDir+File.separator+fileName2);
		
		
		if (f1.isFile()) {
			f1.delete();
		}
		if (f2.isFile()) {
			f2.delete();
		}
		

		
	}
}
