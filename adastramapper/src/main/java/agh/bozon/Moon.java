package agh.bozon;

import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.util.Date;

public class Moon {
    private static boolean isInitialized = false;

    public static void initOrekit(File appDir) {
        File orekitData = new File(appDir, "orekit-data");
        if (orekitData.exists() && orekitData.isDirectory()) {
            DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));
            isInitialized = true;
            System.out.println("Dane z orekit-data wczytane poprawnie.");
        } else {
            System.err.println("Brak danych astronomicznych w: " + orekitData.getAbsolutePath() + ". Pobierz orekit-data.zip i wypakuj do tego folderu, aby obliczać Księżyc.");
        }
    }

    //zwraca wspolrzedzne godzinowe DEC/HOUR (w stopniach) Ksiezyca
    public static double[] getDeclinationHourAngle(ObservationStation station) {
        if (!isInitialized) return null;
        try {
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
            GeodeticPoint stationLocation = new GeodeticPoint(Math.toRadians(station.getLatitude()), Math.toRadians(station.getLongitude()), station.getAltitude());
            TopocentricFrame stationFrame = new TopocentricFrame(earth, stationLocation, "Teleskop");

            CelestialBody moon = CelestialBodyFactory.getMoon();
            AbsoluteDate date = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());

            //wspolrzedzne azymutalne
            double elevation = stationFrame.getElevation(moon.getPVCoordinates(date, stationFrame).getPosition(), stationFrame, date);
            double azimuth = stationFrame.getAzimuth(moon.getPVCoordinates(date, stationFrame).getPosition(), stationFrame, date);

            //przeliczenie na wspolrzedne godzinowe
            double declination=Math.asin(Math.sin(elevation)*Math.sin(station.getLatitude()) - Math.cos(station.getLatitude())*Math.cos(elevation)*Math.cos(azimuth));
            double hourAngle=Math.atan((Math.sin(azimuth)) / (Math.cos(azimuth)*Math.sin(station.getLatitude()) + Math.tan(elevation)*Math.cos(station.getLatitude())));
            
            return new double[]{Math.toDegrees(declination), Math.toDegrees(hourAngle)};
        } catch (Exception e) {
            return null;
        }
    }
}
