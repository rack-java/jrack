package com.zzzhc.web;

import java.io.File;

import org.apache.commons.jci.ReloadingClassLoader;
import org.apache.commons.jci.listeners.ReloadNotificationListener;
import org.apache.commons.jci.listeners.ReloadingListener;
import org.apache.commons.jci.monitor.FilesystemAlterationMonitor;
import org.apache.commons.jci.stores.FileResourceStore;

import com.zzzhc.rack.Builder;
import com.zzzhc.rack.Env;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.RackEnv;
import com.zzzhc.rack.Response;
import com.zzzhc.web.template.FreeMarkerTemplateEngine;
import com.zzzhc.web.template.JadeTemplateEngine;
import com.zzzhc.web.template.MultiTemplateEngine;

public class Application implements IMiddleware {

	protected RackEnv rackEnv;
	protected Builder builder = new Builder();
	protected MultiTemplateEngine templateEngine = new MultiTemplateEngine();

	protected RouteSetBuilder routeSetBuilder;
	protected volatile boolean changed;
	protected ReloadingClassLoader loader;

	protected IMiddleware app;

	private String packageName;

	public void setRackEnv(RackEnv rackEnv) {
		this.rackEnv = rackEnv;
	}

	public String getPackageName() {
		if (packageName == null) {
			String name = getClass().getName();
			packageName = name.replaceFirst("\\.?[^.]+$", "");
		}
		return packageName;
	}

	public String getViewsPath() {
		return "/" + getPackageName().replace('.', '/');
	}

	protected void prepare() {
		loader = new ReloadingClassLoader(ClassLoader.getSystemClassLoader());
		loader.addResourceStore(new FileResourceStore(new File("bin")));
		loader.addResourceStore(new FileResourceStore(
				new File("target/classes")));
		ReloadingListener listener = new ReloadingListener();

		listener.addReloadNotificationListener(loader);
		listener.addReloadNotificationListener(new ReloadNotificationListener() {

			public void handleNotification() {
				Application.this.changed = true;
			}
		});

		FilesystemAlterationMonitor fam = new FilesystemAlterationMonitor();
		fam.setInterval(1000);
		fam.addListener(new File("src/main/java"), listener);// TODO
		fam.start();

		routeSetBuilder = new RouteSetBuilder(templateEngine, loader);
		templateEngine.addTemplateEngine(new FreeMarkerTemplateEngine());
		templateEngine.addTemplateEngine(new JadeTemplateEngine());
		routeSetBuilder.addPackage(this.getClass());
	}

	public void initialize() {
		prepare();
		templateEngine.setup(this);
		builder.run(new RouteWrapper());
		app = builder.toApp();
	}

	public Response call(Env env) {
		return app.call(env);
	}

	private class RouteWrapper implements IMiddleware {
		private IMiddleware app;

		private IMiddleware getApp() {
			if (app == null || changed) {
				System.out.println("reload!!");
				app = routeSetBuilder.build();
				changed = false;
			}
			return app;
		}

		public Response call(Env env) {
			Thread current = Thread.currentThread();
			ClassLoader old = current.getContextClassLoader();
			try {
				current.setContextClassLoader(loader);
				return getApp().call(env);
			} finally {
				current.setContextClassLoader(old);
			}
		}

	}

}
