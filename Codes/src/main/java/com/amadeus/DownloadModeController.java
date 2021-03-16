package com.amadeus;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import com.amadeus.Main;
import com.amadeus.ActionTool;
import com.amadeus.InfoTool;

public class DownloadModeController extends BorderPane {

    //-----------------------------------------------------

    @FXML
    private Rectangle rectangle;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private StackPane failedStackPane;

    @FXML
    private Button tryAgainButton;

    @FXML
    private Button downloadManually;

    @FXML
    private StackPane mainpane;

    @FXML
    private Pane pane;

    @FXML
    private Pane pane2;

    @FXML
    private Button finishbutton;



    // -------------------------------------------------------------

    /** The logger. */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Constructor.
     */
    public DownloadModeController() {

        // ------------------------------------FXMLLOADER ----------------------------------------
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/DownloadModeController.fxml"));
        loader.setController(this);
        loader.setRoot(this);

        try {
            loader.load();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "", ex);
        }

    }
    @FXML
    private void initialize() {
        //Finish Pane
       // mainpane.setVisible(false);
        pane2.setVisible(false);
        finishbutton.setDisable(true);

        //-- failedStackPane
        failedStackPane.setVisible(false);

        //-- tryAgainButton
        tryAgainButton.setOnAction(a -> {
            System.exit(0);
        });

        //== Download Manually
        downloadManually.setOnAction(a -> ActionTool.openWebSite("https://amadeus.com/en"));
        // Finish Button
        finishbutton.setOnAction(a -> closeApp());
    }

    public ProgressIndicator getProgressBar() {
        return progressBar;
    }
    public Label getProgressLabel() {
        return progressLabel;
    }
    public Pane  getPane(){return pane;}
    public Pane  getPane2(){return pane2;}
    public Button getFinishButton(){return finishbutton;}
    public StackPane getFailedStackPane() {
        return failedStackPane;
    }
    public StackPane getMainpane(){return mainpane;}
    public Button getDownloadManually() {
        return downloadManually;
    }


    public void closeApp()
    {
        System.out.println("Successfully Updated");
        System.exit(0);
    }
}

