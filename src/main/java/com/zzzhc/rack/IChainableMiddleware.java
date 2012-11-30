package com.zzzhc.rack;

public interface IChainableMiddleware extends IMiddleware {

	void setApp(IMiddleware app);

}
