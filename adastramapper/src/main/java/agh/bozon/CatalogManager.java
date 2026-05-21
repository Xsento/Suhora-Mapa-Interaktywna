package agh.bozon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CatalogManager {

    // Odczyt plików cat
    public static List<AstroObject> loadCatalog(List<File> files, List<AstroObject> loadedObjects) {
        for (File file : files) {
            try (FileReader fileReader = new FileReader(file)) {
                char[] buffer = new char[100];
                int length = fileReader.read(buffer);

                if (length != -1) {
                    String content = new String(buffer, 0, length);
                    String[] lines = content.split("\\r?\\n");
                    String [] line1 = lines[0].trim().split("\\s+");
                    String [] line2 = lines[1].trim().split("\\s+");
                    String name = line1[0] + " " + line1[1];

                    for (AstroObject obj : loadedObjects) {
                        if (name.equals(obj.getName())) {
                            System.out.println("Obiekt " + name + " już został załadowany");
                            System.out.println("Liczba załadowanych obiektów: " + loadedObjects.size());
                            return loadedObjects;
                        }
                    }

                    String comment = line1[2] + " ";
                    for (int i = 3; i < line1.length; i++){
                        comment += line1[i] + " ";
                    }

                    System.out.println(name);
                    System.out.println(comment);
                    double raHours = Double.parseDouble(line2[0]) + Double.parseDouble(line2[1]) * (1.0 / 60) + Double.parseDouble(line2[2]) * (1.0 / 3600);
                    double ra = raHours * 15;  // Zamiana godzin na stopnie
                    System.out.println("RA in degrees: " + ra);
                    // zakładając że deklinacja jest podana w stopniach
                    double dec = Double.parseDouble(line2[3]) + Double.parseDouble(line2[4]) * (1.0 / 60) + Double.parseDouble(line2[5]) * (1.0 / 3600);
                    System.out.println("DEC in degrees: " + dec); 
                    double epoch = Double.parseDouble(line2[6]);
                    System.out.println("Epoch: " + epoch);
                    loadedObjects.add(new AstroObject(name, comment, ra, dec, epoch));
                    System.out.println("Liczba załadowanych obiektów: " + loadedObjects.size());
                }

            } catch (FileNotFoundException fileNotFoundException) {
                System.out.println("Nie znaleziono pliku: " + fileNotFoundException.getMessage());
            } catch (IOException iOException) {
                System.out.println("Błąd podczas odczytu pliku: " + iOException.getMessage());
            }
        }
        return loadedObjects;
    }

    public static void saveCatalog(File file, List<AstroObject> objects) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("# Name, RA, DEC\n");
            for (AstroObject obj : objects) {
                bw.write(String.format("%s, %s, %.4f, %.4f, %.2f\n", obj.getName(), obj.getComment(), obj.getRa(), obj.getDec(), obj.getEpoch()));
            }
            System.out.println("Zapisano do: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisu: " + e.getMessage());
        }
    }
}