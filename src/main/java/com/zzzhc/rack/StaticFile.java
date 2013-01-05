package com.zzzhc.rack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.io.Closeables;

public class StaticFile implements IMiddleware {
	private static final String[] ALLOWED_METHODS = new String[] { "GET",
			"HEAD" };
	private static final Pattern SEPS_PATTERN = Pattern.compile("/");
	private static final int BUF_SIZE = 8 * 1024;

	private File root;
	private String cacheControl;

	public StaticFile(File root, String cacheControl) {
		this.root = root;
		this.cacheControl = cacheControl;
	}

	private boolean isAlloedMethod(Env env) {
		String method = env.getString(Env.REQUEST_METHOD);
		for (int i = 0; i < ALLOWED_METHODS.length; i++) {
			if (ALLOWED_METHODS[i].equals(method)) {
				return true;
			}
		}
		return false;
	}

	private Response fail(int status, String body) {
		body = body + "\n";
		byte[] bytes = Utils.toUTF8Bytes(body);
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "text/plain");
		headers.put("Content-Length", String.valueOf(bytes.length));
		headers.put("X-Cascade", "pass");
		return new Response(status, headers, new MultiBody(bytes));
	}

	public Response call(Env env) {
		if (!isAlloedMethod(env)) {
			return fail(405, "Method Not Allowed");
		}

		String pathInfo = env.getString(Env.PATH_INFO);
		String[] parts = SEPS_PATTERN.split(pathInfo);
		int depth = 0;
		for (String part : parts) {
			if ("".equals(part) || ".".equals(part)) {
				continue;
			} else if ("..".equals(part)) {
				if (depth - 1 < 0) {
					return fail(404, "Not Found");
				}
				depth--;
			} else {
				depth++;
			}
		}

		String relativePath = pathInfo.substring(1);
		File file = new File(root, relativePath);
		if (file.exists() && file.canRead()) {
			return serving(env, file);
		} else {
			return fail(404, "File not found: " + pathInfo);
		}
	}

	private Response serving(Env env, File file) {
		String clientLastModified = env.getString("HTTP_IF_MODIFIED_SINCE");

		Date lastModifiedDate = new Date(file.lastModified());
		String lastModified = Utils.toHttpDate(lastModifiedDate);
		if (clientLastModified != null) {
			if (lastModified.equals(clientLastModified)) {
				return new Response(304, new MultiBody());
			}
		}

		int status = 200;
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Last-Modified", lastModified);
		headers.put("Content-Type", "text/plain");// TODO
		if (cacheControl != null) {
			headers.put("Cache-Control", cacheControl);
		}
		long size = file.length();
		long range[] = new long[] { 0, size - 1 };
		List<long[]> ranges = Utils.byteRanges(env, size);
		if (ranges == null || ranges.size() > 1) {
			// go on
		} else if (ranges.size() == 0) {
			Response response = fail(416, "Byte range unsatisfiable");
			response.setHeader("Content-Range", "bytes */" + size);
			return response;
		} else {
			// Partial content:
			range = ranges.get(0);
			status = 206;
			headers.put("Content-Range", "bytes " + range[0] + "-" + range[1]);
			size = range[1] - range[0] + 1;
		}
		headers.put("Content-Length", String.valueOf(size));

		RangeBody body = new RangeBody(file, range[0], range[1], BUF_SIZE);
		return new Response(status, headers, body);
	}

	private static class RangeBody implements IBody {
		private File file;
		private RandomAccessFile in;
		private long start;
		private long end;
		private long pos;
		private int bufSize;
		private byte[] buf;
		private volatile boolean eof;

		public RangeBody(File file, long start, long end, int bufSize) {
			this.file = file;
			this.start = start;
			this.end = end;
			this.pos = start;
			this.bufSize = bufSize;
		}

		private void close() {
			eof = true;
			
			try {
				Closeables.close(in, true);
			} catch (IOException e) {
			}
		}

		public byte[] next() {
			if (eof) {
				return null;
			}
			try {
				if (in == null) {
					in = new RandomAccessFile(file, "r");
					in.seek(start);
					buf = new byte[bufSize];
				}
				long ramaing = end - pos + 1;
				long max = ramaing > buf.length ? buf.length : ramaing;
				int len = in.read(buf, 0, (int) max);
				if (len == -1) {
					close();
					return null;
				} else {
					pos += len;
					if (pos >= end) {
						close();
					}
				}
				if (len < buf.length) {
					return Arrays.copyOf(buf, len);
				}
				return buf;
			} catch (IOException e) {
				close();
				throw new RackException(e);
			}
		}

	}
}
