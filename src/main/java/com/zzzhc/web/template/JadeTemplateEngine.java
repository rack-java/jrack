package com.zzzhc.web.template;

import java.util.HashMap;
import java.util.Map;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.RackException;
import com.zzzhc.web.Application;
import com.zzzhc.web.ITemplateEngine;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.FileTemplateLoader;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.TemplateLoader;

public class JadeTemplateEngine implements ITemplateEngine {

	private JadeConfiguration config;

	@Override
	public void setup(Application app) {
		config = new JadeConfiguration();
		TemplateLoader loader = new FileTemplateLoader("src/main/views",
				"UTF-8");//TODO, remove hard code path
		config.setTemplateLoader(loader);
		config.setMode(Jade4J.Mode.HTML);
	}

	@Override
	public String render(Env env, String template) {
		String jadeTemplate = "/" + template + ".html.jade";
		try {
			JadeTemplate t = config.getTemplate(jadeTemplate);

			Map<String, Object> context = new HashMap<String, Object>();
			context.put("env", env);
			context.put("request", env.get("rack.request"));

			return config.renderTemplate(t, context);
		} catch (Exception e) {
			throw new RackException(e);
		}
	}

}
