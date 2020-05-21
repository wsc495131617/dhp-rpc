package org.dhp.common.utils;

public class DoubleUtils {
	private static final double DEFAULT_DELTA = 0.000001; // 默认比较精度

	// 比较2个double值是否相等（默认精度）
	public static boolean considerEqual(double v1, double v2) {
		return considerEqual(v1, v2, DEFAULT_DELTA);
	}

	// 比较2个double值是否相等（指定精度）
	public static boolean considerEqual(double v1, double v2, double delta) {
		return Double.compare(v1, v2) == 0 || considerZero(v1 - v2, delta);
	}

	// 判断指定double是否为0（默认精度）
	public static boolean considerZero(double value) {
		return considerZero(value, DEFAULT_DELTA);
	}

	// 判断指定double是否为0（指定精度）
	public static boolean considerZero(double value, double delta) {
		return Math.abs(value) <= delta;
	}

}
