package com.zzzhc.rack;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;

public class Builder {

	private static class Tuple {
		Class<? extends IMiddleware> klass;
		IChainableMiddleware app;
	}

	private final List<Tuple> chain;
	private IMiddleware run;

	private final Map<String, IMiddleware> map;

	public Builder() {
		chain = new ArrayList<Tuple>();
		map = new HashMap<String, IMiddleware>();
	}

	public Builder(IMiddleware defaultApp) {
		this();
		run(defaultApp);
	}

	private boolean isChainableMiddleware(Class<? extends IMiddleware> klass) {
		for (Class<?> k : klass.getInterfaces()) {
			if (k == IChainableMiddleware.class) {
				return true;
			}
		}
		try {
			klass.getConstructor(IMiddleware.class);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public void use(Class<? extends IMiddleware> klass) {
		if (!isChainableMiddleware(klass)) {
			throw new IllegalArgumentException(
					klass
							+ " must have a constructor accept a IMiddleware or implement IChainableMiddleware");
		}
		Tuple tuple = new Tuple();
		tuple.klass = klass;
		chain.add(tuple);
	}

	public void use(IChainableMiddleware app) {
		Tuple tuple = new Tuple();
		tuple.app = app;
		chain.add(tuple);
	}

	public void run(IMiddleware run) {
		this.run = run;
	}

	public void map(String path, IMiddleware app) {
		map.put(path, app);
	}

	public void map(String path, Builder builder) {
		map(path, builder.toApp());
	}

	public IMiddleware toApp() {
		IMiddleware app = run;
		if (map.size() > 0) {
			app = generateMap(run, map);
		}
		for (int i = chain.size() - 1; i >= 0; i--) {
			Tuple tuple = chain.get(i);
			if (tuple.klass != null) {
				Class<? extends IMiddleware> klass = tuple.klass;
				try {
					Constructor<? extends IMiddleware> c = klass
							.getConstructor(IMiddleware.class);
					app = c.newInstance(app);
				} catch (Exception e) {
					throw new RackException(e);
				}
			} else {
				tuple.app.setApp(app);
				app = tuple.app;
			}
		}
		return app;
	}

	private IMiddleware generateMap(IMiddleware defaultApp,
			Map<String, IMiddleware> mapping) {
		Map<String, IMiddleware> mapped = new HashMap<String, IMiddleware>();
		mapped.put("/", defaultApp);
		mapped.putAll(mapping);
		return new URLMap(mapped);
	}

	public static Builder load(File config) throws IOException {
		String text = FileUtils.readFileToString(config);
		Builder builder = new Builder();

		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("javascript");
		engine.put("builder", builder);
		try {
			engine.eval(text);
		} catch (ScriptException e) {
			throw new RackException(e);
		}

		return builder;
	}

}
