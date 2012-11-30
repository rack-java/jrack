package com.zzzhc.rack.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.SocketHttpServerConnection;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.HandlerOptions;
import com.zzzhc.rack.IHandler;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.RackException;
import com.zzzhc.rack.Response;

public class HttpCoreHandler implements IHandler {
	private static Logger logger = LoggerFactory
			.getLogger(HttpCoreHandler.class);

	private Server server;

	public void start(IMiddleware app, HandlerOptions options) throws Exception {
		RackHandler rackHandler = new RackHandler(app);
		server = new Server(new InetSocketAddress(options.getHost(),
				options.getPort()), rackHandler, options);
		server.start();
	}

	public void stop() {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	static class RackHandler implements HttpRequestHandler {
		private IMiddleware app;

		public RackHandler(IMiddleware app) {
			this.app = app;
		}

		public void handle(final HttpRequest request,
				final HttpResponse httpResponse, final HttpContext context)
				throws HttpException, IOException {
			Env env = toEnv(request, context);
			Response response = app.call(env);
			writeResponse(httpResponse, response);
		}

		private void writeResponse(final HttpResponse httpResponse,
				Response response) throws IOException {
			httpResponse.setStatusCode(response.getStatus());
			for (Map.Entry<String, String> entry : response.getHeaders()
					.entrySet()) {
				httpResponse.addHeader(entry.getKey(), entry.getValue());
			}
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			for (;;) {
				byte[] bytes = response.getBody().next();
				if (bytes == null) {
					break;
				}
				b.write(bytes);
			}
			ByteArrayEntity entity = new ByteArrayEntity(b.toByteArray());
			httpResponse.setEntity(entity);
		}

		private Env toEnv(HttpRequest request, HttpContext context) {
			Env env = new Env();
			env.set(Env.REQUEST_METHOD, request.getRequestLine().getMethod());
			env.set(Env.SCRIPT_NAME, "");

			String uri = request.getRequestLine().getUri();
			String path = uri;
			String queryString = "";
			int idx = uri.indexOf("?");
			if (idx != -1) {
				path = uri.substring(0, idx);
				queryString = uri.substring(idx + 1);
			}
			env.set(Env.PATH_INFO, path);
			env.set(Env.QUERY_STRING, queryString);

			Header hostHeader = request.getFirstHeader("Host");
			if (hostHeader != null) {
				String host = hostHeader.getValue();
				String parts[] = host.split(":");
				env.set(Env.SERVER_NAME, parts[0]);
				int port = 80;
				if (parts.length > 1) {
					port = Integer.parseInt(parts[1]);
				}
				env.set(Env.SERVER_PORT, port);
			}
			env.set(Env.REMOTE_ADDR, context.getAttribute(Env.REMOTE_ADDR));
			env.set(Env.RACK_URL_SCHEME, "http");

			StringBuilder buf = new StringBuilder(64);
			buf.append("HTTP_");
			for (Header header : request.getAllHeaders()) {
				String name = header.getName();
				String value = header.getValue();
				String normalizedName = name.replace('-', '_').toUpperCase();

				if (Env.CONTENT_LENGTH.equals(normalizedName)
						|| Env.CONTENT_TYPE.equals(normalizedName)) {
					env.set(normalizedName, value);
				} else {
					buf.setLength(5);
					buf.append(normalizedName);

					env.set(buf.toString(), value);
				}
			}

			InputStream in = null;
			if (request instanceof HttpEntityEnclosingRequest) {
				try {
					in = ((HttpEntityEnclosingRequest) request).getEntity()
							.getContent();
				} catch (Exception e) {
					throw new RackException(e);
				}
			} else {
				in = new ByteArrayInputStream(new byte[0]);
			}
			env.set(Env.RACK_INPUT, in);

			return env;
		}
	}

	static class Server implements Runnable {

		private final ServerSocket serversocket;
		private final HttpParams params;
		private final HttpService httpService;

		private Thread thread;
		private volatile boolean stopped = false;

		private ExecutorService executor;

		public Server(InetSocketAddress addr, HttpRequestHandler handler,
				HandlerOptions options) throws IOException {
			serversocket = new ServerSocket();
			serversocket.bind(addr);

			params = new SyncBasicHttpParams();
			params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
					.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
							8 * 1024)
					.setBooleanParameter(
							CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
					.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
					.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "Rack/1.1");

			HttpProcessor httpproc = new ImmutableHttpProcessor(
					new HttpResponseInterceptor[] { /*
													 * new ResponseDate(), new
													 * ResponseServer(), new
													 * ResponseContent(),
													 */
					new ResponseConnControl() });

			HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
			registry.register("*", handler);

			httpService = new HttpService(httpproc,
					new DefaultConnectionReuseStrategy(),
					new DefaultHttpResponseFactory(), registry, this.params);

			int threadCore = options.getInt("thread.min", 10);
			int threadMax = options.getInt("thread.max", 200);
			int queueSize = options.getInt("queue.size", 5000);
			LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(
					queueSize);
			executor = new ThreadPoolExecutor(threadCore, threadMax, 5,
					TimeUnit.SECONDS, queue);
		}

		public void start() {
			thread = new Thread(this);
			thread.start();
		}

		public void stop() {
			if (thread == null) {
				return;
			}
			stopped = true;
			for (;;) {
				thread.interrupt();
				try {
					thread.join();
					break;
				} catch (InterruptedException e) {
				}
			}
		}

		public void run() {
			while (!stopped) {
				try {
					Socket socket = this.serversocket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					conn.bind(socket, this.params);

					Worker worker = new Worker(this.httpService, conn);
					executor.execute(worker);
				} catch (InterruptedIOException ex) {
				} catch (IOException e) {
					logger.error("I/O error initialising connection thread: "
							+ e.getMessage(), e);
					break;
				}
			}
		}
	}

	static class Worker implements Runnable {

		private final HttpService httpservice;
		private final SocketHttpServerConnection conn;

		public Worker(final HttpService httpservice,
				final SocketHttpServerConnection conn) {
			this.httpservice = httpservice;
			this.conn = conn;
		}

		public void run() {
			logger.info("accept new connection: " + conn);
			HttpContext context = new BasicHttpContext(null);
			try {
				context.setAttribute(Env.REMOTE_ADDR, conn.getRemoteAddress()
						.getHostAddress());

				while (!Thread.interrupted() && conn.isOpen()) {
					httpservice.handleRequest(this.conn, context);
				}
			} catch (ConnectionClosedException ex) {
				logger.error("Client closed connection");
			} catch (IOException ex) {
				logger.error("I/O error: " + ex.getMessage());
			} catch (HttpException ex) {
				logger.error("Unrecoverable HTTP protocol violation: "
						+ ex.getMessage());
			} finally {
				try {
					this.conn.shutdown();
				} catch (IOException ignore) {
				}
			}
		}

	}
}
