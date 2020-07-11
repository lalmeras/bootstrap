package org.likide.bootstrap.tasking.task;

public interface ITaskExecutor {

	<T> void execute(Task<T> task);

}
