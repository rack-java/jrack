package com.zzzhc.routing.condition;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.RequestMethod;
import com.zzzhc.routing.ICondition;

public class RequestMethodCondition implements ICondition {

	private RequestMethod method;

	public RequestMethodCondition(RequestMethod method) {
		this.method = method;
	}

	public boolean isMatched(Env env) {
		return method.name().equals(env.get(Env.REQUEST_METHOD));
	}

}
