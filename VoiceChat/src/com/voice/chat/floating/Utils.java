package com.voice.chat.floating;

public class Utils {
	public static boolean isSet(int flags, int flag) {
		return (flags & flag) == flag;
	}
}
