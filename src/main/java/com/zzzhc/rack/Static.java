package com.zzzhc.rack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Static extends BaseChainableMiddleware {
	private File root;
	private String cacheControl;

	private StaticFile staticFile;

	private String index;
	private List<String> urls = new ArrayList<String>();
	private Map<String, String> overwriteRules = new HashMap<String, String>();

	public Static(String root) {
		this(root, null);
	}

	public Static(String root, String cacheControl) {
		this.root = new File(root);
		this.cacheControl = cacheControl;

		staticFile = new StaticFile(this.root, this.cacheControl);
	}

	public void setIndex(String index) {
		this.index = index;
	}
	
	public void addUrl(String url) {
		urls.add(url);
	}
	
	public void addOverwriteRule(String url, String local) {
		overwriteRules.put(url, local);
	}

	private boolean overwriteFilePath(String path) {
		return overwriteRules.containsKey(path)
				|| (index != null && "/".equals(path));
	}

	private boolean routeFile(String path) {
		for (String url : urls) {
			if (path.startsWith(url)) {
				return true;
			}
		}
		return false;
	}

	private boolean canServe(String path) {
		return overwriteFilePath(path) || routeFile(path);
	}

	public Response call(Env env) {
		String path = env.getString(Env.PATH_INFO);
		if (!canServe(path)) {
			return app.call(env);
		}

		if (overwriteFilePath(path)) {
			String overwritePath = overwriteRules.get(path);
			if (overwritePath == null) {
				overwritePath = index;
			}
			env.set(Env.PATH_INFO, overwritePath);
		}
		return staticFile.call(env);
	}
}
