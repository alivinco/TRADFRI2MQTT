/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.alivinco.tradfri.types;
import java.math.BigDecimal;


/**
 * The HSBType is a complex type with constituents for hue, saturation and
 * brightness and can be used for color items.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Chris Jackson - Added fromRGB
 *
 */
public class HSBType  {

    private static final long serialVersionUID = 322902950356613226L;

    protected BigDecimal hue;
    protected BigDecimal saturation;
    protected int value;

    public HSBType(BigDecimal h, BigDecimal s, int b) {
        this.hue = h;
        this.saturation = s;
        this.value = b;
    }


    /**
     * Create HSB from RGB
     *
     * @param r red 0-255
     * @param g green 0-255
     * @param b blue 0-255
     */
    public static HSBType fromRGB(int r, int g, int b) {
        float tmpHue, tmpSaturation, tmpBrightness;

        int max = (r > g) ? r : g;
        if (b > max) {
            max = b;
        }
        int min = (r < g) ? r : g;
        if (b < min) {
            min = b;
        }
        tmpBrightness = max / 2.55f;
        tmpSaturation = (max != 0 ? ((float) (max - min)) / ((float) max) : 0) * 100;
        if (tmpSaturation == 0) {
            tmpHue = 0;
        } else {
            float red = ((float) (max - r)) / ((float) (max - min));
            float green = ((float) (max - g)) / ((float) (max - min));
            float blue = ((float) (max - b)) / ((float) (max - min));
            if (r == max) {
                tmpHue = blue - green;
            } else if (g == max) {
                tmpHue = 2.0f + red - blue;
            } else {
                tmpHue = 4.0f + green - red;
            }
            tmpHue = tmpHue / 6.0f * 360;
            if (tmpHue < 0) {
                tmpHue = tmpHue + 360.0f;
            }
        }

        return new HSBType(new BigDecimal((int) tmpHue), new BigDecimal((int) tmpSaturation),((int) tmpBrightness));
    }


    public BigDecimal getHue() {
        return hue;
    }

    public BigDecimal getSaturation() {
        return saturation;
    }

    public int getBrightness() {
        return value;
    }

    public int getRed() {
        return toRGB()[0];
    }

    public int getGreen() {
        return toRGB()[1];
    }

    public int getBlue() {
        return toRGB()[2];
    }


    public int[] toRGB() {
        int red = 0;
        int green = 0;
        int blue = 0;

        BigDecimal h = hue.divide(BigDecimal.valueOf(100), 10, BigDecimal.ROUND_HALF_UP);
        BigDecimal s = saturation.divide(BigDecimal.valueOf(100));

        int h_int = h.multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(3), 10, BigDecimal.ROUND_HALF_UP)
                .intValue();
        BigDecimal f = h.multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(3), 10, BigDecimal.ROUND_HALF_UP)
                .remainder(BigDecimal.ONE);
        int a = value*(BigDecimal.ONE.subtract(s)).intValue();
        int b = value*(BigDecimal.ONE.subtract(s.multiply(f))).intValue();
        int c = value*(BigDecimal.ONE.subtract((BigDecimal.ONE.subtract(f)).multiply(s))).intValue();

        if (h_int == 0 || h_int == 6) {
            red = getBrightness();
            green = c;
            blue = a;
        } else if (h_int == 1) {
            red = b;
            green = getBrightness();
            blue = a;
        } else if (h_int == 2) {
            red = a;
            green = getBrightness();
            blue = c;
        } else if (h_int == 3) {
            red = a;
            green = b;
            blue = getBrightness();
        } else if (h_int == 4) {
            red = c;
            green = a;
            blue = getBrightness();
        } else if (h_int == 5) {
            red = getBrightness();
            green = a;
            blue = b;
        } else {
            throw new RuntimeException();
        }
        return new int[] { red, green, blue };
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof HSBType)) {
            return false;
        }
        HSBType other = (HSBType) obj;
        if ((getHue() != null && other.getHue() == null) || (getHue() == null && other.getHue() != null)
                || (getSaturation() != null && other.getSaturation() == null)
                || (getSaturation() == null && other.getSaturation() != null)
                || (getBrightness() != 0 && other.getBrightness() == 0)
                || (getBrightness() == 0 && other.getBrightness() != 0)) {
            return false;
        }
        if (!getHue().equals(other.getHue()) || !getSaturation().equals(other.getSaturation())
                || getBrightness()==(other.getBrightness())) {
            return false;
        }
        return true;
    }



}