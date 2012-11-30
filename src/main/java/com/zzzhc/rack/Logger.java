package com.zzzhc.rack;

public class Logger implements IMiddleware {

	private IMiddleware app;

	public Logger(IMiddleware app) {
		this.app = app;
	}

	public Response call(Env env) {
		org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("rack");
		env.set(Env.RACK_LOGGER, logger);

		return app.call(env);
	}

}
