package com.zzzhc.rack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLMap implements IMiddleware {

	private static class Tuple {
		final String host;
		final String location;
		final Pattern pattern;
		final IMiddleware app;

		public Tuple(String host, String location, Pattern pattern,
				IMiddleware app) {
			this.host = host;
			this.location = location;
			this.pattern = pattern;
			this.app = app;
		}
	}

	public static final Pattern LOCATION_PATTERN = Pattern
			.compile("\\Ahttps?://(.*?)(/.*)");

	private List<Tuple> mapping;

	public URLMap(Map<String, IMiddleware> map) {
		mapping = new ArrayList<Tuple>();
		remap(map);
	}

	private void remap(Map<String, IMiddleware> map) {
		for (Map.Entry<String, IMiddleware> entry : map.entrySet()) {
			String location = entry.getKey();
			IMiddleware app = entry.getValue();

			String host = null;
			Matcher m = LOCATION_PATTERN.matcher(location);
			if (m.find()) {
				host = m.group(1);
				location = m.group(2);
			}

			if (location.length() == 0 || location.charAt(0) != '/') {
				throw new IllegalArgumentException("paths need to start with /");
			}

			location = location.replaceAll("/$", "");
			String re = Pattern.quote(location).replaceAll("/", "/+");
			Pattern pattern = Pattern.compile(re + "(.*)");

			mapping.add(new Tuple(host, location, pattern, app));
		}

		Collections.sort(mapping, new Comparator<Tuple>() {

			public int compare(Tuple o1, Tuple o2) {
				int h1 = o1.host != null ? o1.host.length() : Integer.MIN_VALUE;
				int h2 = o2.host != null ? o2.host.length() : Integer.MIN_VALUE;
				if (h1 != h2) {
					return h1 - h2;
				}

				return o1.location.compareTo(o2.location);
			}

		});
	}

	public Response call(Env env) {
		String scriptName = (String) env.get(Env.SCRIPT_NAME);
		String path = (String) env.get(Env.PATH_INFO);

		try {
			for (Tuple tuple : mapping) {
				if (!isHostMatch(env, tuple)) {
					continue;
				}

				Matcher m = tuple.pattern.matcher(path);
				if (!m.find()) {
					continue;
				}
				String rest = m.group(1);
				if (rest.isEmpty() || rest.charAt(0) == '/') {
					env.set(Env.SCRIPT_NAME, scriptName + tuple.location);
					env.set(Env.PATH_INFO, rest);
					return tuple.app.call(env);
				}
			}
		} finally {
			env.set(Env.SCRIPT_NAME, scriptName);
			env.set(Env.PATH_INFO, path);
		}
		return new Response(404, new MultiBody());
	}

	private boolean isHostMatch(Env env, Tuple tuple) {
		String httpHost = (String) env.get("HTTP_HOST");
		String serverName = (String) env.get(Env.SERVER_NAME);
		int serverPort = (Integer) env.get(Env.SERVER_PORT);

		String host = tuple.host;
		if (host != null) {
			if (host.equals(httpHost)) {
				return true;
			}
			if (host.equals(serverName)) {
				return true;
			}
		} else {
			if (httpHost == null || serverName == null) {
				return true;
			}
			if ((httpHost.equals(serverName) || httpHost.equals(serverName
					+ ":" + serverPort))) {
				return true;
			}
		}
		return false;
	}
}
