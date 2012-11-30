package com.zzzhc.rack;

public interface IHandler {

	void start(IMiddleware app, HandlerOptions options) throws Exception;

	void stop();

}
