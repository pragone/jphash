package com.pragone.jphash.index;

public class DoubleVector implements Vector {
    private final double[] coords;
    private double norm2 = -1;

    public DoubleVector(double[] coordinates) {
        this.coords = coordinates;
    }

    @Override
    public double getNorm2() {
        if (norm2 < 0) {
            calculateNorm2();
        }
        return norm2;
    }

    protected void calculateNorm2() {
        double resp = 0;
        for (int i = 0; i < coords.length; i++) {
            resp += coords[i]*coords[i];
        }
        this.norm2 = Math.sqrt(resp);
    }

    @Override
    public double similarity(Vector other) {
        if (other == null || other.getDimensions() != getDimensions()) {
            throw new IllegalArgumentException("Similarity can only be calculated on two non-null equal-size vectors");
        }
        double t = dotProduct(other);
        if (t <= 0) {
            return 0;
        } else {
            t = t / (getNorm2()*other.getNorm2());
            if (t > 1){
                return 1;
            }
            return t;
        }
    }

    @Override
    public double dotProduct(Vector other) {
        if (other == null || other.getDimensions() != getDimensions()) {
            throw new IllegalArgumentException("Dot Product can only be calculated on two non-null equal-size vectors");
        }
        int resp = 0;
        for (int i = 0; i < coords.length; i++) {
            resp += coords[i]*other.getCoord(i);
        }
        return resp;
    }

    @Override
    public double angularDistance(Vector other) {
        if (other == null || other.getDimensions() != getDimensions()) {
            throw new IllegalArgumentException("Angular distance can only be calculated on two non-null equal-size vectors");
        }
        return Math.acos(similarity(other));
    }

    @Override
    public int getDimensions() {
        return this.coords.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (double element : coords) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(element);
        }
        sb.append('}');
        return sb.toString();
    }

    public double[] getCoords() {
        return coords;
    }

    @Override
    public double getCoord(int index) {
        return coords[index];
    }

    @Override
    public void multiply(double scale) {
        for (int i = 0; i < coords.length; i++) {
            coords[i] *= scale;
        }
    }
}