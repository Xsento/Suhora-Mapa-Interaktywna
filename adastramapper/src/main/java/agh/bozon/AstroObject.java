package agh.bozon;

public class AstroObject {
    private String name;
    private String comment;
    private double ra;  // Rektascensja w stopniach
    private double dec; // Deklinacja w stopniach
    private double epoch;

    public AstroObject(String name, String comment, double ra, double dec, double epoch) {
        this.name = name;
        this.comment = comment;
        this.ra = ra;
        this.dec = dec;
        this.epoch = epoch;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public double getRa() {
        return ra;
    }

    public double getDec() {
        return dec;
    }

    public double getEpoch() {
        return epoch;
    }
    
    @Override
    public String toString() {
        return String.format("%s (RA: %.2f, DEC: %.2f, Epoch: %.2f)", name, ra, dec, epoch);
    }
}