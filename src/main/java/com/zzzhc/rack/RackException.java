package com.zzzhc.rack;

public class RackException extends RuntimeException {

	private static final long serialVersionUID = -8782352403460092063L;

	public RackException() {
	}

	public RackException(String message) {
		super(message);
	}

	public RackException(Throwable cause) {
		super(cause);
	}

	public RackException(String message, Throwable cause) {
		super(message, cause);
	}

}
