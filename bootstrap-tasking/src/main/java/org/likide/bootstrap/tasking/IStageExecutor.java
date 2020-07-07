package org.likide.bootstrap.tasking;

public interface IStageExecutor {

	<T> void execute(Stage<T> stage, StageControl<T> payload, IStageListener<T> listener);

}
