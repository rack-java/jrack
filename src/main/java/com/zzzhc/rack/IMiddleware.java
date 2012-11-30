package com.zzzhc.rack;

public interface IMiddleware {

	Response call(Env env);

}
