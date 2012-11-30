package com.zzzhc.routing.condition;

import com.zzzhc.rack.Env;
import com.zzzhc.routing.ICondition;

public class MatchAllCondition implements ICondition {

	public boolean isMatched(Env env) {
		return true;
	}

}
