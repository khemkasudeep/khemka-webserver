package com.adobe.webserver;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.adobe.webserver.util.ThreadPoolExecutorBuilder;

public class HTTPStressTest {

	static ServerMain server = new ServerMain();
	private static Logger logger = Logger.getLogger(HTTPStressTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		
		server.init(new File("src"+File.separator+"test"+File.separator+"resources"+File.separator+"conf"+
				File.separator+ "properties.xml"));
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

	
	@Test
	public void stressTest() throws InterruptedException {
		final String fileName = "index.html";
		int numOfTasks = 100;
		int concurrentRequest = 4;
		int timeOut = 10;
		
		
		ArrayList<Callable<Boolean>> taskList = new ArrayList<Callable<Boolean>>(numOfTasks);
		for(int i=0;i<numOfTasks;i++){
			Callable<Boolean> getReq =   new Callable<Boolean>() {
				public Boolean call() throws IOException {
					return GetRequest.request(fileName);			//accessing index.html
				}
		};
			taskList.add(getReq);
		}
		ExecutorService threadPool = Executors.newFixedThreadPool(concurrentRequest);
		
		try{
		List<Future<Boolean>> result = threadPool.invokeAll(taskList, timeOut, TimeUnit.SECONDS);
		
		Iterator<Future<Boolean>> itr = result.iterator();
		int success=0;
		int cancelled = 0;
		int exception = 0;
		int interrupted = 0;
		while (itr.hasNext()) {
			Future<Boolean> output = itr.next();
			try {
				output.get();
				success++;
			}catch(CancellationException e){
				cancelled++;
			} catch (InterruptedException e) {
				interrupted++;
			} catch (ExecutionException e) {
				exception++;
			}
		}
		
		
		assertEquals("statistics of failure - " + "suceesss:"+ success +" cancelled:"
				+ cancelled + " exception:" + exception + " interrupted:" + interrupted +
				" num of threads could not stopped:" + (numOfTasks-success-cancelled-exception-interrupted)
				, new Integer(numOfTasks), new Integer(success));
		
		
		}finally{
			ThreadPoolExecutorBuilder.shutdownAndAwaitTermination(threadPool);
			taskList.clear();
		}
		
	}

}
