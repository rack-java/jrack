package com.zzzhc.rack;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {

	public static final Pattern DEFAULT_SEP = Pattern.compile("[&;] *");
	public static final Pattern COOKIE_SEP = Pattern.compile("[;,] *");

	private static final Pattern NAME_PATTERN = Pattern
			.compile("\\A[\\[\\]]*([^\\[\\]]+)\\]*");
	private static final Pattern HASH_KEY_PATTERN1 = Pattern
			.compile("^\\[\\]\\[([^\\[\\]]+)\\]$");
	private static final Pattern HASH_KEY_PATTERN2 = Pattern
			.compile("^\\[\\](.+)$");

	private static class UtilsContext {
		SimpleDateFormat httpDateFormat;

		public UtilsContext() {
			httpDateFormat = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
	}

	private static ThreadLocal<UtilsContext> threadLocal = new ThreadLocal<Utils.UtilsContext>() {
		protected UtilsContext initialValue() {
			return new UtilsContext();
		}
	};

	public static Map<String, Object> parseNestedQuery(String qs, Pattern d) {
		Map<String, Object> params = new HashMap<String, Object>();

		if (qs == null) {
			qs = "";
		}
		for (String p : d.split(qs)) {
			String kv[] = splitKeyValue(p);
			normalizeParams(params, kv[0], kv[1]);
		}
		return params;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> normalizeParams(
			Map<String, Object> params, String name, Object value) {
		Matcher m = NAME_PATTERN.matcher(name);
		if (!m.find()) {
			return null;
		}
		String k = m.group(1);
		String after = name.substring(m.end());
		if (after.isEmpty()) {
			params.put(k, value);
		} else if ("[]".equals(after)) {
			Object v = params.get(k);
			if (v == null) {
				v = new ArrayList<Object>();
				params.put(k, v);
			} else {
				if (!(v instanceof List)) {
					throw new RackException("expected List (got "
							+ v.getClass() + ") for param `" + k + "`");
				}
			}
			((List<Object>) v).add(value);
		} else {
			String childKey = extractArrayHashKey(after);
			if (childKey == null) {
				Object v = params.get(k);
				if (v == null) {
					v = new HashMap<String, Object>();
					params.put(k, v);
				} else {
					if (!(v instanceof Map)) {
						throw new RackException("expected Map (got "
								+ v.getClass() + ") for param `" + k + "`");
					}
				}
				params.put(k,
						normalizeParams((Map<String, Object>) v, after, value));
			} else {
				Object v = params.get(k);
				if (v == null) {
					v = new ArrayList<Map<String, Object>>();
					params.put(k, v);
				} else {
					if (!(v instanceof List)) {
						throw new RackException("expected List (got "
								+ v.getClass() + ") for param `" + k + "`");
					}
				}
				List<Object> ol = (List<Object>) v;
				if (isLastKeyNotFound(ol, childKey)) {
					normalizeParams(
							(Map<String, Object>) ol.get(ol.size() - 1),
							childKey, value);
				} else {
					Object p = normalizeParams(new HashMap<String, Object>(),
							childKey, value);
					ol.add(p);
				}
			}
		}
		return params;
	}

	@SuppressWarnings("unchecked")
	private static boolean isLastKeyNotFound(List<Object> list, String key) {
		if (list.size() == 0) {
			return false;
		}
		Object v = list.get(list.size() - 1);
		if (!(v instanceof Map<?, ?>)) {
			return false;
		}
		Map<String, Object> h = (Map<String, Object>) v;
		return !h.containsKey(key);
	}

	private static String extractArrayHashKey(String after) {
		Matcher m = HASH_KEY_PATTERN1.matcher(after);
		if (m.matches()) {
			return m.group(1);
		}
		m = HASH_KEY_PATTERN2.matcher(after);
		if (m.matches()) {
			return m.group(1);
		}
		return null;
	}

	public static String[] splitKeyValue(String s) {
		String kv[] = new String[] { s, "" };
		int idx = s.indexOf('=');
		if (idx != -1) {
			kv[0] = s.substring(0, idx);
			kv[1] = s.substring(idx + 1);
		}
		return kv;
	}

	public static String unescape(String s) {
		try {
			return unescape(s, Env.DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RackException(e);
		}
	}

	public static String unescape(String s, String enc)
			throws UnsupportedEncodingException {
		return URLDecoder.decode(s, enc);
	}

	public static byte[] toUTF8Bytes(String s) {
		try {
			return s.getBytes(Env.DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RackException(e);
		}
	}

	public static String toHttpDate(Date date) {
		return threadLocal.get().httpDateFormat.format(date);
	}

	private static final Pattern RANGES_SEP = Pattern.compile(",\\s*");
	private static final Pattern RANGE_BYTES = Pattern
			.compile("bytes=(\\d*)-(\\d*)");

	/*
	 * See <http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35>
	 */
	public static List<long[]> byteRanges(Env env, long size) {
		String httpRange = env.getString("HTTP_RANGE");
		if (httpRange == null) {
			return null;
		}
		List<long[]> ranges = new ArrayList<long[]>();
		for (String rangeSpec : RANGES_SEP.split(httpRange)) {
			Matcher m = RANGE_BYTES.matcher(rangeSpec);
			if (!m.find()) {
				return null;
			}
			String r0 = m.group(1);
			String r1 = m.group(2);
			long l0 = 0;
			long l1 = 0;
			if (r0.isEmpty()) {
				if (r1.isEmpty()) {
					return null;
				}
				// suffix-byte-range-spec, represents trailing suffix of file
				l0 = size - Long.parseLong(r1);
				if (l0 < 0) {
					l0 = 0;
				}
				l1 = size - 1;
			} else {
				l0 = Long.parseLong(r0);
				if (r1.isEmpty()) {
					l1 = size - 1;
				} else {
					l1 = Long.parseLong(r1);
					if (l1 < l0) {
						return null;
					}
					if (l1 >= size) {
						l1 = size - 1;
					}
				}
			}

			if (l0 <= l1) {
				ranges.add(new long[] { l0, l1 });
			}
		}

		return ranges;
	}
}
