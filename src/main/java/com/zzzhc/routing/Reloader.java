package com.zzzhc.routing;

import java.io.File;
import java.net.MalformedURLException;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.RackException;
import com.zzzhc.rack.Response;

public class Reloader implements IMiddleware {

	private ReloadableClassLoader loader;
	private IMiddleware app;

	public Reloader(IMiddleware app) {
		this.app = app;
		loader = new ReloadableClassLoader();
		try {
			// TODO
			loader.addPath(new File("bin").toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RackException(e);
		}
	}

	public Response call(Env env) {
		Thread current = Thread.currentThread();
		ClassLoader oldClassLoader = current.getContextClassLoader();
		try {
			loader.detectChange();
			current.setContextClassLoader(loader);
			env.set("rack.classloader", loader);

			return app.call(env);
		} finally {
			current.setContextClassLoader(oldClassLoader);
		}
	}

}
