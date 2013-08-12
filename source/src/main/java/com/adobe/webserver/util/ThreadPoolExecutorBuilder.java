package com.adobe.webserver.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.adobe.webserver.Listener;
import com.adobe.webserver.ServerParams;


/**
 * ThreadPoolExecutorBuilder provides utilities for creation of threadPool and also for stopping of a thread pool.
 * 
 * {@link Listener} uses this class for creating thread pool for serving client requests.
 * @author KHEMKA
 *
 */
public class ThreadPoolExecutorBuilder {
	
	/**
	 * in seconds
	 */
	private static final int timeToWaitForNormalShutdown = 60;
	
	private static Logger logger = Logger.getLogger(ThreadPoolExecutorBuilder.class.getName());
	
	
	private ThreadPoolExecutorBuilder(){
		
	}
	
	
	
	/**
	 * creates a thread pool with default settings
	 * @return a threadpool
	 */
	public static ExecutorService newCustomThreadPool(){
		ThreadPoolExecutor tPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(ServerParams.THREAD_POOL_SIZE);
		
		return tPool;
	}
	
	

	/**
	 * It shuts down a executor service. 
	 * First of all it disables new taks from being submitted, and waits for 60 seconds for normal shutdown.
	 * if it does not shutdown till then,it Cancels currently executing tasks
	 * 
	 * code copied from "http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/ExecutorService.html" 
	 * used to shutdown an executor service properly .. slightly modified to use logger instead of System.err
	 * @param pool
	 */
	public static void  shutdownAndAwaitTermination(ExecutorService pool) {
		   pool.shutdown(); // Disable new tasks from being submitted
		   try {
		     // Wait a while for existing tasks to terminate
		     if (!pool.awaitTermination(timeToWaitForNormalShutdown, TimeUnit.SECONDS)) {
		       pool.shutdownNow(); // Cancel currently executing tasks
		       // Wait a while for tasks to respond to being cancelled
		       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
		           logger.error("Pool did not terminate");
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
		     pool.shutdownNow();
		     // Preserve interrupt status
		     Thread.currentThread().interrupt();
		   }
		 }
		 
	
	
	
}
