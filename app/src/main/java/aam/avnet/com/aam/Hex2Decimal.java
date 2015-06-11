package aam.avnet.com.aam;

import android.util.Log;

public class Hex2Decimal {

    public static int hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }


    // precondition:  d is a nonnegative integer
    public static String decimal2hex(int d) {
        String digits = "0123456789ABCDEF";
        if (d == 0) return "0";
        String hex = "";
        while (d > 0) {
            int digit = d % 16;                // rightmost digit
            hex = digits.charAt(digit) + hex;  // string concatenation
            d = d / 16;
        }
        return hex;
    }
    //0100 0011
    //C0011
    public static void main(String[] ar){
        System.out.println(hex2decimal("4300"));
    }

    public static double getRPM(String rpm){
        return hex2decimal(rpm)/(4.0);
    }

    public static int getSpeed(String speed){
        return hex2decimal(speed);
    }

    public static int getCoolantTemp(String temp){
        return (hex2decimal(temp) - 40);
    }

    public static int engineRunTime(String runTime){
        return (hex2decimal(runTime));
    }

    public static double getMAF(String maf){
        return hex2decimal(maf)/(100.0);
    }

    public static int getTorque(String torque){
        return hex2decimal(torque)-125;
    }
}
