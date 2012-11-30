package com.zzzhc.routing.condition;

import java.util.ArrayList;
import java.util.List;

import com.zzzhc.rack.Env;
import com.zzzhc.routing.ICondition;

public class MultiCondition implements ICondition {
	private List<ICondition> conditions = new ArrayList<ICondition>();

	public void addCondition(ICondition condition) {
		conditions.add(condition);
	}

	public boolean isMatched(Env env) {
		for (ICondition condition : conditions) {
			if (!condition.isMatched(env)) {
				return false;
			}
		}
		return true;
	}

}
