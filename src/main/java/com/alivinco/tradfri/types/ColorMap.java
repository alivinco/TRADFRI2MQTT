package com.alivinco.tradfri.types;

/**
 * Created by alivinco on 15/04/2018.
 */


/**
 *  1:1 - Color X = 21109 Y = 21738 rgb(220, 241, 247)
 *  1:2 - Color X = 22584 Y = 23272 rgb(234, 246, 251)
 *  1:3 - Color X = 25022 Y = 24884 rgb(245, 250, 247)
 *  1:4 - Color X = 29713 Y = 26431 rgb(242, 235, 209)
 *  1:5 - Color X = 30015 Y = 26870 rgb(241, 224, 184)
 *
 *  2:1 - Color X = 32886 Y = 27217 rgb(239, 209, 125)
 *  2:2 - Color X = 35848 Y = 26214 rgb(235, 181, 77)
 *  2:3 - Color X = 38011 Y = 24904 rgb(231, 135, 65)
 *  2:4 - Color X = 38011 Y = 22938 rgb(229, 115, 77)
 *  2:5 - Color X = 40632 Y = 22282 rgb(218, 94, 71)
 *
 *  3:1 - Color X = 42926 Y = 21299 rgb(213, 74, 55)
 *  3:2 - Color X = 32768 Y = 18350 rgb(227, 147, 174)
 *  3:3 - Color X = 29491 Y = 18350 rgb(233, 191, 220)
 *  3:4 - Color X = 32768 Y = 15729 rgb(217, 56, 124)
 *  3:5 - Color X = 22282 Y = 12452 rgb(201, 134, 185)
 *
 *  4:1 - Color X = 20316 Y = 8520 rgb(142, 44, 132)t
 *  4:2 - Color X = 11469 Y = 3277 rgb(73, 67, 135)
 *  4:3 - Color X = 13107 Y = 6554  rgb(108, 132, 184)
 *  4:4 - Color X = 26870 Y = 33423 rgb(169, 212, 66)
 *  4:5 - Color X = 29491 Y = 30802 rgb(214, 226, 92)
 *
 */




public class ColorMap {

    public static  class MapRecord {
        int ID ;
        int X;
        int Y;

        int R;
        int G;
        int B;

        public MapRecord(int id ,int x, int y, int r, int g, int b, String name) {
            ID = id;
            X = x;
            Y = y;
            R = r;
            G = g;
            B = b;
            this.name = name;
        }

        String name;

    }

    static MapRecord[] colorMap = {
            // 1
            new MapRecord(1,21109,21738,220,241,247,""),
            new MapRecord(2,22584,23272,234, 246, 251,""),
            new MapRecord(3,25022, 24884,245, 250, 247,""),
            new MapRecord(4,29713, 26431,242, 235, 209,""),
            new MapRecord(5,30015, 26870,241, 224, 184,""),
            // 2
            new MapRecord(6,32886, 27217 ,239, 209, 125,""),
            new MapRecord(7,35848 ,26214 ,235, 181, 77,""),
            new MapRecord(8,38011 , 24904 ,231, 135, 65,""),
            new MapRecord(9,38011 , 22938 ,229, 115, 77,""),
            new MapRecord(10,40632 ,22282 ,218, 94, 71,""),
            // 3
            new MapRecord(11,42926 , 21299 ,213, 74, 55,""),
            new MapRecord(12,32768 , 18350 ,227, 147, 174,""),
            new MapRecord(13,29491 ,18350 ,233, 191, 220,""),
            new MapRecord(14,32768 ,15729 ,217, 56, 124,""),
            new MapRecord(15,22282 , 12452 ,201, 134, 185,""),
            // 4
            new MapRecord(16,20316 , 8520 ,142, 44, 132,""),
            new MapRecord(17,11469 , 3277 ,73, 67, 135,""),
            new MapRecord(18,13107 , 6554  ,108, 132, 184,""),
            new MapRecord(19,26870 , 33423 ,169, 212, 66,""),
            new MapRecord(20,29491 , 30802 ,214, 226, 92,"")

    };



    int getRGBDistance(MapRecord rec, int r,int g,int b) {
        int rd = java.lang.Math.abs(rec.R-r);
        int gd = java.lang.Math.abs(rec.G-g);
        int bd = java.lang.Math.abs(rec.B-b);
        return (int)java.lang.Math.sqrt(rd*rd+gd*gd+bd*bd);
    }

    int getXYDistance(MapRecord rec, int x,int y) {
        int xd = java.lang.Math.abs(rec.X-x);
        int yd = java.lang.Math.abs(rec.Y-y);
        return (int)java.lang.Math.sqrt(xd*xd+yd*yd);
    }

    public RGB XYtoRGB(int x,int y) {
        int minDistance = 100000;
        MapRecord minDistRecord = null;
        for (MapRecord rec : colorMap) {
            int dist = getXYDistance(rec,x,y);
            if (dist<minDistance) {
                minDistance = dist;
                minDistRecord = rec;
            }
        }
        RGB rgb = new RGB();
        if (minDistRecord != null ) {
            rgb.r = minDistRecord.R;
            rgb.g = minDistRecord.G;
            rgb.b = minDistRecord.B;
        }
        return rgb;
    }

    public XY RGBtoXY(int r,int g,int b)  {
         int minDistance = 100000;
         MapRecord minDistRecord = null;
         for (MapRecord rec : colorMap) {
             int dist = getRGBDistance(rec,r,g,b);
             if (dist<minDistance) {
                 minDistance = dist;
                 minDistRecord = rec;
             }
         }
         XY xy = new XY();
         if (minDistRecord != null ) {
              xy.x = minDistRecord.X;
              xy.y = minDistRecord.Y;
         }
         return xy;
    }



}
