package com.zzzhc.routing;

import com.zzzhc.rack.Env;

public interface ICondition {

	boolean isMatched(Env env);

}
