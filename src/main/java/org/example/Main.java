package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Services.interview.InterviewReminderScheduler;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("\n════════════════════════════════════════════════════════");
        System.out.println("║ [Main] Application starting...");
        System.out.println("════════════════════════════════════════════════════════\n");

        System.out.println("[Main] Loading Login.fxml...");
        Parent root = FXMLLoader.load(getClass().getResource("/views/user/Login.fxml"));
        Scene scene = new Scene(root);
        System.out.println("[Main] ✅ Login.fxml loaded successfully");

        // If you use a SceneManager, initialize it once
        System.out.println("[Main] Initializing SceneManager...");
        Utils.SceneManager.init(stage, scene);
        System.out.println("[Main] ✅ SceneManager initialized");

        stage.setTitle("RH Project");
        stage.setScene(scene);

        // ✅ Good default window size
        stage.setWidth(1000);
        stage.setHeight(720);

        // ✅ Prevent tiny ugly window
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        stage.centerOnScreen();
        stage.setResizable(true); // put false if you want fixed size

        // ✅ START INTERVIEW REMINDER SCHEDULER
        System.out.println("[Main] Starting Interview Reminder Scheduler...");
        InterviewReminderScheduler.start();
        System.out.println("[Main] ✅ Scheduler started\n");

        // ✅ STOP SCHEDULER ON APP CLOSE
        stage.setOnCloseRequest(e -> {
            System.out.println("\n[Main] Closing application...");
            InterviewReminderScheduler.stop();
            System.out.println("[Main] ✅ Scheduler stopped");
        });

        stage.show();
        System.out.println("[Main] ✅ Application window shown\n");

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("UNCAUGHT in thread " + t.getName());
            e.printStackTrace();
        });
    }

    public static void main(String[] args) {
        System.out.println("\n════════════════════════════════════════════════════════");
        System.out.println("║ [Main] RH Application - Starting Main Process");
        System.out.println("════════════════════════════════════════════════════════\n");
        launch(args);
    }
}