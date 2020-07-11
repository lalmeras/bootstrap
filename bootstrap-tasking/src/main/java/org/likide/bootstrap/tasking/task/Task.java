package org.likide.bootstrap.tasking.task;

import java.util.List;
import java.util.function.Predicate;

import org.likide.bootstrap.tasking.stage.Stage;

public class Task<T> {

	private String description;

	private List<Stage<T>> stages;

}
