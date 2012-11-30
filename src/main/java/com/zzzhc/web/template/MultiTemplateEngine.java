package com.zzzhc.web.template;

import java.util.ArrayList;
import java.util.List;

import com.zzzhc.rack.Env;
import com.zzzhc.web.Application;
import com.zzzhc.web.ITemplateEngine;

public class MultiTemplateEngine implements ITemplateEngine {
	private List<ITemplateEngine> engines = new ArrayList<ITemplateEngine>();

	public void addTemplateEngine(ITemplateEngine engine) {
		engines.add(engine);
	}

	public void setup(Application app) {
		for (ITemplateEngine engine : engines) {
			engine.setup(app);
		}
	}

	public String render(Env env, String template) {
		for (ITemplateEngine engine : engines) {
			String value = engine.render(env, template);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

}
