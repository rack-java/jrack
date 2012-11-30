package com.zzzhc.rack;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.zzzhc.rack.multipart.UploadedFile;

public class Request {
	private static final List<String> PARSEABLE_DATA_MEDIA_TYPES = new ArrayList<String>();
	private static final List<String> FORM_DATA_MEDIA_TYPES = new ArrayList<String>();

	static {
		PARSEABLE_DATA_MEDIA_TYPES.add("multipart/related");
		PARSEABLE_DATA_MEDIA_TYPES.add("multipart/mixed");
		FORM_DATA_MEDIA_TYPES.add("application/x-www-form-urlencoded");
		FORM_DATA_MEDIA_TYPES.add("multipart/form-data");
	}

	private final Env env;
	private Map<String, Object> params;

	public Request(Env env) {
		this.env = env;
	}

	public Env getEnv() {
		return env;
	}

	public String getScriptName() {
		return env.getString(Env.SCRIPT_NAME);
	}

	public String getPathInfo() {
		return env.getString(Env.PATH_INFO);
	}

	public String getRequestMethod() {
		return env.getString(Env.REQUEST_METHOD);
	}

	public String getQueryString() {
		return env.getString(Env.QUERY_STRING);
	}

	public int getContentLength() {
		return env.getContentLength();
	}

	public String getContentType() {
		return env.getContentType();
	}

	public String getMediaType() {
		return env.getMediaType();
	}

	public Map<String, String> getMediaTypeParams() {
		return env.getMediaTypeParams();
	}

	public String getContentCharset() {
		return env.getContentCharset();
	}

	public String getScheme() {
		if ("on".equals(env.get("HTTPS"))) {
			return "https";
		} else if ("on".equals(env.get("HTTP_X_FORWARDED_SSL"))) {
			return "https";
		} else if (env.get("HTTP_X_FORWARDED_SCHEME") != null) {
			return (String) env.get("HTTP_X_FORWARDED_SCHEME");
		} else if (env.get("HTTP_X_FORWARDED_PROTO") != null) {
			return (String) env.get("HTTP_X_FORWARDED_PROTO");
		} else {
			return (String) env.get(Env.RACK_URL_SCHEME);
		}
	}

	public boolean isSsl() {
		return "https".equals(getScheme());
	}

	public String getReferer() {
		return (String) env.get("HTTP_REFERER");
	}

	public String getUserAgent() {
		return (String) env.get("HTTP_USER_AGENT");
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getCookies() {
		return (Map<String, String>) env.get("rack.request.cookies");
	}

	public String getPath() {
		return getScriptName() + getPathInfo();
	}

	public boolean isXhr() {
		return "XMLHttpRequest".equals(env.get("HTTP_X_REQUESTED_WITH"));
	}

	public boolean isGet() {
		return "GET".equals(getRequestMethod());
	}

	public boolean isPost() {
		return "POST".equals(getRequestMethod());
	}

	public boolean isPut() {
		return "PUT".equals(getRequestMethod());
	}

	public boolean isDelete() {
		return "DELETE".equals(getRequestMethod());
	}

	public boolean isHead() {
		return "HEAD".equals(getRequestMethod());
	}

	public boolean isTrace() {
		return "TRACE".equals(getRequestMethod());
	}

	public boolean isPatch() {
		return "PATCH".equals(getRequestMethod());
	}

	public boolean isOptions() {
		return "OPTIONS".equals(getRequestMethod());
	}

	public boolean isFormData() {
		String type = getMediaType();
		String meth = env.getString("rack.methodoverride.original_method");
		if (meth == null) {
			meth = env.getString("REQUEST_METHOD");
		}
		return ("POST".equals(meth) && type == null)
				|| FORM_DATA_MEDIA_TYPES.contains(type);
	}

	public boolean isParseableData() {
		return PARSEABLE_DATA_MEDIA_TYPES.contains(getMediaType());
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> GET() {
		String qs = getQueryString();
		if (env.get("rack.request.query_string") == qs) {
			return (Map<String, Object>) env.get("rack.request.query_hash");
		} else {
			Map<String, Object> map = parseQuery(qs);
			env.set("rack.request.query_hash", map);
			env.set("rack.request.query_string", qs);
			return map;
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> POST() {
		if (env.get(Env.RACK_INPUT) == null) {
			throw new RackException("Missing rack.input");
		} else if (env.get("rack.request.form_input") == env.get("rack.input")) {
			return (Map<String, Object>) env.get("rack.request.form_input");
		} else {
			Map<String, Object> formMap = new HashMap<String, Object>();
			if (isFormData() || isParseableData()) {
				formMap = parseMultipart(env);
				if (formMap == null) {
					String formVars = readFormVars();
					formMap = parseQuery(formVars);
				}
			}
			env.set("rack.request.form_hash", formMap);
			env.set("rack.request.form_input", env.get("rack.input"));
			return formMap;
		}
	}

	private String readFormVars() {
		String old = env.getString("rack.request.form_vars");
		if (old != null) {
			return old;
		}
		InputStream in = (InputStream) env.get(Env.RACK_INPUT);
		try {
			String formVars = IOUtils.toString(in, env.getContentCharset());
			if (formVars.charAt(formVars.length() - 1) == 0) {
				formVars = formVars.substring(0, formVars.length() - 1);
			}
			env.set("rack.request.form_vars", formVars);
			return formVars;
		} catch (IOException e) {
			throw new RackException(e);
		}
	}

	private Map<String, Object> parseMultipart(Env env) {
		return Multipart.parseMultipart(env);
	}

	private Map<String, Object> parseQuery(String qs) {
		return Utils.parseNestedQuery(qs, Utils.DEFAULT_SEP);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getParams() {
		if (params == null) {
			params = new HashMap<String, Object>();

			Map<String, Object> routingArgs = (Map<String, Object>) env
					.get("rack.routing_args");
			if (routingArgs != null) {
				params.putAll(routingArgs);
			}

			params.putAll(GET());
			params.putAll(POST());
		}
		return params;
	}

	public Object getParam(String name) {
		return getParams().get(name);
	}

	public UploadedFile getFile(String name) {
		return (UploadedFile) getParams().get(name);
	}

}
