package tests.keyboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class ScenarioMaker {
    public static void main(String[] args) {
        Impl app = new Impl();
        app.run();
    }

    public static class Impl {
        final static String START_BUTTON_TEXT = "\uD83D\uDD34(Press to start)";
        final static String STOP_BUTTON_TEXT = "\u23F9(Press to stop)";

        private final JFrame myFrame = new JFrame("Keyboard test scenario generator");

        private final JButton mySaveButton = new JButton("Save...");
        private final JButton myLoadButton = new JButton("Load...");
        private final JButton myDeleteScenarioButton = new JButton("Delete");
        private final JButton myResetCompositionButton = new JButton("Reset composition");
        private final JButton myResetButton = new JButton("New");
        private final JLabel myStartButton = new JLabel(START_BUTTON_TEXT);

        private final DefaultListModel<Scenario> myScenariosList = new DefaultListModel<>();
        private final JList<Scenario> myScenariosListComponent = new JList<>(myScenariosList);

        private final DefaultListModel<Scenario.EventDataJava> myEventsList = new DefaultListModel<>();
        private final JList<Scenario.EventDataJava> myEventListComponent = new JList<>(myEventsList);

        private final DefaultListModel<Scenario.EventDataJava> myCompositionEventsList = new DefaultListModel<>();
        private final JList<Scenario.EventDataJava> myCompositionEventListComponent = new JList<>(myCompositionEventsList);

        private final List<JComponent> activeComponents = Arrays.asList(mySaveButton, myLoadButton,
                myDeleteScenarioButton, myResetCompositionButton, myResetButton, myScenariosListComponent,
                myEventListComponent);
        private final ScenarioCompositor myScenarioCompositor = new ScenarioCompositor();

        public void run() {
            SwingUtilities.invokeLater(this::makeUI);
            SwingUtilities.invokeLater(this::init);
        }

        private void makeUI() {
            myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            myFrame.getContentPane().setLayout(new BoxLayout(myFrame.getContentPane(), BoxLayout.X_AXIS));

            JPanel scenarioPanel = new JPanel();
            scenarioPanel.setBorder(BorderFactory.createTitledBorder("Scenarios"));
            scenarioPanel.setLayout(new BoxLayout(scenarioPanel, BoxLayout.Y_AXIS));
            scenarioPanel.add(new JScrollPane(myScenariosListComponent));
            myFrame.add(scenarioPanel);

            JPanel scenarioButtonsPanel = new JPanel();
            scenarioButtonsPanel.setLayout(new BoxLayout(scenarioButtonsPanel, BoxLayout.X_AXIS));
            scenarioButtonsPanel.add(mySaveButton);
            scenarioButtonsPanel.add(myLoadButton);
            scenarioButtonsPanel.add(myResetButton);
            scenarioButtonsPanel.add(Box.createHorizontalGlue());
            myStartButton.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            scenarioButtonsPanel.add(myStartButton);
            scenarioButtonsPanel.add(Box.createHorizontalGlue());
            scenarioButtonsPanel.add(myDeleteScenarioButton);
            scenarioButtonsPanel.add(myResetCompositionButton);
            scenarioPanel.add(scenarioButtonsPanel);

            JPanel compositionPanel = new JPanel();
            compositionPanel.setLayout(new BoxLayout(compositionPanel, BoxLayout.Y_AXIS));
            compositionPanel.add(new Label("Composition:"));
            compositionPanel.add(new JScrollPane(myCompositionEventListComponent));
            scenarioPanel.add(compositionPanel);
            compositionPanel.setMaximumSize(compositionPanel.getPreferredSize());

            JPanel eventsPanel = new JPanel();
            eventsPanel.setBorder(BorderFactory.createTitledBorder("Events in the scenario"));
            eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));
            myCompositionEventListComponent.setVisibleRowCount(5);
            eventsPanel.add(new JScrollPane(myEventListComponent));
            myFrame.add(eventsPanel);

            JPanel eventsButtonsPanel = new JPanel();
            eventsButtonsPanel.setLayout(new BoxLayout(eventsButtonsPanel, BoxLayout.X_AXIS));
            eventsButtonsPanel.add(Box.createHorizontalGlue());
            eventsPanel.add(eventsButtonsPanel);

            myFrame.pack();
            myFrame.setVisible(true);
        }

        private void init() {
            myStartButton.addMouseListener(new MouseAdapter() {
                boolean recording = false;

                @Override
                public void mouseClicked(MouseEvent e) {
                    recording = !recording;
                    setRecording(recording);
                    super.mouseClicked(e);
                }
            });

            myResetButton.addActionListener(e -> {
                myScenariosList.clear();
                myScenarioCompositor.reset();
            });

            myDeleteScenarioButton.addActionListener(e -> {
                int selected = myScenariosListComponent.getSelectedIndex();
                if (selected >= 0) {
                    myScenariosList.remove(selected);
                    myScenariosListComponent.setSelectedIndex(Math.min(selected, myScenariosList.size() - 1));
                }
            });

            myScenariosListComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            myScenariosListComponent.addListSelectionListener(ignored -> {
                myEventsList.clear();
                int selected = myScenariosListComponent.getSelectedIndex();
                if (selected >= 0)
                    myEventsList.addAll(myScenariosList.get(selected).eventsJava);
            });

            myLoadButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showOpenDialog(myFrame) == JFileChooser.APPROVE_OPTION) {
                    String jsonText = null;
                    try {
                        jsonText = Files.readString(fileChooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(myFrame,
                                "Failed to write the file:\n" + fileChooser.getSelectedFile(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    Type typeToken = new TypeToken<ArrayList<Scenario>>() {}.getType();
                    ArrayList<Scenario> scenarios = new Gson().fromJson(jsonText, typeToken);
                    myScenariosList.clear();
                    myScenariosList.addAll(scenarios);
                }
            });

            mySaveButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File("scenario_mac.json"));
                if (fileChooser.showSaveDialog(myFrame) == JFileChooser.APPROVE_OPTION) {
                    String jsonString = new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(myScenariosList.toArray());
                    File outFile = fileChooser.getSelectedFile();
                    try {
                        Files.writeString(outFile.toPath(), jsonString, StandardCharsets.UTF_8);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(myFrame,
                                "Failed to write the file:\n" + outFile,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            myResetCompositionButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    myScenarioCompositor.reset();
                }
            });
        }


        private void setRecording(boolean start) {
            myStartButton.setText(start ? STOP_BUTTON_TEXT : START_BUTTON_TEXT);
            for (JComponent c : activeComponents) {
                c.setEnabled(!start);
            }
            if (start) {
                DefaultFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(myScenarioCompositor);
            } else {
                DefaultFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(myScenarioCompositor);
            }
        }

        class ScenarioCompositor implements KeyEventDispatcher {
            final Set<Integer> pressedKeys = new HashSet<>();
            List<Scenario.EventDataJava> keyEvents = new ArrayList<>();
            String scenarioName = "";

            void reset() {
                pressedKeys.clear();
                keyEvents = new ArrayList<>();
                scenarioName = "";
                myCompositionEventsList.clear();
            }

            void commit() {
                int i = myScenariosListComponent.getSelectedIndex();
                myScenariosList.insertElementAt(new Scenario(scenarioName, keyEvents), i + 1);
                myScenariosListComponent.setSelectedIndex(i + 1);
                myScenariosListComponent.ensureIndexIsVisible(i + 1);
                reset();
            }

            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                System.err.println("dispatchKeyEvent: " + e);
                if (e.getKeyCode() == 0 && e.getKeyChar() == 0xFFFF) {
                    // fn key
                    return false;
                }
                keyEvents.add(new Scenario.EventDataJava(e));
                myCompositionEventsList.clear();
                myCompositionEventsList.addAll(keyEvents);
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    pressedKeys.add(e.getKeyCode());
                    appendName(KeyEvent.getKeyText(e.getKeyCode()));
                } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                    pressedKeys.remove(e.getKeyCode());
                    if (pressedKeys.isEmpty() && keyEvents.get(0).keyCode == keyEvents.get(keyEvents.size() - 1).keyCode) {
                        commit();
                    }
                }
                e.consume();
                return true;
            }

            private void appendName(String keyName) {
                if (scenarioName.isEmpty()) {
                    scenarioName = keyName;
                } else {
                    scenarioName = scenarioName + "+" + keyName;
                }
            }
        }
    }
}
