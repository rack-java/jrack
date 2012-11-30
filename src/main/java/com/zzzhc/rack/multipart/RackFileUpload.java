package com.zzzhc.rack.multipart;

import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;

import com.zzzhc.rack.Env;
import com.zzzhc.rack.RackException;

public class RackFileUpload extends FileUpload {

	public RackFileUpload() {
		super();
	}

	public RackFileUpload(FileItemFactory fileItemFactory) {
		super(fileItemFactory);
	}

	@SuppressWarnings("unchecked")
	public List<FileItem> parseRequest(Env env) {
		try {
			List<FileItem> items = parseRequest(new RackRequestContext(env));
			return items;
		} catch (FileUploadException e) {
			throw new RackException(e);
		}
	}

}
