package com.zzzhc.rack;

public interface IBody {

	byte[] next();

	public static class EmptyBody implements IBody {

		public byte[] next() {
			return null;
		}

	}

}
