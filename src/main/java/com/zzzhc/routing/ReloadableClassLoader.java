package com.zzzhc.routing;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ReloadableClassLoader extends ClassLoader {
	private final List<URL> classPaths;
	private URLClassLoader delegator;

	private volatile long newestTimestamp;
	private volatile boolean changed = true;

	public ReloadableClassLoader() {
		classPaths = new ArrayList<URL>();
	}

	public void addPath(URL url) {
		classPaths.add(url);
	}

	public synchronized void detectChange() {
		if (changed) {
			return;
		}

		long maxLastModified = 0;
		for (URL url : classPaths) {
			File file = new File(url.getFile());
			maxLastModified = Math.max(maxLastModified,
					getMaxLastModified(file));
		}
		if (maxLastModified > newestTimestamp) {
			changed = true;
			newestTimestamp = maxLastModified;
		}
	}

	private long getMaxLastModified(File file) {
		if (file.isDirectory()) {
			long max = 0;
			for (File f : file.listFiles()) {
				max = Math.max(max, getMaxLastModified(f));
			}
			return max;
		} else if (file.isFile()) {
			return file.lastModified();
		} else {
			return 0;
		}
	}

	private boolean isChanged() {
		return changed;
	}

	private URLClassLoader getDelegator() {
		if (isChanged()) {
			changed = false;
			URL[] urls = classPaths.toArray(new URL[classPaths.size()]);
			delegator = new URLClassLoader(urls);
		}
		return delegator;
	}

	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return getDelegator().loadClass(name);
	}

}
