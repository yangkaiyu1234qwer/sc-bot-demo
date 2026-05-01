package com.yangky.scbotdemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Maths
 *
 * @author yangky
 * @Date 2026/4/27 23:25
 */
public class Maths {


    public static double add(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).doubleValue();
    }

    public static double sub(double a, double b) {
        return BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b)).doubleValue();
    }

    public static double div(double a, double b, int scale, RoundingMode roundingMode) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), scale, roundingMode).doubleValue();
    }

    public static double mul(double a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b)).doubleValue();
    }
}
