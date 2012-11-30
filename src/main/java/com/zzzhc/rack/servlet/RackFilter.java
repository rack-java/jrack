package com.zzzhc.rack.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.Response;

public class RackFilter implements Filter {
	private IMiddleware app;

	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO load app
	}

	public void doFilter(ServletRequest request,
			ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		Env env = ServletAdapter.toEnv((HttpServletRequest) request);
		Response response = app.call(env);
		if (response.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
			chain.doFilter(request, servletResponse);
		} else {
			ServletAdapter.writeResponse(response,
					(HttpServletResponse) servletResponse);
		}
	}

	public void destroy() {
	}

}
