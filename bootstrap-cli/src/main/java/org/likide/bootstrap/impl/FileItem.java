package org.likide.bootstrap.impl;

import java.nio.file.Path;

public class FileItem {

	private final Path path;

	public FileItem(Path path) {
		this.path = path;
	}

	public Path getPath() {
		return path;
	}

}
