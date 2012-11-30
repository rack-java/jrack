package com.zzzhc.rack;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import com.zzzhc.rack.multipart.RackFileUpload;
import com.zzzhc.rack.multipart.UploadedFile;

public class Multipart extends BaseChainableMiddleware {
	public static final String RACK_MULTIPART = "rack.multipart";

	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseMultipart(Env env) {
		if (env.get(RACK_MULTIPART) != null) {
			return (Map<String, Object>) env.get(RACK_MULTIPART);
		}
		Map<String, Object> params = new HashMap<String, Object>();
		env.set(RACK_MULTIPART, params);
		if (!env.isMultipartContent()) {
			return params;
		}

		DiskFileItemFactory factory = new DiskFileItemFactory();
		RackFileUpload upload = new RackFileUpload(factory);
		List<FileItem> items = upload.parseRequest(env);

		for (FileItem item : items) {
			if (item.isFormField()) {
				String name = item.getFieldName();
				String value;
				try {
					String enc = env.getContentCharset();
					if (enc == null) {
						enc = Env.DEFAULT_ENCODING;
					}
					value = item.getString(enc);
				} catch (UnsupportedEncodingException e) {
					throw new RackException(e);
				}
				Utils.normalizeParams(params, name, value);
			} else {
				UploadedFile file = new UploadedFile(item);
				Utils.normalizeParams(params, item.getFieldName(), file);
			}
		}
		return params;
	}

	public Response call(Env env) {
		parseMultipart(env);
		return app.call(env);
	}
}
