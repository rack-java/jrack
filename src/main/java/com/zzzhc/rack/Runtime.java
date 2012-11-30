package com.zzzhc.rack;

public class Runtime extends BaseChainableMiddleware {

	public static final String HEADER_NAME = "X-Runtime";

	public Response call(Env env) {
		long start = System.nanoTime();
		Response response = app.call(env);
		long end = System.nanoTime();
		double delta = (end - start) / 1000000000.0;
		response.setHeader(HEADER_NAME, String.format("%6f", delta));
		return null;
	}

}
