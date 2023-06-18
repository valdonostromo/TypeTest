package com.example.monkeytype;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends Application {

    private VBox layout;
    private TextFlow textFlow;
    private List<List<Text>> wordLetters;
    private int currentWordIndex;
    private int currentLetterIndex;
    private List<Text> currentWordLetters;
    private StringBuilder typedWord;
    private List<String> generatedParagraph;
    private ComboBox<Integer> time;
    private Label timerLabel;
    private Timeline timeline;
    private int remainingTime;
    private boolean isPaused;
    private int correctChars;
    private int incorrectChars;
    private int missedChars;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Writing Test");

        ComboBox<String> language = new ComboBox<>();
        language.setPromptText("Choose language");
        File directory = new File("dictionary");
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String fileName = file.getName();
                        String nameWithoutExtension = fileName.replaceFirst("[.][^.]+$", "");
                        language.getItems().add(nameWithoutExtension);
                    }
                }
            }
        }

        layout = new VBox();
        textFlow = new TextFlow();
        textFlow.setPrefWidth(300);
        time = new ComboBox<>();
        time.setPromptText("Choose time");
        time.getItems().addAll(15, 20, 45, 60, 90, 120, 300);
        timerLabel = new Label();
        Label shortcutsLabel = new Label("Shortcuts:\nTab + Enter -> Restart Test\nCtrl + Shift + P -> Pause\nESC -> End Test");
        layout.getChildren().addAll(time, language, textFlow, timerLabel, shortcutsLabel);
        layout.setSpacing(10);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 700, 400);
        scene.setOnKeyPressed(keyEventHandler);
        stage.setScene(scene);
        stage.show();

        language.setOnAction(event -> {
            String selectedLanguage = language.getValue();
            if (selectedLanguage != null) {
                String filePath = "dictionary/" + selectedLanguage + ".txt";
                try {
                    List<String> lines = Files.readAllLines(Paths.get(filePath));
                    generatedParagraph = generateParagraph(lines);
                    displayParagraph();
                    startTypingMode();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private List<String> generateParagraph(List<String> words) {
        List<String> randomWords = new ArrayList<>();
        Random random = new Random();

        int WORD_COUNT = 30;
        for (int i = 0; i < WORD_COUNT; i++) {
            int randomIndex = random.nextInt(words.size());
            randomWords.add(words.get(randomIndex));
        }

        return randomWords;
    }

    private void displayParagraph() {
        textFlow.getChildren().clear();
        wordLetters = new ArrayList<>();
        currentWordIndex = 0;
        currentLetterIndex = 0;
        typedWord = new StringBuilder();
        correctChars = 0;
        incorrectChars = 0;
        missedChars = 0;

        for (String word : generatedParagraph) {
            List<Text> wordTexts = new ArrayList<>();
            for (int i = 0; i < word.length(); i++) {
                Text letter = new Text(String.valueOf(word.charAt(i)));
                letter.setFont(Font.font("Arial", 30));
                letter.setFill(Color.GRAY);
                wordTexts.add(letter);
                textFlow.getChildren().add(letter);
            }
            wordLetters.add(wordTexts);
            Text space = new Text(" ");
            textFlow.getChildren().add(space);
        }
    }

    private boolean isAlertOpen = false;

    private void startTypingMode() {
        Scene scene = layout.getScene();
        scene.setOnKeyTyped(event -> {
            if (!isPaused && !isAlertOpen) {
                if (timeline == null || timeline.getStatus() == Timeline.Status.STOPPED) {
                    if (time.getValue() != null) {
                        int selectedTime = time.getValue();
                        if (selectedTime != 0) {
                            startTimer(selectedTime);
                        }
                    } else {
                        isAlertOpen = true;
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Warning");
                        alert.setHeaderText("You have to choose time first!");
                        alert.setOnHidden(e -> isAlertOpen = false);
                        alert.showAndWait();
                    }
                }
                String typedCharacter = event.getCharacter();
                if (typedCharacter.matches("[A-Za-z ]")) {
                    if (typedCharacter.equals(" ")) {
                        handleSpaceKeyPress();
                    } else {
                        handleLetterKeyPress(typedCharacter);
                    }
                }
            }
        });

        Stage primaryStage = (Stage) scene.getWindow();
        primaryStage.setOnHiding(Event::consume);
    }



    private void startTimer(int timeInSeconds) {
        remainingTime = timeInSeconds;
        timerLabel.setText("Time: " + remainingTime + "s");

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), event -> {
            remainingTime--;
            timerLabel.setText("Time: " + remainingTime + "s");
            if (remainingTime <= 0) {
                timeline.stop();
                layout.getScene().setOnKeyTyped(null);
                displayStatistics();
                saveTestResults();
            }
        }));
        timeline.play();
    }

    private void handleLetterKeyPress(String typedCharacter) {
        if (currentWordIndex < wordLetters.size()) {
            currentWordLetters = wordLetters.get(currentWordIndex);
            if (currentLetterIndex < currentWordLetters.size()) {
                Text currentLetter = currentWordLetters.get(currentLetterIndex);
                if (currentLetter.getText().equals(typedCharacter)) {
                    currentLetter.setFill(Color.GREEN);
                    correctChars++;
                } else {
                    currentLetter.setFill(Color.RED);
                    incorrectChars++;
                }
                currentLetterIndex++;
                typedWord.append(typedCharacter);

                TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.1), currentLetter);
                translateTransition.setFromY(0);
                translateTransition.setToY(-10);
                translateTransition.setCycleCount(2);
                translateTransition.setAutoReverse(true);
                translateTransition.play();
            }
        }
    }

    private void handleSpaceKeyPress() {
        if (currentWordIndex < wordLetters.size()) {
            for (int i = currentLetterIndex; i < currentWordLetters.size(); i++) {
                Text letter = currentWordLetters.get(i);
                letter.setFill(Color.BLACK);
                missedChars++;
            }
            currentWordIndex++;
            currentLetterIndex = 0;
            typedWord.setLength(0);

        }

        if (currentWordIndex == wordLetters.size()) {
            timeline.stop();
            layout.getScene().setOnKeyTyped(null);
            displayStatistics();
            saveTestResults();
        }
    }

    private boolean tabPressed = false;

    private EventHandler<KeyEvent> keyEventHandler = event -> {
        if (event.getCode() == KeyCode.TAB) {
            tabPressed = true;
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER && tabPressed) {
            handleResetShortcut();
            tabPressed = false;
        }
        else if (event.getCode() == KeyCode.P && event.isControlDown() && event.isShiftDown()) {
            handlePauseShortcut();
        }else if(event.getCode() == KeyCode.ESCAPE)
            handleEscapeKeyPress();
        else {
            tabPressed = false;
        }
    };
    private void handlePauseShortcut() {
        if (timeline != null && timeline.getStatus() == Timeline.Status.RUNNING) {
            isPaused = true;
            timeline.pause();
        } else if (timeline != null && timeline.getStatus() == Timeline.Status.PAUSED) {
            isPaused = false;
            timeline.play();
        }
    }
    private void handleResetShortcut() {
        int selectedTime = time.getValue();
        if (selectedTime != 0) {
            if (timeline != null) {
                timeline.stop();
            }
            startTimer(selectedTime);
        }
        resetWordColors();
        isPaused = false;

        currentWordIndex = 0;
        currentLetterIndex = 0;
        typedWord.setLength(0);

        correctChars = 0;
        incorrectChars = 0;
        missedChars = 0;
    }
    private void handleEscapeKeyPress() {
        if (timeline != null) {
            timeline.stop();
        }
        resetWordColors();
        isPaused = true;
        displayStatistics();
        saveTestResults();
        currentWordIndex = 0;
        currentLetterIndex = 0;
        typedWord.setLength(0);
    }
    private void resetWordColors() {
        for (List<Text> word : wordLetters) {
            for (Text letter : word) {
                letter.setFill(Color.GRAY);
            }
        }
    }

    private void displayStatistics() {
        double totalChars = correctChars + incorrectChars + missedChars;
        double wpm = (correctChars / 5.0) / (time.getValue() / 60.0);
        int accuracy = Math.round((float) (correctChars * 100) / (float) totalChars);

        Label wpmLabel = new Label("WPM: " + (int) Math.round(wpm));
        Label statsLabel = new Label("Correct/Incorrect/Missed: " + correctChars + "/" + incorrectChars + "/" + missedChars);
        Label accuracyLabel = new Label("Accuracy: " + accuracy + "%");

        VBox statisticsBox = new VBox(wpmLabel, statsLabel, accuracyLabel);
        statisticsBox.setSpacing(10);
        layout.getChildren().add(statisticsBox);
    }

    private void saveTestResults() {
        String fileName = getCurrentDateTimeAsString() + ".txt";
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < wordLetters.size(); i++) {
            List<Text> word = wordLetters.get(i);
            StringBuilder sb = new StringBuilder();
            for (Text letter : word) {
                sb.append(letter.getText());
            }
            if (i < currentWordIndex) {
                double wpm = (sb.length() / 5.0) / (time.getValue() / 60.0);
                lines.add(sb + " -> " + (int) Math.round(wpm) + "wpm");
            }
        }
        try {
            Files.write(Paths.get(fileName), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private String getCurrentDateTimeAsString() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        return now.format(formatter);
    }
}

