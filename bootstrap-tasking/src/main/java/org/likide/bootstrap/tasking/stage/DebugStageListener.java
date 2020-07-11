package org.likide.bootstrap.tasking.stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugStageListener<T> implements IStageListener<T> {

	@Override
	public void onBegin(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: {}", stage.getShortName(), stage.getDescription());
		getLogger(stage).debug("{}: begin", stage.getShortName());
	}

	@Override
	public void onBeforeExecute(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: before-execute", stage.getShortName());
	}

	@Override
	public void onAfterExecute(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: after-execute", stage.getShortName());
	}

	@Override
	public void onBeforeExceptionHandling(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: before-exception-handling - {}", stage.getShortName(), control.getThrowable());
	}

	@Override
	public void onAfterExceptionHandling(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: after-exception-handling - {}", stage.getShortName(), control.getThrowable());
	}

	@Override
	public void onEnd(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: end", stage.getShortName());
	}

	@Override
	public void onSkip(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: skip", stage.getShortName());
	}

	@Override
	public void onContinue(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: continue", stage.getShortName());
	}

	@Override
	public void onExit(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: exit", stage.getShortName());
	}

	@Override
	public void onBreak(Stage<? extends T> stage, IStageControl<? extends T> control) {
		getLogger(stage).debug("{}: break", stage.getShortName());
	}

	private Logger getLogger(Stage<? extends T> stage) {
		return LoggerFactory.getLogger(stage.getLoggerName());
	}

}
