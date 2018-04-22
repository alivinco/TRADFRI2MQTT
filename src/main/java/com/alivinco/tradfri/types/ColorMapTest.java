package com.alivinco.tradfri.types;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by alivinco on 22/04/2018.
 */
public class ColorMapTest {
    @Test
    public void getRGBDistance() throws Exception {
    }

    @Test
    public void getXYDistance() throws Exception {
    }

    @Test
    public void XYtoRGB() throws Exception {
        ColorMap cmap = new ColorMap();
        RGB rgb =  cmap.XYtoRGB(21109 , 21738);
        assertEquals(rgb.r,220);
        assertEquals(rgb.g,241);
        assertEquals(rgb.b,247);

        rgb =  cmap.XYtoRGB(21209 , 21838);
        assertEquals(rgb.r,220);
        assertEquals(rgb.g,241);
        assertEquals(rgb.b,247);

        rgb =  cmap.XYtoRGB(42926 , 21299);
        assertEquals(rgb.r,213);
        assertEquals(rgb.g,74);
        assertEquals(rgb.b,55);

        rgb =  cmap.XYtoRGB(42826 , 21299);
        assertEquals(rgb.r,213);
        assertEquals(rgb.g,74);
        assertEquals(rgb.b,55);
    }

    @Test
    public void RGBtoXY() throws Exception {
        ColorMap cmap = new ColorMap();
        XY xy = cmap.RGBtoXY(220,241,247);
        assertEquals(xy.x,21109);
        assertEquals(xy.y,21738);

        xy = cmap.RGBtoXY(213,74,55);
        assertEquals(xy.x,42926);
        assertEquals(xy.y,21299);
    }

}