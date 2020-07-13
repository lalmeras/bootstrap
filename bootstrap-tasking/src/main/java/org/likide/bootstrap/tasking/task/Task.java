package org.likide.bootstrap.tasking.task;

import org.likide.bootstrap.tasking.stage.Stage;

public class Task<T> {

	private String description;

	private Stage<T> stage;

	public Task(String description, Stage<T> stage) {
		super();
		this.description = description;
		this.stage = stage;
	}

	public String getDescription() {
		return description;
	}

	public Stage<T> getStage() {
		return stage;
	}

}
