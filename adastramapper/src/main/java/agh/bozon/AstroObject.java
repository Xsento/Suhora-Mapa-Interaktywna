package agh.bozon;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import java.util.Date;

public class AstroObject {
    private String name;
    private String comment;
    private double ra;  // Rektascensja w stopniach
    private double dec; // Deklinacja w stopniach
    private double epoch;

    //Pozycja obiektu jako wektor
    Vector3D position = new Vector3D(
        Math.cos(Math.toRadians(dec)) * Math.cos(Math.toRadians(ra)),
        Math.cos(Math.toRadians(dec)) * Math.sin(Math.toRadians(ra)),
        Math.sin(Math.toRadians(dec))
    );
    
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

    //Zwraca tablice z Az w pozycji 0 i Alt w pozycji 1
    public double[] getAzimuthElevation(ObservationStation station) {
        try {
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
            GeodeticPoint stationLocation = new GeodeticPoint(Math.toRadians(station.getLatitude()), Math.toRadians(station.getLongitude()), station.getAltitude());
            TopocentricFrame stationFrame = new TopocentricFrame(earth, stationLocation, "Teleskop");

            AbsoluteDate date = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
            
            double elevation = stationFrame.getElevation(position, stationFrame, date);
            double azimuth = stationFrame.getAzimuth(position, stationFrame, date);
            return new double[]{Math.toDegrees(azimuth), Math.toDegrees(elevation)};
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s (RA: %.2f, DEC: %.2f, Epoch: %.2f)", name, ra, dec, epoch);
    }
}
