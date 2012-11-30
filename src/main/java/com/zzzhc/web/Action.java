package com.zzzhc.web;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.RackException;
import com.zzzhc.rack.Request;
import com.zzzhc.rack.Response;

public class Action implements IMiddleware {

	private Class<?> klass;
	private Method method;

	private ITemplateEngine templateEngine;

	public Action(Class<?> klass, Method method, ITemplateEngine templateEngine) {
		this.klass = klass;
		this.method = method;
		this.templateEngine = templateEngine;
	}

	private Object initializeController(Env env) {
		try {
			Object controller = klass.newInstance();
			return controller;
		} catch (Exception e) {
			throw new RackException(e);
		}
	}

	public Response call(Env env) {
		Object controller = initializeController(env);
		View view = new View(env, controller, method, templateEngine);
		env.set("rack.view", view);

		Request request = (Request) env.get("rack.request");
		if (request == null) {
			request = new Request(env);
			env.set("rack.request", request);
		}

		try {
			if (controller instanceof IEnvAware) {
				((IEnvAware) controller).setEnv(env);
				method.invoke(controller);
			} else {
				method.invoke(controller, env);
			}
		} catch (IllegalArgumentException e) {
			throw new RackException(e);
		} catch (IllegalAccessException e) {
			throw new RackException(e);
		} catch (InvocationTargetException e) {
			throw new RackException(e);
		}

		if (!view.isRendered()) {
			view.render();
		}

		return view.toResponse();
	}

}
