package com.zzzhc.web;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.Request;

/**
 * annotations:<br>
 * controller/action level:<br>
 * layout<br>
 * before<br>
 * after<br>
 */
public class BaseController implements IEnvAware {

	protected Env env;
	protected Request request;
	protected View view;

	public void setEnv(Env env) {
		this.env = env;
		view = (View) env.get("rack.view");
		request = (Request) env.get("rack.request");
	}

}
