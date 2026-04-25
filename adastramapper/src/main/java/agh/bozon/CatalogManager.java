package agh.bozon;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CatalogManager {

    public static List<AstroObject> loadCatalog(File file) {
        List<AstroObject> objects = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    objects.add(new AstroObject(parts[0].trim(), Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim())));
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas wczytywania katalogu: " + e.getMessage());
        }
        return objects;
    }

    public static void saveCatalog(File file, List<AstroObject> objects) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("# Name, RA, DEC\n");
            for (AstroObject obj : objects) {
                bw.write(String.format("%s,%.4f,%.4f\n", obj.getName(), obj.getRa(), obj.getDec()));
            }
            System.out.println("Zapisano do: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisu: " + e.getMessage());
        }
    }
}