package org.likide.bootstrap.tasking.stage;

import java.util.ArrayList;
import java.util.List;

public class StageCollection<T> {

	private List<Stage<T>> stages = new ArrayList<>();

	private Stage<T> parent;

	public StageCollection(Stage<T> parent) {
		this.parent = parent;
	}

	public Stage<T> stage() {
		Stage<T> stage = new Stage<>();
		stage.parent(this.parent);
		this.stages.add(stage);
		return stage;
	}

	public Stage<T> up() {
		return parent;
	}

}
