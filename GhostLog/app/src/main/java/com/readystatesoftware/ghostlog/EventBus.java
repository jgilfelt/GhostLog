package com.readystatesoftware.ghostlog;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class EventBus {
	private static final Bus BUS = new Bus(ThreadEnforcer.ANY);

	public static Bus getInstance() {
		return BUS;
	}

	private EventBus() {
		// No instances.
	}

    public static class PlayLogEvent {

    }

    public static class PauseLogEvent {

    }

    public static class ClearLogEvent {

    }
}
