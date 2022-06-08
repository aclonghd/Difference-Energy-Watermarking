/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dew;

import java.awt.Color;

/**
 *
 * @author Vũ Gia Long - B18DCAT154
 */
public class YCrCb {

    private int Y;
    private int Cr;
    private int Cb;
    private int p;
    
    public YCrCb(){
        
    }

    public YCrCb(int r, int g, int b) {
        this.Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        this.Cb = (int) (128 - 0.169 * r - 0.331 * g + 0.500 * b);
        this.Cr = (int) (128 + 0.500 * r - 0.419 * g - 0.081 * b);
        this.p = (Y << 16) | (Cb << 8) | (Cr);
    }

    public Color getColorFromYCbCr() {
        double y = (double) Y;
        double cb = (double) Cb;
        double cr = (double) Cr;

        int r = (int) (y + 1.40200 * (cr - 0x80));
        int g = (int) (y - 0.34414 * (cb - 0x80) - 0.71414 * (cr - 0x80));
        int b = (int) (y + 1.77200 * (cb - 0x80));

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        Color color=  new Color(r,g,b);
        return color;
    }

    public int getY() {
        return Y;
    }

    public void setY(int Y) {
        this.Y = Y;
    }

    public int getCr() {
        return Cr;
    }

    public void setCr(int Cr) {
        this.Cr = Cr;
    }

    public int getCb() {
        return Cb;
    }

    public void setCb(int Cb) {
        this.Cb = Cb;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }
}
