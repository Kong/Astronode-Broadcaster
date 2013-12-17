package com.mashape.astronode.broadcaster.configuration;

public enum LogLevel {
	TRACE(0), INFO(1), ERROR(2);

	private final int value;

	private LogLevel(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
