package com.jadn.cc.trace;

import android.util.Log;

public class TraceUtil {

	public static void report(Throwable e) {
		saveTrace(e);
	}

	public static void saveTrace(Throwable e) {
		Log.e(TraceUtil.class.getName(), "Huh", e);
		return;
	}

}
