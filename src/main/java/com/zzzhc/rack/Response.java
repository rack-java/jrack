package com.zzzhc.rack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Response {

	private final int status;
	private final Map<String, String> headers = new HashMap<String, String>();
	private final IBody body;

	public Response(int status, IBody body) {
		this(status, null, body);
	}

	public Response(int status, Map<String, String> headers, IBody body) {
		this.status = status;
		if (headers != null) {
			this.headers.putAll(headers);
		}
		this.body = body;
	}

	public int getStatus() {
		return status;
	}

	public IBody getBody() {
		return body;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Set<String> getHeaderNames() {
		return headers.keySet();
	}

	public String getHeader(String name) {
		return headers.get(name);
	}

	public void setHeader(String name, String value) {
		headers.put(name, value);
	}

}
