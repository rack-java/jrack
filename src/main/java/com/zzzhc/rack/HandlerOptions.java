package com.zzzhc.rack;

import java.util.HashMap;
import java.util.Map;

public class HandlerOptions {

	private String host = "0.0.0.0";
	private int port = 9292;
	private String env = "development";

	private Map<String, String> options = new HashMap<String, String>();

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public void addOption(String name, String value) {
		options.put(name, value);
	}

	public String getOption(String name) {
		return options.get(name);
	}

	public int getInt(String name, int defaultValue) {
		String value = options.get(name);
		if (value == null) {
			return defaultValue;
		}
		return Integer.parseInt(value);
	}
}
