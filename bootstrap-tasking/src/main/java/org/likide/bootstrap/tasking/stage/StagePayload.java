package org.likide.bootstrap.tasking.stage;

public class StagePayload<T> {

	private final StageBehavior behavior;
	private final T payload;
	private final Throwable throwable;

	private StagePayload(StageBehavior behavior, T payload, Throwable throwable) {
		super();
		this.behavior = behavior;
		this.payload = payload;
		this.throwable = throwable;
	}

	public StageBehavior getBehavior() {
		return behavior;
	}

	public T getPayload() {
		return payload;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public static <S> StagePayload<S> doContinue(S payload) {
		return new StagePayload<>(StageBehavior.CONTINUE, payload, null);
	}

	public static <S> StagePayload<S> doBreak(S payload) {
		return new StagePayload<>(StageBehavior.CONTINUE, payload, null);
	}

	public static <S> StagePayload<S> doThrow(S payload, Throwable throwable) {
		return new StagePayload<>(StageBehavior.THROWING, payload, throwable);
	}

	public static <S> StagePayload<S> doExit(S payload, Throwable throwable) {
		return new StagePayload<>(StageBehavior.EXIT, payload, throwable);
	}

	public static <S> StagePayload<S> doSkip(S payload) {
		return new StagePayload<>(StageBehavior.SKIP, payload, null);
	}

	public static <S> StagePayload<S> fromException(S payload, StageBehavior behavior, Throwable e) {
		return new StagePayload<S>(behavior, payload, e);
	}
}
