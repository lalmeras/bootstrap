package org.likide.bootstrap.tasking.stage;

/**
 * <pre>
 * digraph Stage {
 *   begin -> beforeExecute -> afterExecute -> end;
 *   beforeExecute -> beforeHandlingException -> afterHandlingException -> end;
 *   end -> skip;
 *   end -> continue;
 *   end -> exit;
 *   end -> break;
 * }
 * </pre>
 *
 * @param <T> Stage payload's type or supertype
 */
public interface IStageListener<T> {

	void onBegin(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onBeforeExecute(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onAfterExecute(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onBeforeExceptionHandling(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onAfterExceptionHandling(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onEnd(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onSkip(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onContinue(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onExit(Stage<? extends T> stage, IStageControl<? extends T> control);

	void onBreak(Stage<? extends T> stage, IStageControl<? extends T> control);
}
