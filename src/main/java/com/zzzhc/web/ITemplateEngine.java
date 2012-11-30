package com.zzzhc.web;

import com.zzzhc.rack.Env;

public interface ITemplateEngine {
	void setup(Application app);

	String render(Env env, String template);
}
