package com.zzzhc.rack.multipart;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.RequestContext;

import com.zzzhc.rack.Env;

public class RackRequestContext implements RequestContext {

	private final Env env;

	public RackRequestContext(Env env) {
		this.env = env;
	}

	public String getCharacterEncoding() {
		return env.getContentCharset();
	}

	public String getContentType() {
		return env.getContentType();
	}

	public int getContentLength() {
		return env.getContentLength();
	}

	public InputStream getInputStream() throws IOException {
		return (InputStream) env.get(Env.RACK_INPUT);
	}

}
