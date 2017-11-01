package com.alivinco.tradfri;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alivinco on 24/09/2017.
 */
public class Color {
    public static List<Integer> convertRGBtoXY(int cred ,int cgreen,int cblue) {
        // For the hue bulb the corners of the triangle are:
        // -Red: 0.675, 0.322
        // -Green: 0.4091, 0.518
        // -Blue: 0.167, 0.04
//        float cred =

        double[] normalizedToOne = new double[3];
        normalizedToOne[0] = (cred / 255);
        normalizedToOne[1] = (cgreen / 255);
        normalizedToOne[2] = (cblue / 255);
        float red, green, blue;

        // Make red more vivid
        /*if (normalizedToOne[0] > 0.04045) {
            red = (float) Math.pow(
                    (normalizedToOne[0] + 0.055) / (1.0 + 0.055), 2.4);
        } else {
            red = (float) (normalizedToOne[0] / 12.92);
        }

        // Make green more vivid
        if (normalizedToOne[1] > 0.04045) {
            green = (float) Math.pow((normalizedToOne[1] + 0.055)
                    / (1.0 + 0.055), 2.4);
        } else {
            green = (float) (normalizedToOne[1] / 12.92);
        }

        // Make blue more vivid
        if (normalizedToOne[2] > 0.04045) {
            blue = (float) Math.pow((normalizedToOne[2] + 0.055)
                    / (1.0 + 0.055), 2.4);
        } else {
            blue = (float) (normalizedToOne[2] / 12.92);
        }*/

        red = (float) (normalizedToOne[0]);
        green = (float) (normalizedToOne[1]);
        blue = (float) (normalizedToOne[2]);

        float X = (float) (red * 0.649926 + green * 0.103455 + blue * 0.197109);
        float Y = (float) (red * 0.234327 + green * 0.743075 + blue + 0.022598);
        float Z = (float) (red * 0.0000000 + green * 0.053077 + blue * 1.035763);

        System.out.println(X);
        System.out.println(Y);
        System.out.println(Z);

        float x = X / (X + Y + Z);
        float y = Y / (X + Y + Z);

        System.out.println(x);
        System.out.println(y);

        List<Integer> xyAsList = new ArrayList<Integer>();
        //xyAsList.add(Math.round(x*100));
        //xyAsList.add(Math.round(y*100));
        xyAsList.add(Math.round(x*65535));
        xyAsList.add(Math.round(y*65535));
        return xyAsList;
    }

    public static String convertRGBtoHex(int red ,int green,int blue) {
        String hex = String.format("#%02x%02x%02x", red, green, blue);
        return hex;
    }
}
