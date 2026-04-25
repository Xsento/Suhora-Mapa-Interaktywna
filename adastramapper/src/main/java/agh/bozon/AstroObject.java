package agh.bozon;

public class AstroObject {
    private String name;
    private double ra;  // Rektascensja w stopniach
    private double dec; // Deklinacja w stopniach

    public AstroObject(String name, double ra, double dec) {
        this.name = name;
        this.ra = ra;
        this.dec = dec;
    }

    public String getName() {
        return name;
    }

    public double getRa() {
        return ra;
    }

    public double getDec() {
        return dec;
    }
    
    @Override
    public String toString() {
        return String.format("%s (RA: %.2f, DEC: %.2f)", name, ra, dec);
    }
}