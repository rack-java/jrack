package com.zzzhc.rack;

public abstract class BaseChainableMiddleware implements IChainableMiddleware {

	protected IMiddleware app;

	public void setApp(IMiddleware app) {
		this.app = app;
	}

}
