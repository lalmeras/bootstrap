package org.likide.bootstrap.tasking;

public class StageException extends RuntimeException {

	private static final long serialVersionUID = 5445508356966301411L;

	private StageBehavior behavior = StageBehavior.BREAK;

	public StageException() {
		super();
	}

	public StageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public StageException(String message, Throwable cause) {
		super(message, cause);
	}

	public StageException(String message) {
		super(message);
	}

	public StageException(Throwable cause) {
		super(cause);
	}

	public StageException behavior(StageBehavior behavior) {
		this.behavior = behavior;
		return this;
	}

	public StageBehavior getBehavior() {
		return this.behavior;
	}

}
