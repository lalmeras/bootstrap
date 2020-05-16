package org.likide.bootstrap;

public enum MinicondaVersion {

	_2("2"),
	_3("3");

	private final String name;

	private MinicondaVersion(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
