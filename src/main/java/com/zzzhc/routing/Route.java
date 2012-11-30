package com.zzzhc.routing;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.IMiddleware;

public class Route {

	private ICondition condition;
	private IMiddleware app;

	public Route(ICondition condition, IMiddleware app) {
		this.condition = condition;
		this.app = app;
	}

	public boolean isMatched(Env env) {
		return condition.isMatched(env);
	}

	public IMiddleware getApp() {
		return app;
	}
}
