package org.dhp.common.utils;

public class LongUtils {

    static char[] digits = new char[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57,//0-9
            65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, //A-Z
            95, //_
            97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122//a-z
    };
    static int[] digitPos = new int[122 - 48 + 1];

    static {
        for (int i = 0; i < digits.length; i++) {
            int digit = digits[i];
            digitPos[digit - 48] = i;
        }
    }


    /**
     *
     * @param value
     * @param shift 2-6
     * @return
     */
    public static String toShortString(long value, int shift) {
        if (shift < 2 || shift > 6) {
            throw new NumberFormatException("unsupported shift, use 2-6, current=" + shift);
        }
        int radix = 1 << shift;
        long left = value;
        StringBuffer sb = new StringBuffer();
        do {
            char c = digits[(int) (left % radix)];
            sb.append(c);
            left = left >> shift;
        } while (left > radix);
        char c = digits[(int) (left % radix)];
        sb.append(c);
        return sb.toString();
    }

    /**
     *
     * @param s
     * @param shift 2-6
     * @return
     */
    public static long parseLong(String s, int shift) {
        if (shift < 2 || shift > 6) {
            throw new NumberFormatException("unsupported shift, use 2-6, current=" + shift);
        }
        int radix = 1 << shift;
        int len = s.length();
        int charPos = len - 1;
        long value = 0;
        do {
            char c = s.charAt(charPos);
            int index = digitPos[(int) (c - 48)];
            value = value * radix + index;
            charPos--;
        } while (charPos >= 0);
        return value;
    }
}
