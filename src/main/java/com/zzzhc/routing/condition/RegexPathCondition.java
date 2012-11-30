package com.zzzhc.routing.condition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zzzhc.rack.Env;
import com.zzzhc.routing.ICondition;

public class RegexPathCondition implements ICondition {

	private Pattern regex;

	public RegexPathCondition(String regex) {
		this.regex = Pattern.compile(regex);
	}

	public RegexPathCondition(Pattern regex) {
		this.regex = regex;
	}

	public boolean isMatched(Env env) {
		String path = env.getString(Env.PATH_INFO);
		Matcher m = regex.matcher(path);
		if (m.matches()) {
			// TODO, extract params?
			return true;
		}
		return false;
	}

}
