package com.zzzhc.routing.condition;

import com.zzzhc.rack.Env;
import com.zzzhc.routing.ICondition;

public class PathCondition implements ICondition {

	private String path;
	
	public PathCondition(String path) {
		this.path = path;
	}

	public boolean isMatched(Env env) {
		String pathInfo = env.getString(Env.PATH_INFO);
		return pathInfo.equals(path);
	}
	
}
