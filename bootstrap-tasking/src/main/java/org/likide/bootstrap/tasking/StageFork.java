package org.likide.bootstrap.tasking;

import java.util.ArrayList;
import java.util.List;

public class StageFork<T> {

	private List<Stage<T>> stages = new ArrayList<>();

	private Stage<T> parent;

	public StageFork(Stage<T> parent) {
		this.parent = parent;
	}

	public Stage<T> stage() {
		Stage<T> stage = new Stage<>();
		stage.parent(this.parent);
		stages.add(stage);
		return stage;
	}

	public Stage<T> up() {
		return parent;
	}

}
