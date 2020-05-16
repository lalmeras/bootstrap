package org.likide.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileRegistry {

	public final List<FileItem> items = new ArrayList<>();

	public FileRegistry() {
		super();
	}

	public void addFile(FileItem fileItem) {
		items.add(fileItem);
	}

	public List<FileItem> getFiles() {
		return Collections.unmodifiableList(items);
	}

}
