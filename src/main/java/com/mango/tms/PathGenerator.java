package com.mango.tms;

import java.util.Properties;

public class PathGenerator {

	public String fURLPattern;
	
	public void init(Properties props) {
		String value = props.getProperty("url.pattern");
		if (value == null) {
			throw new RuntimeException("url.pattern");
		}
		fURLPattern = value;
	}
}
