package com.zzzhc.routing;

import java.util.ArrayList;
import java.util.List;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.MultiBody;
import com.zzzhc.rack.Response;

public class RouteSet implements IMiddleware {

	private List<Route> routes = new ArrayList<Route>();

	public void addRoute(Route route) {
		routes.add(route);
	}

	public Response call(Env env) {
		Route route = findMatchedRoute(env);
		if (route == null) {
			return notFound();
		}
		return route.getApp().call(env);
	}

	private Route findMatchedRoute(Env env) {
		for (Route route : routes) {
			if (route.isMatched(env)) {
				return route;
			}
		}
		return null;
	}

	private Response notFound() {
		Response response = new Response(404, new MultiBody("Not Found"));
		response.setHeader("Content-Type", "text/html");
		response.setHeader("X-Cascade", "pass");
		return response;
	}

}
