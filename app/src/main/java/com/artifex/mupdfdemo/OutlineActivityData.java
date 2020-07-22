package com.artifex.mupdfdemo;

import java.util.HashMap;

public class OutlineActivityData {
	public OutlineItem items[];
	public int         position;
	static private OutlineActivityData singleton;
	static private HashMap<String, String> book;
	private static boolean isNightMode = false;

	static public void set(OutlineActivityData d) {
		singleton = d;
	}

	static public OutlineActivityData get() {
		if (singleton == null)
			singleton = new OutlineActivityData();
		return singleton;
	}
	
	static public void setBook(HashMap<String, String> b) {
		book = b;
	}

	static public HashMap<String, String> getBook() {
		if (book == null)
			book = new HashMap<String, String>();
		return book;
	}
	
	public static void setNightMode(boolean nightMode)	{
		isNightMode = nightMode;
	}
	
	public static boolean getNightMode()	{
		return isNightMode;
	}
}