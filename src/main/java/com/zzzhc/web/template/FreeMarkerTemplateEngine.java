package com.zzzhc.web.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.RackException;
import com.zzzhc.web.Application;
import com.zzzhc.web.ITemplateEngine;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreeMarkerTemplateEngine implements ITemplateEngine {

	private Configuration cfg;

	public void setup(Application app) {
		// ClassTemplateLoader ctl = new ClassTemplateLoader(app.getClass(),
		// app.getViewsPath());
		ClassTemplateLoader ctl = new ClassTemplateLoader(app.getClass(), "/");
		cfg = new Configuration();
		cfg.setTemplateLoader(ctl);
		cfg.setLocalizedLookup(false);
	}

	public String render(Env env, String template) {
		try {
			Template t = cfg.getTemplate(template);
			Map<String, Object> rootMap = new HashMap<String, Object>();
			rootMap.put("env", env);
			rootMap.put("request", env.get("rack.request"));
			// TODO
			StringWriter out = new StringWriter(1024);
			t.process(rootMap, out);
			return out.toString();
		} catch (IOException e) {
			throw new RackException(e);
		} catch (TemplateException e) {
			throw new RackException(e);
		}
	}
}
