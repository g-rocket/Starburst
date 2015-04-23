package net.clonecomputers.lab.starburst.util;

import java.util.*;
import java.util.regex.*;

public class StaticUtils {

	public static int clamp(int min, int x, int max) {
		return (x <= min)? min: (x >= max)? max: x;
	}

	public static int color(int r, int g, int b){
		return (r << 16) | (g << 8) | b;
	}

	public static int red(int rgb){
		return (rgb & 0x00ff0000) >> 16;
	}

	public static int green(int rgb){
		return (rgb & 0x0000ff00) >> 8;
	}

	public static int blue(int rgb){
		return rgb & 0x000000ff;
	}
	

	public static int randomColor(Random rand) {
		return rand.nextInt(0xffffff+1);
	}
	
	public static double lerp(double a, double b, double lerpVal){
		return a + b*lerpVal;
	}
	
	static Pattern hyphenRegex = Pattern.compile("(?<=^|\\s)([^\\s]*-[^\\s]*)(?=$|\\s)");
	static Pattern spaceRegex = Pattern.compile("\\s+(\\w)");
	public static String toCamelCase(String s) {
		StringBuffer dehyphenated = new StringBuffer();
		Matcher hyphenMatcher = hyphenRegex.matcher(s);
		while(hyphenMatcher.find()) {
			hyphenMatcher.appendReplacement(dehyphenated, "");
			String[] words = s.substring(hyphenMatcher.start(), hyphenMatcher.end()).split("-");
			for(String word: words) {
				dehyphenated.append(word.charAt(0));
			}
		}
		hyphenMatcher.appendTail(dehyphenated);
		s = dehyphenated.toString();
		s = s.toLowerCase();
		StringBuffer camelcase = new StringBuffer();
		Matcher spaceMatcher = spaceRegex.matcher(s);
		while(spaceMatcher.find()) {
			spaceMatcher.appendReplacement(camelcase, spaceMatcher.group(1).toUpperCase());
		}
		spaceMatcher.appendTail(camelcase);
		s = camelcase.toString();
		return s;
	}
	
}
