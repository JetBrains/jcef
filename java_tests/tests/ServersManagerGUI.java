package tests;

import com.jetbrains.cef.remote.NativeServerManager;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.ThriftTransport;
import com.jetbrains.cef.remote.thrift.transport.TTransportException;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.handler.CefAppHandler;
import org.cef.misc.CefLog;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ServersManagerGUI {
    JFrame frame;
    DefaultListModel<Integer> listModel;
    JList<Integer> numberList;

    public static void main(String[] args) {
        System.out.println("Start GUI for NativeServerManager.");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new ServersManagerGUI().initUI();
        });
    }

    private void initUI() {
        frame = new JFrame("CEF servers manager");
        frame.setLayout(new BorderLayout());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CefApp.getInstance().dispose();
            }
        });

        // Create model and populate with port numbers
        listModel = new DefaultListModel<>();
        java.util.List<Integer> numbers = NativeServerManager.listRunningInstancesPorts();
        numbers.forEach(listModel::addElement);

        // Create JList with model
        numberList = new JList<>(listModel);
        numberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        numberList.setVisibleRowCount(8);
        numberList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        // Optional: customize cell appearance
        numberList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setText(String.format("%2d", value)); // right-align 2-digit numbers
                return label;
            }
        });

        // 🔑Add click handler
        numberList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    handleSingleClick(e);
                } else if (e.getClickCount() == 2) {
                    handleDoubleClick(e);
                }
            }
        });

        // Optional: also respond to Enter key (accessibility)
        numberList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !numberList.isSelectionEmpty()) {
                    int value = numberList.getSelectedValue();
                    JOptionPane.showMessageDialog(
                            frame,
                            "✅ Selected: " + value + "\n(Square: " + (value * value) + ")",
                            "Info",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });

        // 1. List of running servers
        JScrollPane scrollPane = new JScrollPane(numberList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Running cef_server instances"));
        frame.add(scrollPane, BorderLayout.NORTH);

        // 2. Panel for starting cef_server instances
        PathSelectionPanel pathSelectionPanel = new PathSelectionPanel();
        frame.add(pathSelectionPanel, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null); // center on screen
    }

    private void handleSingleClick(MouseEvent e) {
        int index = numberList.locationToIndex(e.getPoint());
        if (index >= 0 && index < listModel.size()) {
            numberList.setSelectedIndex(index);
            // int value = listModel.get(index);
            // System.out.println("Selected: " + value);
        }
    }

    private void handleDoubleClick(MouseEvent e) {
        int index = numberList.locationToIndex(e.getPoint());
        if (index >= 0 && index < listModel.size()) {
            int value = listModel.get(index);
            JFrame sc = new ServerControls(value);
            sc.setSize(800, 600);
            sc.setVisible(true);
            sc.setLocationRelativeTo(null); // center on screen
        }
    }

    // NOTE: user must manually call closeTransport() after usage.
    static RpcExecutor connect(int port) {
        ThriftTransport serverPort = new ThriftTransport(port);
        try {
            RpcExecutor result = new RpcExecutor().openTransport(serverPort);
            return result;
        } catch (TTransportException e) {
            CefLog.Debug("Exception when trying to connect server, err: %s", e.getMessage());
        }
        return null;
    }

    private static boolean startCefServerAndWait(String exePath, int port, String logPath, int logLevel) {
        ThriftTransport thriftTransport = new ThriftTransport(port);
        String runningRoot = NativeServerManager.isRunning(thriftTransport);
        if (runningRoot != null) {
            CefLog.Debug("cef_server instance is already running on port %d, root=%s", port, runningRoot);
            return false;
        }

        File exeFile = new File(exePath);
        if (!exeFile.exists() || !exeFile.isFile()) {
            CefLog.Debug("File '%s' doesn't exist (or it's directory).", exePath);
            return false;
        }

        // TODO: support AppHandler, args and settings
        CefAppHandler appHandler = null;
        String[] args = null;
        CefSettings settings = new CefSettings();
        final boolean success = NativeServerManager.startProcessAndWait(exeFile, thriftTransport, appHandler, args, settings, logPath, logLevel, false, 20000);
        return success;
    }

    private static String getCurrentRuntimePath() {
        final String javaHome = System.getProperty("java.home");
//        if (OS.isWindows()) return javaHome + "/bin/java.exe";
//        if (OS.isMacintosh()) return javaHome + "/bin/java";
//        if (OS.isLinux()) return javaHome + "/bin/java";
        return javaHome;
    }

    private static String getExeName() {
        if (OS.isWindows())
            return "cef_server.exe";
        return "cef_server";
    }

    private static JPanel addLabelForComponent(JComponent component, String labelText) {
        JLabel label = new JLabel(labelText);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label,BorderLayout.WEST);
        panel.add(component,BorderLayout.CENTER);
        return panel;
    }

    private static JPanel creatGroupBox(String boxText, int boxAxis) {
        JPanel gb = new JPanel();
        gb.setLayout(new BoxLayout(gb, boxAxis));
        Border blackLine = BorderFactory.createLineBorder(Color.BLACK);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                blackLine, boxText
        );
        titledBorder.setTitleJustification(TitledBorder.LEFT);
        gb.setBorder(titledBorder);
        return gb;
    }

    private static class PathSelectionPanel extends JPanel {
        public PathSelectionPanel() {
            // 1. Path selection panel
            JTextField pathField = new JTextField(getCurrentRuntimePath(), 100);
            JButton pathSelect = new JButton("Select folder");
            pathSelect.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.showOpenDialog(null);
                String path = chooser.getSelectedFile().getAbsolutePath();
                pathField.setText(path);
            });
            // TODO: remember last 100 path-items and show dropdown list with them when user clicks on pathField
            JPanel pathPanel = new JPanel(new BorderLayout());
            pathPanel.add(pathField, BorderLayout.CENTER);
            pathPanel.add(pathSelect, BorderLayout.EAST);

            // 2. Arguments and params panel
            // log level and path
            JTextField logLevelField = new JTextField("trace", 10);
            logLevelField.setSize(new Dimension(100, logLevelField.getSize().height));
            JTextField logPathField = new JTextField("stderr", 128);
            JPanel gbLogging = creatGroupBox("Logging options", BoxLayout.Y_AXIS);
            gbLogging.add(addLabelForComponent(logLevelField, "Level"));
            gbLogging.add(addLabelForComponent(logPathField, "Path "));

            // port selection
            int defaultPort = 9999;
            try {
                ServerSocket ss = new ServerSocket(defaultPort, 0, InetAddress.getByName(null));
                ss.close();
            } catch (IOException e) {
                defaultPort = ThriftTransport.findFreePort();
            }
            JTextField portField = new JTextField(String.format("%d", defaultPort), 10);
            JButton freePortButton = new JButton("Find free port");
            freePortButton.addActionListener(e -> portField.setText(String.format("%d", ThriftTransport.findFreePort())));
            JPanel gbPort = creatGroupBox("Transport options", BoxLayout.X_AXIS);
            gbPort.add(addLabelForComponent(portField, "Port"));
            gbPort.add(freePortButton);
            gbPort.add(Box.createHorizontalGlue());

            // run button
            JButton runButton = new JButton("Run");
            runButton.addActionListener(e -> {
                File exeFile = findCefServerExe(pathField.getText());
                if (exeFile == null) {
                    JOptionPane.showMessageDialog(null, "Can't find cef_server in selected folder.");
                    return;
                }

                int port = Integer.parseInt(portField.getText());
                String logPath = logPathField.getText();
                int logLevel = NativeServerManager.ServerLogLevel.LEVEL_INFO;
                try {
                    Integer.parseInt(logLevelField.getText());
                } catch (NumberFormatException ex) {
                    logLevel = NativeServerManager.ServerLogLevel.str2native(logLevelField.getText());
                }

                int finalLogLevel = logLevel;
                new Thread(()-> {
                    boolean success = startCefServerAndWait(exeFile.getAbsolutePath(), port, logPath, finalLogLevel);
                    if (!success)
                        JOptionPane.showMessageDialog(null, "Failed to start cef_server.");
                });
            });

            // TODO: add ability to set some important params
            // TODO: add possibility to run cef_server without --params (with good default params)

            JPanel argsPanel = new JPanel(new BorderLayout());
            argsPanel.add(gbLogging, BorderLayout.NORTH);
            argsPanel.add(gbPort, BorderLayout.SOUTH);
            argsPanel.add(runButton, BorderLayout.CENTER);

            // 3. Populate self.
            setLayout(new BorderLayout());
            add(pathPanel, BorderLayout.NORTH);
            add(argsPanel, BorderLayout.CENTER);
        }
    }

    private static String getServerInfo(int port) {
        RpcExecutor result = connect(port);
        if (result == null)
            return null;

        String state = result.execObj(s -> s.getServerInfo("state_with_details"));
        CefLog.Debug("Server state: %s", state);
        result.closeTransport();
        return state;
    }

    private static class ServerControls extends JFrame {
        ServerControls(int port) {
            setTitle("Server " + port);
            setLayout(new BorderLayout());

            // 1. State of the server (with detailed info).
            Component stateComponent;
            String serverState = getServerInfo(port);
            if (serverState == null) {
                stateComponent = new JLabel("Can't connect to server...");
            } else {
                final int[] delayMs = new int[]{5000};
                JPanel updatePanel = new JPanel(new BorderLayout());
                JLabel updateDelayLabel = new JLabel("update delay ms");
                JTextField updateDelayField = new JTextField(String.format("%d", delayMs[0]), 10);
                updateDelayField.addActionListener(e -> {
                    delayMs[0] = Integer.parseInt(updateDelayField.getText());
                });
                updatePanel.add(updateDelayLabel, BorderLayout.WEST);
                updatePanel.add(updateDelayField, BorderLayout.EAST);

                JTextArea textArea = new JTextArea(serverState);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setBorder(BorderFactory.createTitledBorder("State of the server"));

                Thread updater = new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(delayMs[0]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String newState = getServerInfo(port);
                        if (newState == null)
                            newState = "Can't connect to server...";

                        String finalNewState = newState;
                        SwingUtilities.invokeLater(()-> textArea.setText(finalNewState));
                    }
                });

                updater.start();
                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        updater.interrupt();
                    }
                });

                JPanel statePanel = new JPanel(new BorderLayout());
                statePanel.add(updatePanel, BorderLayout.NORTH);
                statePanel.add(scrollPane, BorderLayout.CENTER);

                stateComponent = statePanel;
            }
            add(stateComponent, BorderLayout.CENTER);

            // 2. Panel for operations with the running server
            JCheckBox cbxMaster = new JCheckBox("Connect as master");

            JButton runSimple = new JButton("Run simple frame");
            JButton runDetailed = new JButton("Run detailed frame");

            JButton crash = new JButton("Crash");
            JButton stop = new JButton("Stop");
            JButton openLog = new JButton("Open log");

            JPanel optionsPanel = creatGroupBox("Server operations", BoxLayout.Y_AXIS);
            optionsPanel.add(cbxMaster);
            optionsPanel.add(runSimple);
            optionsPanel.add(runDetailed);
            optionsPanel.add(crash);
            optionsPanel.add(stop);
            optionsPanel.add(openLog);

            add(optionsPanel, BorderLayout.WEST);

            pack();
        }
    }

    private static List<Path> findFile(Path dir, String fname) {
        List<Path> found = new ArrayList<>();
        try (Stream<Path> walkStream = Files.walk(dir)) {
            walkStream.filter(p -> p.toFile().isFile()).forEach(f -> {
                if (f.toString().endsWith(getExeName()))
                    found.add(f);
            });
        } catch (IOException e) {
            CefLog.Debug("findFile: failed to walk path '%s', exception: ", dir, e.getMessage());
        }
        return found;
    }

    private static File findCefServerExe(String pathString) {
        File fpath = new File(pathString);
        if (!fpath.exists()) {
            CefLog.Debug("findCefServerExe: path '%s' doesn't exist.", pathString);
            return null;
        }
        if (fpath.isFile())
            return fpath;

        Path path = Paths.get(pathString);

        // 1. Find cef_server.exe in this folder or any subfolder
        List<Path> found = findFile(path, getExeName());

        if (found.size() == 0) {
            // 2. In OSX try to find cef_server in the Contents/Frameworks/cef_server.app folder
            if (OS.isMacintosh()) {
                if (path.endsWith("Home"))
                    found = findFile(path.getParent().resolve("Frameworks/cef_server.app"), getExeName());
            }
        }

        if (found.size() == 1)
            return found.get(0).toFile();

        // TODO: select proper file
        if (found.size() > 1)
            return found.get(0).toFile();

        CefLog.Debug("findCefServerExe: can't find cef_server in path '%s'", pathString);
        return null;
    }
}
