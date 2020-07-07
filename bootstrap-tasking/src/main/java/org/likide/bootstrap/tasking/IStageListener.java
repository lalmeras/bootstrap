package org.likide.bootstrap.tasking;

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
 * @param <T> Stage payload's type
 */
public interface IStageListener<T> {

	void onBegin(Stage stage, IStageControl<T> control);

	void onBeforeExecute(Stage stage, IStageControl<T> control);

	void onAfterExecute(Stage stage, IStageControl<T> control);

	void onBeforeExceptionHandling(Stage stage, IStageControl<T> control);

	void onAfterExceptionHandling(Stage stage, IStageControl<T> control);

	void onEnd(Stage stage, IStageControl<T> control);

	void onSkip(Stage stage, IStageControl<T> control);

	void onContinue(Stage stage, IStageControl<T> control);

	void onExit(Stage stage, IStageControl<T> control);

	void onBreak(Stage stage, IStageControl<T> control);
}
