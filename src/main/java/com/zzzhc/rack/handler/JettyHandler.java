package com.zzzhc.rack.handler;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzzhc.rack.HandlerOptions;
import com.zzzhc.rack.IHandler;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.servlet.ServletAdapter;

public class JettyHandler implements IHandler {
	private Logger logger = LoggerFactory.getLogger(JettyHandler.class);

	private Server server;

	public void start(IMiddleware app, HandlerOptions options) throws Exception {
		InetSocketAddress addr = new InetSocketAddress(options.getHost(),
				options.getPort());
		server = new Server(addr);
		server.setHandler(new RackAdapter(app));
		server.start();
	}

	public void stop() {
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				logger.error("stop jetty failed: " + e.getMessage(), e);
			}
			server = null;
		}
	}

	static class RackAdapter extends AbstractHandler {
		private IMiddleware app;

		public RackAdapter(IMiddleware app) {
			this.app = app;
		}

		public void handle(String target, Request baseRequest,
				HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			ServletAdapter.call(request, response, app);
		}

	}

}
