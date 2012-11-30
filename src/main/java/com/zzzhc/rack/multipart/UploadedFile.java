package com.zzzhc.rack.multipart;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;

public class UploadedFile {

	private final FileItem item;

	public UploadedFile(FileItem item) {
		this.item = item;
	}

	public String getOriginalFilename() {
		return item.getName();
	}

	public String getContentType() {
		return item.getContentType();
	}

	public InputStream getInputStream() throws IOException {
		return item.getInputStream();
	}
	
	public byte[] getData() {
		return item.get();
	}

}
