package org.likide.bootstrap.tasking.stage;

public interface IStageExecutor {

	<T> void execute(Stage<T> stage, StageControl<T> payload, IStageListener<? super T> listener);

}
