package com.amadeus;
import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.image.Image;
import com.amadeus.Main;
public final class InfoTool {

    /** Logger */
    public static final Logger logger = Logger.getLogger(InfoTool.class.getName());

    // --------------------------------------------------------------------------------------------------------------
    private InfoTool() {
    }
    public static final String getBasePathForClass(Class<?> classs) {

        // Local variables
        File file;
        String basePath = "";
        boolean failed = false;

        // Let's give a first try
        try {
            file = new File(classs.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

            basePath = ( file.isFile() || file.getPath().endsWith(".jar") || file.getPath().endsWith(".zip") ) ? file.getParent() : file.getPath();
        } catch (URISyntaxException ex) {
            failed = true;
            Logger.getLogger(classs.getName()).log(Level.WARNING, "Cannot figure out base path for class with way (1): ", ex);
        }

        // The above failed?
        if (failed)
            try {
                file = new File(classs.getClassLoader().getResource("").toURI().getPath());
                basePath = file.getAbsolutePath();

                // the below is for testing purposes...
                // starts with File.separator?
                // String l = local.replaceFirst("[" + File.separator +
                // "/\\\\]", "")
            } catch (URISyntaxException ex) {
                Logger.getLogger(classs.getName()).log(Level.WARNING, "Cannot figure out base path for class with way (2): ", ex);
            }

        // fix to run inside Eclipse
        if (basePath.endsWith(File.separator + "lib") || basePath.endsWith(File.separator + "bin") || basePath.endsWith("bin" + File.separator)
                || basePath.endsWith("lib" + File.separator)) {
            basePath = basePath.substring(0, basePath.length() - 4);
        }
        // fix to run inside NetBeans
        if (basePath.endsWith(File.separator + "build" + File.separator + "classes")) {
            basePath = basePath.substring(0, basePath.length() - 14);
        }
        // end fix
        if (!basePath.endsWith(File.separator))
            basePath += File.separator;

        return basePath;
    }

    public static boolean isReachableByPing(String host) {
        try {

            // Start a new Process
            Process process = Runtime.getRuntime().exec("ping -" + ( System.getProperty("os.name").toLowerCase().startsWith("windows") ? "n" : "c" ) + " 1 " + host);

            //Wait for it to finish
            process.waitFor();

            //Check the return value
            return process.exitValue() == 0;

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.INFO, null, ex);
            return false;
        }
    }
    public static String getFileSizeEdited(long bytes) {

        //Find it
        int kilobytes = (int) ( bytes / 1024 ) , megabytes = kilobytes / 1024;
        if (kilobytes < 1024)
            return kilobytes + " KB";
        else if (kilobytes > 1024)
            return megabytes + " MB + " + ( kilobytes - ( megabytes * 1024 ) ) + " KB";

        return "error";

    }

}
