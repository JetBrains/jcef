package tests;

import com.jetbrains.cef.SharedMemory;
import com.jetbrains.cef.remote.*;
import com.jetbrains.cef.remote.thrift.transport.TTransportException;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.handler.CefAppHandler;
import org.cef.misc.CefLog;
import tests.detailed.MainFrame;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class ServersManagerGUI {
    JFrame frame;
    DefaultListModel<ProcessLister.RunningServerInfo> listModel;
    JList<ProcessLister.RunningServerInfo> runningList;
    static TextArea logArea = new TextArea();

    // Remembered settings
    static Path ourSettingsPath;
    static Properties ourProperties;
    static final List<String> ourLastExePaths = new ArrayList<>();
    static final int maxPathsCount = 10;

    private static void log(String msg, Object... args) {
        logArea.append(String.format(msg, args) + "\n");
    }

    private static void loadProperties() {
        ourProperties = new Properties();
        try (InputStream inputStream = new FileInputStream(ourSettingsPath.toString())) {
            ourProperties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            for (int c = 0; c < maxPathsCount; c++) {
                String lastPath = ourProperties.getProperty("lastExePath." + c);
                if (lastPath != null)
                    ourLastExePaths.add(lastPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveProperties(String comment) {
        try (OutputStream outputStream = new FileOutputStream(ourSettingsPath.toString())) {
            try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                for (int c = 0; c < ourLastExePaths.size(); c++)
                    ourProperties.setProperty("lastExePath." + c, ourLastExePaths.get(c));
                ourProperties.store(writer, comment);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Start GUI for NativeServerManager.");
        System.setProperty("jcef.remote.force_enabled", "true");

        ourSettingsPath = Path.of(System.getProperty("java.io.tmpdir")).resolve("ServersManagerGUI.txt");
        loadProperties();

        String lastSharedMemHelperPath = ourProperties.getProperty("lastSharedMemHelperPath");
        if (lastSharedMemHelperPath != null) {
            File fpath = new File(lastSharedMemHelperPath);
            if (fpath.exists() && !fpath.isDirectory()) {
                log("Try last shared mem helper path: " + lastSharedMemHelperPath + "\n");
                SharedMemory.loadDynamicLib(lastSharedMemHelperPath);
                if (!SharedMemory.isIsLoaded())
                    log("ERROR: Can't load shared mem helper library.\n");
            }
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new ServersManagerGUI().initUI();
        });
    }

    private void updateRunningServersList() {
        SwingUtilities.invokeLater(()->{
            listModel.clear();
            ProcessLister.listRunningInstancesPorts().forEach(s -> listModel.addElement(s));
        });
    }

    private void initUI() {
        frame = new JFrame("CEF servers manager");
        frame.setLayout(new BorderLayout());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CefApp cefApp = CefApp.getInstanceIfAny();
                if (cefApp != null)
                    cefApp.dispose();
                frame.dispose();
                System.exit(0);
            }
        });

        // Create model and populate with port numbers
        listModel = new DefaultListModel<>();
        Thread updater = new Thread(()->{
            while (true) {
                updateRunningServersList();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        updater.start();

        // Create JList with model
        runningList = new JList<>(listModel);
        runningList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        runningList.setVisibleRowCount(8);
        runningList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        // Optional: customize cell appearance
        runningList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                ProcessLister.RunningServerInfo si = (ProcessLister.RunningServerInfo)value;
                label.setText(String.format("port %2d (parent: %s)", si.transport.getPort(), si.getParentProcessCmd())); // right-align 2-digit numbers
                return label;
            }
        });

        runningList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = runningList.locationToIndex(e.getPoint());
                    if (index >= 0 && index < listModel.size())
                        runningList.setSelectedIndex(index);
                } else if (e.getClickCount() == 2) {
                    int index = runningList.locationToIndex(e.getPoint());
                    if (index >= 0 && index < listModel.size()) {
                        ProcessLister.RunningServerInfo value = listModel.get(index);
                        new ServerControls(value);
                    }
                }
            }
        });

        runningList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !runningList.isSelectionEmpty()) {
                    ProcessLister.RunningServerInfo value = runningList.getSelectedValue();
                    new ServerControls(value);
                }
            }
        });

        // 1. List of running servers
        JScrollPane scrollPane = new JScrollPane(runningList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Running cef_server instances"));
        frame.add(scrollPane, BorderLayout.NORTH);

        // 2. Panel for logger
        frame.add(logArea, BorderLayout.CENTER);

        // 3. Panel for starting cef_server instances
        ServerStartOptionsPanel pathSelectionPanel = new ServerStartOptionsPanel();
        frame.add(pathSelectionPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null); // center on screen
    }

    // NOTE: user must manually call closeTransport() after usage.
    static RpcExecutor connect(int port) {
        ThriftTransport serverPort = new ThriftTransport(port);
        try {
            RpcExecutor result = new RpcExecutor().openTransport(serverPort);
            return result;
        } catch (TTransportException e) {
            log("Exception when trying to connect server, err: %s", e.getMessage());
        }
        return null;
    }

    private static boolean startCefServerAndWait(String exePath, int port, String logPath, int logLevel, String[] args, CefSettings settings, CefAppHandler appHandler, Map<String, String> env) {
        ThriftTransport thriftTransport = new ThriftTransport(port);
        String runningRoot = NativeServerManager.isRunning(thriftTransport);
        if (runningRoot != null) {
            log("cef_server instance is already running on port %d, root=%s", port, runningRoot);
            return false;
        }

        File exeFile = new File(exePath);
        if (!exeFile.exists() || !exeFile.isFile()) {
            log("File '%s' doesn't exist (or it's directory).", exePath);
            return false;
        }

        final boolean success = ServerStarter.startProcessAndWait(exeFile, thriftTransport, appHandler, args, settings, logPath, NativeServerManager.ServerLogLevel.nativeDesc(logLevel), false, env, 20000);
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

    private static class ServerStartOptionsPanel extends JPanel {
        public ServerStartOptionsPanel() {
            // 1. Path selection panel
            ArrayList<String> items = new ArrayList<>();
            final String currenRuntimePath = getCurrentRuntimePath();
            final File currenRuntimeExeFile = findCefServerExe(currenRuntimePath);
            if (currenRuntimeExeFile != null)
                items.add(currenRuntimeExeFile.getAbsolutePath());

            final Path currenRuntimeExePath = currenRuntimeExeFile != null ? currenRuntimeExeFile.toPath() : null;
            for (String p: ourLastExePaths) {
                if (currenRuntimeExePath != null && currenRuntimeExePath.equals(p))
                    continue;
                final File exeFile = findCefServerExe(p);
                if (exeFile != null)
                    items.add(exeFile.getAbsolutePath());
            }
            JComboBox<String> pathComboBox = new JComboBox<>(items.toArray(new String[0]));
            pathComboBox.setEditable(true);
            JTextField textField = (JTextField) pathComboBox.getEditor().getEditorComponent();
            textField.setColumns(128); // Adjust width

            JButton pathSelect = new JButton("Select folder");
            pathSelect.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.showOpenDialog(null);
                String path = chooser.getSelectedFile().getAbsolutePath();
                final File exeFile = findCefServerExe(path);
                if (exeFile == null)
                    JOptionPane.showMessageDialog(null, "Can't find cef_server executable in path: " + path);
                else {
                    pathComboBox.insertItemAt(exeFile.getAbsolutePath(), 0);
                    pathComboBox.setSelectedIndex(0);
                    ourLastExePaths.add(exeFile.getAbsolutePath());
                    saveProperties("");
                }
            });

            JPanel pathPanel = new JPanel(new BorderLayout());
            pathPanel.add(pathComboBox, BorderLayout.CENTER);
            pathPanel.add(pathSelect, BorderLayout.EAST);

            // 2. Arguments and params panel
            // log level and path
            JTextField logLevelField = new JTextField("trace", 10);
            logLevelField.setSize(new Dimension(100, logLevelField.getSize().height));
            JTextField logPathField = new JTextField(Path.of(System.getProperty("user.home")).resolve("cef_server_log.txt").toAbsolutePath().toString(), 128);
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

            JPanel gbArgs = creatGroupBox("Cmd line switches", BoxLayout.X_AXIS);
            JTextField argsField = new JTextField();
            gbArgs.add(argsField);

            JPanel gbCefSettings = creatGroupBox("CefSettings", BoxLayout.X_AXIS);
            JTextField rootField = new JTextField();
            gbCefSettings.add(addLabelForComponent(rootField, "Root"));

            JPanel gbEnv = creatGroupBox("Env vars separated with ';'", BoxLayout.X_AXIS);
            JTextField envField = new JTextField();
            gbEnv.add(envField);

            // run button
            JButton runButton = new JButton("Run");
            runButton.addActionListener(e -> {
                File exeFile = findCefServerExe((String) pathComboBox.getSelectedItem());
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
                    CefSettings settings = new CefSettings();
                    settings.cache_path = rootField.getText();
                    String[] args = null;
                    final String argsString = argsField.getText();
                    if (argsString != null && !argsString.isEmpty()) {
                        args = argsString.split(" ");
                        for (int ci = 0; ci < args.length; ci++)
                            args[ci] = args[ci].replace("_SPACESYMBOL_", " ");
                    }
                    Map<String, String> envs = null;
                    final String envsString = envField.getText();
                    if (envsString != null && !envsString.isEmpty()) {
                        envs = new HashMap<>();
                        for (String env: envsString.split(";")) {
                            String[] kv = env.split("=");
                            if (kv.length == 2)
                                envs.put(kv[0].trim(), kv[1].trim());
                            else
                                log("WARNING: invalid env var syntax: '%s'", env);
                        }
                    }

                    CefAppHandler appHandler = null; // TODO: support custom schemes later

                    log("Start cef_server: path=%s, port=%d, logLevel=%s, root=%s, args=%s", exeFile.getAbsolutePath(), port, NativeServerManager.ServerLogLevel.nativeDesc(finalLogLevel), settings.cache_path, Arrays.toString(args));
                    boolean success = startCefServerAndWait(exeFile.getAbsolutePath(), port, logPath, finalLogLevel, args, settings, appHandler, envs);
                    if (!success) {
                        log("ERROR: failed to start.");
                        JOptionPane.showMessageDialog(null, "Failed to start cef_server.");
                    }
                }).start();
            });

            JPanel argsPanel = new JPanel(new BorderLayout());
            argsPanel.add(gbLogging, BorderLayout.NORTH);
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.add(gbPort);
            p.add(gbCefSettings);
            p.add(gbArgs);
            p.add(gbEnv);
            argsPanel.add(p, BorderLayout.CENTER);
            argsPanel.add(runButton, BorderLayout.SOUTH);

            // 3. Populate self.
            setLayout(new BorderLayout());
            add(pathPanel, BorderLayout.NORTH);
            add(argsPanel, BorderLayout.CENTER);
        }
    }

    private static void execServerRpc(int port, RpcExecutor.Rpc r) {
        RpcExecutor rpcExecutor = connect(port);
        if (rpcExecutor != null) {
            try {
                rpcExecutor.exec(r);
            } finally {
                rpcExecutor.closeTransport();
            }
        } else
            log("execServerRpc: failed to connect to server on port " + port + "\n");
    }

    private static <T> T execServerRpcObj(int port, RpcExecutor.RpcObj<T> r) {
        RpcExecutor rpcExecutor = connect(port);
        if (rpcExecutor != null) {
            try {
                return rpcExecutor.execObj(r);
            } finally {
                rpcExecutor.closeTransport();
            }
        } else
            log("execServerRpcObj: failed to connect to server on port " + port + "\n");
        return null;
    }

    private static class ServerControls extends JFrame {
        private static CefApp createCefApp(int port, boolean connectAsMaster) {
            CefApp.startup(null);
            CefLog.initVerbose();
            ThriftTransport transport = new ThriftTransport(port);
            // NOTE: file, args and settings won't be used since it will be connected to an already running instance.
            return CefApp.getInstance(new CefServer(null, transport, null, null, connectAsMaster));
        }

        ServerControls(ProcessLister.RunningServerInfo serverInfo) {
            setTitle("Server " + serverInfo.transport.toString() + " | parent: " + serverInfo.getParentProcessCmd());
            setLayout(new BorderLayout());

            // 1. State of the server (with detailed info).
            Component stateComponent;
            final int port = serverInfo.transport.getPort();
            String serverState = execServerRpcObj(port, s -> s.getServerInfo("state_with_details"));
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

                final String serverVersion = execServerRpcObj(port, s -> s.getServerInfo("version"));
                final String serverRoot = execServerRpcObj(port, s -> s.getServerInfo("root"));
                final String stateTextPrefix = "Version: " + serverVersion + "\n" + "Root: " + serverRoot + "\n";
                JTextArea textArea = new JTextArea(stateTextPrefix + serverState);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setBorder(BorderFactory.createTitledBorder("State of the server"));

                boolean[] stopRequested = new boolean[]{false};
                Thread updater = new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(delayMs[0]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (stopRequested[0])
                            return;

                        String newState = execServerRpcObj(port, s -> s.getServerInfo("state_with_details"));
                        if (newState == null)
                            newState = "Can't connect to server...";

                        String finalNewState = newState;
                        SwingUtilities.invokeLater(()-> textArea.setText(stateTextPrefix + finalNewState));
                    }
                });

                updater.start();
                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        stopRequested[0] = true;
                    }
                });

                JPanel statePanel = new JPanel(new BorderLayout());
                statePanel.add(updatePanel, BorderLayout.NORTH);
                statePanel.add(scrollPane, BorderLayout.CENTER);

                stateComponent = statePanel;
            }
            add(stateComponent, BorderLayout.CENTER);

            // 2. Panel for operations with the running server
            boolean[] connectAsMaster = new boolean[]{false};
            JCheckBox cbxMaster = new JCheckBox("Connect as master", connectAsMaster[0]);
            cbxMaster.addActionListener(e -> {
                connectAsMaster[0] = cbxMaster.isSelected();
            });

            JButton runSimple = new JButton("Run simple frame");
            runSimple.addActionListener(e -> {
                new tests.simple.MainFrame(createCefApp(port, connectAsMaster[0]));
            });

            JButton runDetailed = new JButton("Run detailed frame");
            runDetailed.addActionListener(e -> {
                final MainFrame frame = new tests.detailed.MainFrame(createCefApp(port, connectAsMaster[0]));
                frame.setSize(800, 600);
                frame.setVisible(true);
            });

            JButton crash = new JButton("Crash");
            crash.addActionListener(e -> {
                execServerRpc(port, r -> r.getServerInfo("doCrash"));
            });
            JButton stop = new JButton("Stop");
            stop.addActionListener(e -> {
                execServerRpc(port, r -> r.stop());
            });
            JButton openLog = new JButton("Open log");
            openLog.addActionListener(e -> {
                String logInfo = execServerRpcObj(port, s -> s.getServerInfo("logger_details"));
                if (logInfo == null)
                    return;

                // format: "level=%d,file=%s"
                final int pos0 = logInfo.indexOf(",");
                if (pos0 < 0) {
                    log("openLog: invalid logInfo: " + logInfo + "\n");
                    return;
                }
                String slevel = logInfo.substring(6, pos0).trim();
                String path = logInfo.substring(pos0 + 6).trim();

                int level = Integer.parseInt(slevel);
                if (level >= 100) {
                    JOptionPane.showMessageDialog(null, "This instance of cef_server has disabled logger.");
                    return;
                }

                if (path.equals("stdout") || path.equals("stderr") || path.equals("null") || path.isEmpty()) {
                    // 1. If we use stdout/stderr logger then show dialog with message "Look at console"
                    JOptionPane.showMessageDialog(null, "This instance of cef_server uses stderr logger (probably it is visible at some console).");
                } else {
                    // 2. Open log file with default editor
                    String textViewer = ourProperties.getProperty("textViewer");
                    if (textViewer == null || textViewer.isEmpty()) {
                        textViewer = (String) JOptionPane.showInputDialog(
                                null,                              // parent component (null = center on screen)
                                "Enter text editor command:",
                                "Set text editor",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                null,
                                "sublime"
                        );
                        ourProperties.setProperty("textViewer", textViewer);
                    }

                    if (textViewer == null || textViewer.isEmpty()) {
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.open(new File(path));
                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(null, "Failed to open log file: " + path);
                        }
                    } else {
                        ProcessBuilder pb = new ProcessBuilder(textViewer, path);
                        try {
                            pb.start();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            ourProperties.remove("textViewer");
                        }
                    }
                }
            });

            JPanel optionsPanel = creatGroupBox("Server operations", BoxLayout.Y_AXIS);
            optionsPanel.add(cbxMaster);
            if (!SharedMemory.isIsLoaded()) {
                JButton selectShMem = new JButton("SharedMemHelper path");
                selectShMem.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    chooser.showOpenDialog(null);
                    String path = chooser.getSelectedFile().getAbsolutePath();
                    SharedMemory.loadDynamicLib(path);
                    if (SharedMemory.isIsLoaded()) {
                        ourProperties.setProperty("lastSharedMemHelperPath", path);
                        saveProperties("");
                    } else
                        JOptionPane.showMessageDialog(null, "Failed to load SharedMemHelper.");
                });
                optionsPanel.add(selectShMem);
            }
            optionsPanel.add(runSimple);
            optionsPanel.add(runDetailed);
            optionsPanel.add(crash);
            optionsPanel.add(stop);
            optionsPanel.add(openLog);

            add(optionsPanel, BorderLayout.WEST);

            pack();

            setSize(1000, 800);
            setVisible(true);
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
            // log("findFile: failed to walk path '%s', exception: %s", dir, e.getMessage());
        }
        return found;
    }

    private static File findCefServerExe(String pathString) {
        File fpath = new File(pathString);
        if (!fpath.exists()) {
            log("findCefServerExe: path '%s' doesn't exist.", pathString);
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

        // log("findCefServerExe: can't find cef_server in path '%s'", pathString);
        return null;
    }
}
