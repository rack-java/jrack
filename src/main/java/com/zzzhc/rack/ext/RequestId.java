package com.zzzhc.rack.ext;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.IMiddleware;
import com.zzzhc.rack.RackException;
import com.zzzhc.rack.Response;

public class RequestId implements IMiddleware {
	private IMiddleware app;

	public RequestId(IMiddleware app) {
		this.app = app;
	}

	public Response call(Env env) {
		String requestId = getRequestId(env);
		env.set("rackx.request_id", requestId);
		Response response = app.call(env);
		response.setHeader("X-Request-Id", requestId);
		return response;
	}

	private String getRequestId(Env env) {
		String requestId = externalRequestId(env);
		if (requestId == null) {
			return internalRequestId();
		}
		return requestId;
	}

	private String externalRequestId(Env env) {
		String requestId = env.getString("HTTP_X_REQUEST_ID", "");
		if (requestId.length() > 0) {
			requestId = requestId.replaceAll("[^\\w\\-]", "");
			if (requestId.length() > 255) {
				requestId = requestId.substring(0, 255);
			}
			return requestId;
		}
		return null;
	}

	private String internalRequestId() {
		try {
			byte[] bytes = new byte[16];
			SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
			StringBuilder ss = new StringBuilder(32);
			for (byte b : bytes) {
				int i = b & 0xff;
				String hex = Integer.toHexString(i);
				if (hex.length() == 1) {
					ss.append('0');
				}
				ss.append(hex);
			}
			return ss.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RackException(e);
		}

	}
}
