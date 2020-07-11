package org.likide.bootstrap.tasking.stage;

public interface IStageControl<P> {

	void doContinue();

	void doBreak();

	void doExit();

	P getPayload();

	Throwable getThrowable();

}