package org.likide.bootstrap.tasking;

public interface ITaskExecutor {

	<T> void execute(Task<T> task);

}
