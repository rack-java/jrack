package com.zzzhc.rack;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultiBody implements IBody {
	private final List<byte[]> data = new ArrayList<byte[]>();
	private Iterator<byte[]> ite;

	public MultiBody() {
	}

	public MultiBody(byte[]... bytes) {
		for (byte[] b : bytes) {
			add(b);
		}
	}

	public MultiBody(String... strs) {
		for (String s : strs) {
			add(s);
		}
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public void add(byte[] bytes) {
		data.add(bytes);
	}

	public void add(String s) {
		try {
			data.add(s.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RackException(e);
		}
	}

	public void reset() {
		ite = null;
	}

	public byte[] next() {
		if (ite == null) {
			ite = data.iterator();
		}
		if (ite.hasNext()) {
			return ite.next();
		}
		return null;
	}

}
