package com.zzzhc.rack.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.IBody;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.RackException;
import com.zzzhc.rack.Response;

public final class ServletAdapter {

	public static Env toEnv(HttpServletRequest request) {
		try {
			Env env = new Env();

			env.set(Env.REQUEST_METHOD, request.getMethod());
			env.set(Env.SCRIPT_NAME, request.getServletPath());
			env.set(Env.PATH_INFO, request.getPathInfo());
			env.set(Env.QUERY_STRING, request.getQueryString());
			env.set(Env.SERVER_NAME, request.getServerName());
			env.set(Env.SERVER_PORT, request.getServerPort());
			env.set(Env.REMOTE_ADDR, request.getRemoteAddr());

			env.set(Env.RACK_URL_SCHEME, request.getScheme());

			Enumeration<String> names = request.getHeaderNames();
			StringBuilder buf = new StringBuilder(64);
			buf.append("HTTP_");
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				String value = request.getHeader(name);
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

			Map<String, String> cookies = new HashMap<String, String>();
			if (request.getCookies() != null) {
				for (Cookie cookie : request.getCookies()) {
					cookies.put(cookie.getName(), cookie.getValue());
				}
			}
			env.set("rack.request.cookies", cookies);

			Map<String, Object> params = new HashMap<String, Object>();
			params.putAll(request.getParameterMap());
			env.set("rack.request.params", params);

			InputStream in = request.getInputStream();

			env.set(Env.RACK_INPUT, in);
			return env;
		} catch (Exception e) {
			throw new RackException(e);
		}
	}

	public static void writeResponse(Response response,
			HttpServletResponse servletResponse) {
		servletResponse.setStatus(response.getStatus());
		for (String name : response.getHeaderNames()) {
			servletResponse.setHeader(name, response.getHeader(name));
		}

		try {
			IBody body = response.getBody();
			for (;;) {
				byte[] part = body.next();
				if (part == null) {
					break;
				}
				servletResponse.getOutputStream().write(part);
			}
			servletResponse.flushBuffer();
		} catch (IOException e) {
			throw new RackException(e);
		}
	}

	public static void call(HttpServletRequest request,
			HttpServletResponse servletResponse, IMiddleware app) {
		Env env = ServletAdapter.toEnv(request);
		Response response = app.call(env);
		ServletAdapter.writeResponse(response, servletResponse);
	}
}
