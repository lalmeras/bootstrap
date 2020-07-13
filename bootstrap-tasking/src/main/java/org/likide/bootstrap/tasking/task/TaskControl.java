package org.likide.bootstrap.tasking.task;

import org.likide.bootstrap.tasking.stage.Stage;

public class TaskControl<T> {

	private boolean exit = false;

	private int errorCode = 0;

	private T payload;

	private Stage exitStage;

	private Throwable throwable;

	public TaskControl(T payload) {
		this.payload = payload;
	}

	public void exitOnError(Stage exitStage, Throwable throwable, int errorCode) {
		exit = true;
		// TODO: enforce not-0 exitCode ?
		this.errorCode = errorCode;
		this.throwable = throwable;
		this.exitStage = exitStage;
	}

	public void exitOnSuccess() {
		exit = true;
		errorCode = 0;
	}

	public T getPayload() {
		return payload;
	}

	public boolean isExit() {
		return exit;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public Stage getExitStage() {
		return exitStage;
	}

	public Throwable getThrowable() {
		return throwable;
	}

}
