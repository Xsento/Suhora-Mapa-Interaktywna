package agh.bozon;

import java.util.Date;

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

public class AstroObject {
    final private String name;
    final private String comment;
    final private double ra;  // Rektascensja w stopniach
    final private double dec; // Deklinacja w stopniach
    final private double epoch;
    Vector3D position; //Pozycja obiektu jako wektor

    public AstroObject(String name, String comment, double ra, double dec, double epoch) {
        this.name = name;
        this.ra = ra;
        this.dec = dec;
        this.epoch = epoch;
        this.comment = comment;

        this.position = new Vector3D(
        Math.cos(Math.toRadians(dec)) * Math.cos(Math.toRadians(ra)),
        Math.cos(Math.toRadians(dec)) * Math.sin(Math.toRadians(ra)),
        Math.sin(Math.toRadians(dec))
        );
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

    // Przeliczenie RA/DEC na Az/Alt dla danej stacji obserwacyjnej i aktualnego czasu
    public double[] getAzimuthElevation(ObservationStation station) {
        try {
            // obsługa epoki - przeliczenie pozycji obiektu z układu związanego z epoką na aktualny układ GCRF
            AbsoluteDate epochDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, (this.epoch - 2000.0) * Constants.JULIAN_YEAR);
            Frame epochFrame = FramesFactory.getMOD(IERSConventions.IERS_2010);
            Frame gcrf = FramesFactory.getGCRF();
            org.orekit.frames.Transform epochToGcrf = epochFrame.getTransformTo(gcrf, epochDate);
            Vector3D positionInGcrf = epochToGcrf.transformPosition(position);

            // wektor pozycji ma zawsze długość 1, więc go trzeba przeskalować bo orekit zgłupieje (traktuje to jako odległość 1m od środka ziemi)
            Vector3D spacePosition = positionInGcrf.scalarMultiply(1e12); 

            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
            GeodeticPoint stationLocation = new GeodeticPoint(Math.toRadians(station.getLatitude()), Math.toRadians(station.getLongitude()), station.getAltitude());
            TopocentricFrame stationFrame = new TopocentricFrame(earth, stationLocation, "Teleskop");

            AbsoluteDate currentDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
            
            // argument frame w metodach getElevation i getAzimuth to układ w którym jest zdefiniowana pozycja obiektu, czyli GCRF, a nie stacja obserwacyjna
            double elevation = stationFrame.getElevation(spacePosition, gcrf, currentDate);
            double azimuth = stationFrame.getAzimuth(spacePosition, gcrf, currentDate);

            return new double[]{Math.toDegrees(azimuth), Math.toDegrees(elevation)};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s (RA: %.2f, DEC: %.2f)", name, ra, dec);
    }
}