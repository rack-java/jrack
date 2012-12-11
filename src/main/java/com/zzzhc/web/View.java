package com.zzzhc.web;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.MultiBody;
import com.zzzhc.rack.RackException;
import com.zzzhc.rack.Response;

public class View {

	private Env env;
	private Object controller;
	private Method action;
	private ITemplateEngine templateEngine;

	private int status;
	private Map<String, String> headers = new HashMap<String, String>();
	private MultiBody body = new MultiBody();

	public View(Env env, Object controller, Method action,
			ITemplateEngine templateEngine) {
		this.env = env;
		this.controller = controller;
		this.action = action;
		this.templateEngine = templateEngine;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setHeader(String name, String value) {
		headers.put(name, value);
	}

	private String getDefaultTemplate() {
		String controllerName = controller.getClass().getName();
		controllerName = underscore(controllerName.replaceFirst("Controller$",
				""));
		String path = controllerName + "." + underscore(action.getName());
		return path.replace('.', '/');
	}

	public void render() {
		render(200, getDefaultTemplate());
	}

	public void render(int status, String template) {
		setStatus(status);
		// TODO, locate template with other suffix, e.g. *.html.ftl
		// String ftlTemplate = "/" + template + ".html.ftl";
		String value = templateEngine.render(env, template);
		if (value != null) {
			body.add(value);
		} else {
			// TODO
		}
	}

	public void renderText(String text) {
		renderText(200, text);
	}

	public void renderText(int status, String text) {
		setStatus(status);
		body.add(text);
	}

	public void renderJson(Object data) {
		renderJson(200, data);
	}

	public void renderJson(int status, Object data) {
		setStatus(status);
		StringWriter writer = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(writer, data);
		} catch (Exception e) {
			throw new RackException(e);
		}
		body.add(writer.toString());
	}

	public boolean isRendered() {
		return !body.isEmpty();
	}

	public Response toResponse() {
		return new Response(status, headers, body);
	}

	private String underscore(String s) {
		return s.replaceAll("([^.])([A-Z])", "$1_$2").toLowerCase();
	}

}
