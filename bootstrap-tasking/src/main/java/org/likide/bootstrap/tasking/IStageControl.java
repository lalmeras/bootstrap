package org.likide.bootstrap.tasking;

public interface IStageControl<P> {

	void doContinue();

	void doBreak();

	void doExit();

	P getPayload();

}