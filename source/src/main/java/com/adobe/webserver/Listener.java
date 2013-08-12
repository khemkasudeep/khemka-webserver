package com.adobe.webserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.adobe.webserver.util.ThreadPoolExecutorBuilder;

/**
 * A Listener Listens for client requests.
 * It binds a socket to a port,(IP address if provided in properties) and listens for requests from client.
 * As soon as a request arives, a ClientHandler object is formed and is given the responsibility to deal with the client.
 * And the listener again starts listening for conections
 * 
 * it uses a {@link ExecutorService} for running a clientHandler Thread. It relies on {@link ThreadPoolExecutorBuilder} for 
 * building an appropriate {@link ExecutorService}
 * 
 * it checks for Interrupts at every short interval indicated by {@link ServerParams.#ServerSocketSoTimeout} 
 * 
 * On Interrupt it calls for {@link ThreadPoolExecutorBuilder} method to stop the {@link ExecutorService}
 * 
  * @author sudeepkhemka
 * 
 */
public class Listener implements Runnable {

	private static Logger logger = Logger.getLogger(Listener.class.getName());
	ExecutorService threadPool;

	/**
	 * this is a default constructor
	 */
	public Listener() {
		this.threadPool = ThreadPoolExecutorBuilder.newCustomThreadPool();
	}

	
	
	
	
	/**
	 * it makes server listen at a port. As soon a request comes, it makes a client handler out of it , and 
	 * sends it to execute by a executor service
	 */
	public void run() {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket();//ServerParams.PORT , ServerParams.QUEUE_SIZE);
			serverSocket.bind((ServerParams.HOSTNAME != null) ? 
					new InetSocketAddress(ServerParams.HOSTNAME, ServerParams.PORT) : new InetSocketAddress(ServerParams.PORT) 
					, ServerParams.QUEUE_SIZE );
		} catch (IOException e) {
			logger.fatal("server socket could not be created - \r\n"
					+ e.getMessage() + "\r\n" + e.toString());
			return;
		}
		try {
			serverSocket.setSoTimeout(ServerParams.ServerSocketSoTimeout);
		} catch (SocketException e) {
			logger.fatal("timeout for server socket could not be set - \r\n"
					+ e.getMessage() + "\r\n" + e.toString());
			try {
				serverSocket.close();
			} catch (IOException e1) {
				logger.error("could not cloase server socket - " + e.getMessage());
			}
			return;
		}

		while (true) {
			Socket client = null;
			try {
				client = serverSocket.accept();
				ClientHandler task = new ClientHandler(
						client);
				this.threadPool.execute(task);
				
			}catch(SocketException e){
				logger.error("client so timeout could not be set .. so closing client connection - "  + e.getMessage());
				try {
					client.close();
				} catch (IOException e1) {
					logger.error("client could not be closed properly");
				}
			} catch (SocketTimeoutException e) {
				
				if (Thread.interrupted()) {

					try {
						serverSocket.close();
					} catch (IOException e1) {
						logger.warn("serversocket.accept error occured - \r\n"
								+ e1.getMessage() + "\r\n" + e1.toString());
					}

					ThreadPoolExecutorBuilder.shutdownAndAwaitTermination(this.threadPool);
					logger.info("exiting listener thread");
					return;
				}
				logger.trace("server is idle .. no incoming request ..");
			} catch (IOException e) {
				logger.warn("  serversocket.accept error occured - \r\n"
						+ e.getMessage() + "\r\n" + e.toString());
			} finally {

			}
		}

		
	}
}
