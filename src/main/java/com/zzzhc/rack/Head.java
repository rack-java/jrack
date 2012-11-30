package com.zzzhc.rack;

public class Head implements IMiddleware {
	private IMiddleware app;

	public Head(IMiddleware app) {
		this.app = app;
	}

	public Response call(Env env) {
		Response response = app.call(env);
		if ("HEAD".equals(env.get(Env.REQUEST_METHOD))) {
			return new Response(response.getStatus(), response.getHeaders(),
					new IBody.EmptyBody());
		}
		return response;
	}

}
