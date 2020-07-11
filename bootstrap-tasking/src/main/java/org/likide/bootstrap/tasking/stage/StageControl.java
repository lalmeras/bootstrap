package org.likide.bootstrap.tasking.stage;

public class StageControl<P> implements IStageControl<P> {

	private final P payload;

	private StageBehavior behavior = StageBehavior.CONTINUE;

	private Throwable throwable;

	private boolean preventSkip = false;

	public StageControl(P payload) {
		this.payload = payload;
	}

	public void preventSkip() {
		this.preventSkip = true;
	}

	@Override
	public void doContinue() {
		this.behavior = StageBehavior.CONTINUE;
	}

	@Override
	public void doBreak() {
		this.behavior = StageBehavior.BREAK;
	}

	@Override
	public void doExit() {
		this.behavior = StageBehavior.EXIT;
	}

	public void doSkip() {
		if (preventSkip) {
			throw new IllegalStateException("Skip is not allowed");
		}
		this.behavior = StageBehavior.SKIP;
	}

	public void throwable(Throwable throwable) {
		this.throwable = throwable;
	}

	@Override
	public P getPayload() {
		return payload;
	}

	public StageBehavior getBehavior() {
		return behavior;
	}

	public Throwable getThrowable() {
		return throwable;
	}

}
