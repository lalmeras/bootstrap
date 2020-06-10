package org.likide.bootstrap.impl;

public enum MinicondaVersion {

	V2("2"),
	V3("3");

	private final String name;

	private MinicondaVersion(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
