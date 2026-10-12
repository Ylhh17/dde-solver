package dde;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

public class CSVExporter {
    public static void save(double[] ts, double[] ys) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить как CSV");
        fileChooser.setInitialFileName("dde_output.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));

        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("t,y(t)");
                for (int i = 0; i < ts.length; i++) {
                    writer.println(ts[i] + "," + ys[i]);
                }
                System.out.println("CSV успешно сохранён: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}






