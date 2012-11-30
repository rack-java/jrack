package com.zzzhc.rack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Env {
	public static final String DEFAULT_ENCODING = "UTF-8";

	public static final String REQUEST_METHOD = "REQUEST_METHOD";

	public static final String SCRIPT_NAME = "SCRIPT_NAME";

	public static final String PATH_INFO = "PATH_INFO";

	public static final String QUERY_STRING = "QUERY_STRING";

	public static final String SERVER_NAME = "SERVER_NAME";

	public static final String SERVER_PORT = "SERVER_PORT";

	public static final String REMOTE_ADDR = "REMOTE_ADDR";

	public static final String CONTENT_LENGTH = "CONTENT_LENGTH";

	public static final String CONTENT_TYPE = "CONTENT_TYPE";

	public static final String RACK_VERSION = "rack.version";

	public static final String RACK_URL_SCHEME = "rack.url_scheme";

	public static final String RACK_INPUT = "rack.input";

	public static final String RACK_INPUT_RAW = "rack.input.raw";

	public static final String RACK_ERRORS = "rack.errors";

	public static final String RACK_MULTITHREAD = "rack.multithread";

	public static final String RACK_MULTIPROCESS = "rack.multiprocess";

	public static final String RACK_RUN_ONCE = "rack.run_once";

	public static final String RACK_SESSION = "rack.session";

	public static final String RACK_LOGGER = "rack.logger";

	public static final int[] RACK_VERSION_VALUE = new int[] { 1, 1 };

	private static final Pattern HEADER_SPLIT_PATTERN = Pattern
			.compile("\\s*[;,]\\s*");

	private static final String MULTIPART = "multipart/";

	private final Map<String, Object> data;

	public Env() {
		data = new HashMap<String, Object>();

		set(RACK_VERSION, RACK_VERSION_VALUE);
		set(RACK_MULTITHREAD, true);
		set(RACK_MULTIPROCESS, false);
		set(RACK_RUN_ONCE, true);
		set(RACK_ERRORS, System.err);
	}

	public Object get(String key) {
		return data.get(key);
	}

	public void set(String key, Object value) {
		data.put(key, value);
	}

	public Set<String> keys() {
		return data.keySet();
	}

	public String getString(String key) {
		return getString(key, null);
	}

	public String getString(String key, String defaultValue) {
		Object value = data.get(key);
		if (value == null) {
			return defaultValue;
		}
		return value.toString();
	}

	public int getContentLength() {
		String s = getString(Env.CONTENT_LENGTH);
		if (s == null) {
			return -1;
		}
		return Integer.parseInt(s);
	}

	public String getContentType() {
		String contentType = getString(Env.CONTENT_TYPE);
		if (contentType == null || contentType.isEmpty()) {
			return null;
		}
		return contentType;
	}

	public String getMediaType() {
		String contentType = getContentType();
		if (contentType == null) {
			return null;
		}
		String[] parts = HEADER_SPLIT_PATTERN.split(contentType, 2);
		return parts[0].toLowerCase();
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getMediaTypeParams() {
		Object old = get("rack.request.media_type_params");
		if (old != null) {
			return (Map<String, String>) old;
		}
		Map<String, String> params = new HashMap<String, String>();
		set("rack.request.media_type_params", params);
		String contentType = getContentType();
		if (contentType == null) {
			return params;
		}
		String[] parts = HEADER_SPLIT_PATTERN.split(contentType);
		for (int i = 1; i < parts.length; i++) {
			String kv[] = Utils.splitKeyValue(parts[i]);
			params.put(kv[0].toLowerCase(), kv[1]);
		}
		return params;
	}

	public String getContentCharset() {
		String charset = getString("rack.content.charset");
		if (charset != null) {
			return charset;
		}
		charset = getMediaTypeParams().get("charset");
		if (charset == null) {
			return DEFAULT_ENCODING;
		}
		return charset;
	}

	public boolean isMultipartContent() {
		if (!"POST".equals(get(Env.REQUEST_METHOD))) {
			return false;
		}
		String contentType = getContentType();
		if (contentType == null) {
			return false;
		}
		if (contentType.toLowerCase().startsWith(MULTIPART)) {
			return true;
		}
		return false;
	}
}
