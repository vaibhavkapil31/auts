package com.amadeus;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.PathTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.amadeus.DownloadModeController;
import com.amadeus.DownloadService;
import com.amadeus.ExportZipService;
import com.amadeus.ActionTool;
import com.amadeus.InfoTool;
import com.amadeus.NotificationType;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main extends Application {


    //================Variables================

    /**
     * This is the folder where the update will take place [ obviously the
     * parent folder of the application]
     */
    private static final File updateFolder = new File(InfoTool.getBasePathForClass(Main.class));

    /**
     * Download update as a ZIP Folder , this is the prefix name of the ZIP
     * folder
     */
    private static String foldersNamePrefix;

    /** Update to download */
    private static int update;

    /** DOWNLOAD URL FROM THE REST SERVICE */
    private static String downloadUrl="http://localhost:8080/downloadFile/Sample.zip";

    /** SERVERVERSION.JSON download link */
    private static String versionUrl="http://localhost:8080/downloadFile/serverversion.json";

    /** The name of the application you want to update */
    private String applicationName="Sape";
    /** The system Update version number */
    private static long sysv;
    /** The Server Update version number */
    private static long servv;

    //================Listeners================

    //Create a change listener
    ChangeListener<? super Number> listener = (observable , oldValue , newValue) -> {
        if (newValue.intValue() == 1)
            exportUpdate();
    };
    //Create a change listener
    ChangeListener<? super Number> listener2 = (observable , oldValue , newValue) -> {
        if (newValue.intValue() == 1)
            packageUpdate();
    };

    //================Services================

    DownloadService downloadService;
    ExportZipService exportZipService;

    //=============================================

    private Stage window;
    private static DownloadModeController downloadMode = new DownloadModeController();
    //---------------------------------------------------------------------

    @Override
    public void start(Stage primaryStage) throws Exception {
        sysv=fetchSystemVersion();
        servv=dloadsystemVersion();
        update =(int) sysv;

        //We need this in order to restart the update when it fails
        System.out.println("Application Started");

        // --------Window---------
        window = primaryStage;
        window.setResizable(true);
        window.centerOnScreen();
        window.setOnCloseRequest(exit -> {

            //Check
            if (exportZipService != null && exportZipService.isRunning()) {
                ActionTool.showNotification("Message", "Can not exit right now as it will corrupt the update", Duration.seconds(5), NotificationType.WARNING);
                exit.consume();
                return;
            }

            //Question
            if (!ActionTool.doQuestion("Are you sure you want to exit " + applicationName + " Updater?", window))
                exit.consume();
            else {

                //Delete the ZIP Folder
                deleteZipFolder();
                try {
                    rollback();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Exit the application
                System.exit(0);
            }

        });

        // Scene
        Scene scene = new Scene(downloadMode);
        window.setScene(scene);

        //Show
       // window.show();
        if(servv==-1)
        {
            ActionTool.giveAlert("Cannot fetch the Server Version. Please check your Internet Connection and try again",window);
            System.exit(0);
        }
        if(isUpdateAvailable(sysv,servv))
        {
            window.close();
            if(ActionTool.doQuestion("A new Update "+servv+" is available for download"+"\n You are Currently running Version Number "+sysv+"\n\n Do you want to Update?", window)) {
                window.centerOnScreen();  window.show();
                prepareForUpdate("Sape");
            }
            else
            {
                window.show();
                ActionTool.showNotification("Amadeus SAPE Auto Updater","Update Rejected by User",Duration.seconds(3),NotificationType.WARNING);
                ActionTool.giveAlert("Update Rejected By the User",window);
                System.exit(0);
            }
        }
        else
        {
            ActionTool.giveAlert("Your System is Up to date!",window);
            System.out.println("Application Exited");
            System.exit(0);
        }

    }

    //-------------------------------------------------------------------------------------------------------------------------------

    public void prepareForUpdate(String applicationName) {
        this.applicationName = applicationName;
        window.setTitle(applicationName + " Updater");

        //FoldersNamePrefix
        foldersNamePrefix = updateFolder.getAbsolutePath() + File.separator + applicationName + " Update Package " + update;

        //Check the Permissions
        if (checkPermissions()) {
            downloadMode.getProgressLabel().setText("Checking permissions");
            downloadUpdate(downloadUrl);
        } else {

            //Update
            downloadMode.getProgressBar().setProgress(-1);
            downloadMode.getProgressLabel().setText("Please close the updater");

            //Show Message
            ActionTool.showNotification("Permission Denied",
                    "Application has no permission to write inside this folder:\n [ " + updateFolder.getAbsolutePath()
                            + " ]\n -> You can download " + applicationName + " manually :) ]",
                    Duration.minutes(1), NotificationType.ERROR);
        }
    }
    public boolean checkPermissions() {

        //Check for permission to Create
        try {
            File sample = new File(updateFolder.getAbsolutePath() + File.separator + "empty123123124122354345436.txt");
            /*
             * Create and delete a dummy file in order to check file
             * permissions. Maybe there is a safer way for this check.
             */
            sample.createNewFile();
            sample.delete();
        } catch (IOException e) {
            //Error message shown to user. Operation is aborted
            return false;
        }

        //Also check for Read and Write Permissions
        return updateFolder.canRead() && updateFolder.canWrite();
    }

    /** Try to download the Update */
    private void downloadUpdate(String downloadURL) {

        if (InfoTool.isReachableByPing("www.google.com")) {

            //Download it
            try {
                //Delete the ZIP Folder
                deleteZipFolder();

                //Create the downloadService
                downloadService = new DownloadService();

                //Add Bindings
                downloadMode.getProgressBar().progressProperty().bind(downloadService.progressProperty());
                downloadMode.getProgressLabel().textProperty().bind(downloadService.messageProperty());
                downloadMode.getProgressLabel().textProperty().addListener((observable , oldValue , newValue) -> {
                    //Give try again option to the user
                    if (newValue.toLowerCase().contains("failed")) {
                        downloadMode.getFailedStackPane().setVisible(true);
                        downloadMode.getPane().setVisible(false);
                    }
                });
                downloadMode.getProgressBar().progressProperty().addListener(listener);
                window.setTitle("Downloading ( " + this.applicationName + " ) Update -> " + this.update);

                //Start
                downloadService.startDownload(new URL(downloadURL), Paths.get(foldersNamePrefix + ".zip"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        } else {
            //Update
            downloadMode.getProgressBar().setProgress(-1);
            downloadMode.getProgressLabel().setText("No internet Connection,please check your connection and try again");

            //Delete the ZIP Folder
            deleteZipFolder();

            //Give try again option to the user
            downloadMode.getPane().setVisible(false);
            downloadMode.getFailedStackPane().setVisible(true);
        }
    }

    /** Exports the Update ZIP Folder */
    private void exportUpdate() {

        //Create the com.amadeus.ExportZipService
        exportZipService = new ExportZipService();

        //Remove Listeners
        downloadMode.getProgressBar().progressProperty().removeListener(listener);

        //Add Bindings
        downloadMode.getProgressBar().progressProperty().bind(exportZipService.progressProperty());
        downloadMode.getProgressLabel().textProperty().bind(exportZipService.messageProperty());
        downloadMode.getProgressBar().progressProperty().addListener(listener2);

        //Start it
        exportZipService.exportZip(foldersNamePrefix + ".zip", updateFolder.getAbsolutePath());

    }
    private void packageUpdate() {

        //Remove Listeners
        downloadMode.getProgressBar().progressProperty().removeListener(listener2);

        //Bindings
        downloadMode.getProgressBar().progressProperty().unbind();
        downloadMode.getProgressLabel().textProperty().unbind();

        //Packaging
        downloadMode.getProgressBar().setProgress(-1);
        downloadMode.getProgressLabel().setText("Starting " + applicationName + "...");

        //Delete the ZIP Folder
        deleteZipFolder();
        restartApplication(applicationName);

    }

    //---------------------------------------------------------------------------------------
    public static void restartApplication(String appName) {

        new Thread(() -> {
            Platform.runLater(() -> ActionTool.showNotification("Starting " + appName,
                    "\n\tThe Installer will have opened by now. Update was Installed Successfully!", Duration.seconds(25),
                    NotificationType.INFORMATION));
            updateJson((int) servv);
            launchapp();
            downloadMode.getPane().setVisible(false);
            downloadMode.getPane2().setVisible(true);

        }, "Start Application Thread").start();
    }
    public static boolean deleteZipFolder() {
        return new File(foldersNamePrefix + ".zip").delete();
    }
    public static String CurrDirectory() {

        String path = System.getProperty("user.dir");
        return path;
    }
    public long fetchSystemVersion()
    {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(CurrDirectory()+"\\src\\main\\resources\\Version.json"))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
            JSONObject o=(JSONObject) obj;
            reader.close();

            return (long) o.get("version");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public static void updateJson(int vernum) {
        JSONParser jsonParser = new JSONParser();
        try (FileWriter file = new FileWriter(CurrDirectory()+"\\src\\main\\resources\\Version.json")) {
            JSONObject o=new JSONObject();
            o.put("version",vernum);
            file.write(o.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileWriter file = new FileWriter(updateFolder.getPath()+"\\Version.json")) {
            JSONObject o=new JSONObject();
            o.put("version",vernum);
            file.write(o.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public long dloadsystemVersion(){
        String url = versionUrl;

        try {
            downloadUsingNIO(url, CurrDirectory()+"\\src\\main\\resources\\serverversion.json");

            downloadUsingStream(url, CurrDirectory()+"\\src\\main\\resources\\serverversion.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(CurrDirectory()+"\\src\\main\\resources\\serverversion.json"))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
            JSONObject o=(JSONObject) obj;
            reader.close();

            return (long) o.get("version");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;

    }
    private static void downloadUsingStream(String urlStr, String file) throws IOException{
        URL url = new URL(urlStr);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int count=0;
        while((count = bis.read(buffer,0,1024)) != -1)
        {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
    }

    private static void downloadUsingNIO(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }
    public static void launchapp()
    {
        downloadMode.getFinishButton().setDisable(false);
        try {
            Desktop.getDesktop().open(new File(updateFolder.getPath() +"//1A-PM_CUPPS_Setup_4_0_52.exe"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void rollback() throws IOException {
        updateJson(update);
        FileUtils.deleteDirectory(new File(updateFolder.getPath()));
        System.out.println("Deleted");
    }
    public boolean isUpdateAvailable(long sysv,long servv)
    {
        if(sysv<servv)
            return true;
        else
            return false;
    }

    public static void main(String[] args) {

        launch(args);
    }

}
