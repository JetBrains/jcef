package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift.transport.*;
import org.cef.OS;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ThriftTransport {
    static final boolean MANUAL_SERVER_SELECT = Utils.getBoolean("JCEF_MANUAL_SERVER_SELECT") || Utils.getBoolean("jcef.select.server");
    private static final boolean IS_TCP_USED;
    private static final int PORT_CEF_SERVER;
    private static final int PORT_JAVA_HANDLERS;
    private static final String PIPENAME_JAVA_HANDLERS;
    private static final String PIPENAME_CEF_SERVER;
    private static final long PID = ProcessHandle.current().pid();
    private static final String SUFFIX;
    private static final Path PIPE_DIR = Path.of(System.getProperty("java.io.tmpdir"));
    private static final int FREE_PORT_SEARCH_START = Utils.getInteger("CEF_SERVER_FREE_PORT_SEARCH_START", 30000);
    private static final int FREE_PORT_SEARCH_END = Utils.getInteger("CEF_SERVER_FREE_PORT_SEARCH_START", 65000);

    private final String myPipe;
    private final int myPort;

    public static final ThriftTransport ourDefaultServer;
    public static final ThriftTransport ourDefaultClient;

    static {
        IS_TCP_USED = !Utils.getBoolean("CEF_SERVER_USE_PIPE");

        if (!Utils.getBoolean("DONT_JCEF_USE_UNIQUE_NAMES")) {
            final SimpleDateFormat f = new SimpleDateFormat("hh_mm_ss_SSS");
            SUFFIX = "_" + PID + "_" + f.format(new Date());
        } else
            SUFFIX = "_" + PID;

        if (IS_TCP_USED) {
            final int[] customPort = new int[]{Utils.getInteger("ALT_CEF_SERVER_PORT", -1)};
            if (MANUAL_SERVER_SELECT) {
                JTextField textField = new JTextField("", 10);
                JLabel label = new JLabel("Enter port");
                JPanel portComponent = new JPanel(new BorderLayout());
                portComponent.add(label,BorderLayout.WEST);
                portComponent.add(textField,BorderLayout.EAST);

                JPanel panel = new JPanel(new BorderLayout());
                panel.add(portComponent, BorderLayout.SOUTH);

                List<ProcessLister.RunningServerInfo> runningPorts = ProcessLister.listRunningInstancesPorts();
                if (runningPorts != null && !runningPorts.isEmpty()) {
                    // Fill list
                    String[] runningList = new String[runningPorts.size()];
                    int c = 0;
                    for (ProcessLister.RunningServerInfo s : runningPorts)
                        runningList[c++] = s.transport.myPort + ", parent: " + s.getParentProcessInfo();
                    JList<String> list = new JList<>(runningList);
                    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    list.setFont(list.getFont().deriveFont(13f));
                    list.setVisibleRowCount(8);

                    list.addListSelectionListener(new ListSelectionListener() {
                        @Override
                        public void valueChanged(ListSelectionEvent e) {
                            final String selected = list.getSelectedValue().trim();
                            customPort[0] = Integer.parseInt(selected.substring(0, selected.indexOf(',')));
                            textField.setText(list.getSelectedValue());
                            CefLog.Debug("MANUAL_SERVER_SELECT: selected port %d", customPort[0]);
                        }
                    });

                    JScrollPane scrollPane = new JScrollPane(list);
                    scrollPane.setPreferredSize(new Dimension(400, 250));

                    panel.add(scrollPane, BorderLayout.CENTER);
                }

                JOptionPane.showMessageDialog(
                        null,
                        panel,
                        "Select running cef_server",
                        JOptionPane.PLAIN_MESSAGE
                );

                try {
                    customPort[0] = Integer.parseInt(textField.getText());
                } catch (NumberFormatException e) {}
            }
            if (customPort[0] == -1) {
                PORT_CEF_SERVER = findFreePort(null);
                if (PORT_CEF_SERVER == -1)
                    CefLog.Error("Can't find free tcp-port for server.");
                else
                    CefLog.Info("Found free tcp-port %d for server.", PORT_CEF_SERVER);
            } else {
                CefLog.Info("Use custom tcp-port %d for server.", customPort[0]);
                PORT_CEF_SERVER = customPort[0];
            }

            customPort[0] = Utils.getInteger("ALT_JAVA_HANDLERS_PORT", -1);
            if (customPort[0] == -1) {
                Set<Integer> exclude = new HashSet<>(); exclude.add(PORT_CEF_SERVER);
                PORT_JAVA_HANDLERS = findFreePort(exclude);
                if (PORT_JAVA_HANDLERS == -1)
                    CefLog.Error("Can't find free tcp-port for java-handlers.");
                else
                    CefLog.Info("Found free tcp-port %d for java-handlers.", PORT_JAVA_HANDLERS);
            } else {
                CefLog.Info("Use custom tcp-port %d for java-handlers.", customPort[0]);
                PORT_JAVA_HANDLERS = customPort[0];
            }

            ourDefaultServer = new ThriftTransport(getServerPort());
            ourDefaultClient = new ThriftTransport(getJavaHandlersPort());

            PIPENAME_JAVA_HANDLERS = "";
            PIPENAME_CEF_SERVER = "";
        } else {
            PORT_CEF_SERVER = 0;
            PORT_JAVA_HANDLERS = 0;

            final String pipeServerDefault = "cef_server_pipe";
            final String pipeServerCustom = Utils.getString("ALT_CEF_SERVER_PIPE");
            final String suffixServer;
            if (pipeServerCustom == null || pipeServerCustom.isEmpty()) {
                PIPENAME_CEF_SERVER = pipeServerDefault;
                suffixServer = SUFFIX;
            } else {
                PIPENAME_CEF_SERVER = pipeServerCustom;
                suffixServer = "";
            }

            String pipeJavaDefault = "client_pipe";
            String pipeJavaCustom = Utils.getString("ALT_JAVA_HANDLERS_PIPE");
            final String suffixJava;
            if (pipeJavaCustom == null || pipeJavaCustom.isEmpty()) {
                PIPENAME_JAVA_HANDLERS = pipeJavaDefault;
                suffixJava = SUFFIX;
            } else {
                PIPENAME_JAVA_HANDLERS = pipeJavaCustom;
                suffixJava = "";
            }

            String pipe;
            if (OS.isWindows())
                pipe = PIPENAME_CEF_SERVER + suffixServer;
            else {
                pipe = PIPE_DIR.resolve(PIPENAME_CEF_SERVER + suffixServer).toString();
                pipe = normalizePipeName(pipe);
            }
            ourDefaultServer = new ThriftTransport(pipe);

            if (OS.isWindows())
                pipe = PIPENAME_JAVA_HANDLERS + suffixJava;
            else {
                pipe = PIPE_DIR.resolve(PIPENAME_JAVA_HANDLERS + suffixJava).toString();
                pipe = normalizePipeName(pipe);
            }
            ourDefaultClient = new ThriftTransport(pipe);
        }
    }

    public ThriftTransport(File pipe) {
        this.myPipe = OS.isWindows() ? pipe.getName() : pipe.getAbsolutePath() ;
        this.myPort = 0;
    }

    public ThriftTransport(String pipe) {
        this.myPipe = pipe;
        this.myPort = 0;
    }

    public ThriftTransport(int port) {
        this.myPipe = null;
        this.myPort = port;
    }

    public boolean isTcp() { return myPipe == null; }

    public String getPipe() { return myPipe; }
    public int getPort() { return myPort; }

    @Override
    public String toString() {
        return myPipe != null ? String.format("pipe='%s'", myPipe) : String.format("port=%d", myPort);
    }

    public String toStringShort() {
        return myPipe != null ? String.format("pipe_%s", myPipe).trim().replace(" ","") : String.format("port_%d", myPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ThriftTransport) {
            ThriftTransport other = (ThriftTransport)obj;
            return myPipe != null ? myPipe.equals(other.myPipe) : myPort == other.myPort;
        }
        return false;
    }

    public static String getJavaHandlersPipe(String suffix) {
        if (OS.isWindows())
            return PIPENAME_JAVA_HANDLERS + "_" + suffix;
        return PIPE_DIR.resolve(PIPENAME_JAVA_HANDLERS + "_" + suffix).toString();
    }

    public static String getServerPipe(String suffix) {
        if (OS.isWindows())
            return PIPENAME_CEF_SERVER + "_" + suffix;
        return PIPE_DIR.resolve(PIPENAME_CEF_SERVER + "_" + suffix).toString();
    }

    public static boolean isTcpUsed() { return IS_TCP_USED; }

    public static String getUniqueSuffix() { return SUFFIX; }

    private static int getServerPort() { return PORT_CEF_SERVER; }

    private static int getJavaHandlersPort() {
        return PORT_JAVA_HANDLERS;
    }

    public static int findFreePort() { return findFreePort(null); }

    public static int findFreePort(Set<Integer> exclude) { return findFreePort(FREE_PORT_SEARCH_START, FREE_PORT_SEARCH_END, exclude); }

    public static int findFreePort(int from, int to, Set<Integer> exclude) {
        for (int port = from; port < to; ++port) {
            if (exclude != null && exclude.contains(port))
                continue;
            if (isPortFree(port))
                return port;
        }
        return -1;
    }

    public static boolean isPortFree(int port) {
        try {
            ServerSocket ss = new ServerSocket(port, 0, InetAddress.getByName(null));
            ss.close();
            return true;
        } catch (IOException e) {}
        return false;
    }

    public TServerTransport createServerTransport() throws Exception {
        if (isTcp())
            return new TServerSocket(new InetSocketAddress(InetAddress.getByName(null), myPort));

        if (OS.isWindows()) {
            WindowsPipeServerSocket pipeSocket = new WindowsPipeServerSocket(myPipe);
            return new TServerTransport() {
                @Override
                public void listen() {}

                @Override
                public TTransport accept() throws TTransportException {
                    try {
                        Socket client = pipeSocket.accept();
                        return client != null ?
                                new TIOStreamTransport(client.getInputStream(), client.getOutputStream()) : null;
                    } catch (IOException e) {
                        CefLog.Debug("Exception occurred during pipe listening: %s", e);
                        throw new TTransportException(TTransportException.UNKNOWN, e.getMessage());
                    }
                }

                @Override
                public void close() {
                    try {
                        pipeSocket.close();
                    } catch (IOException e) {
                        CefLog.Error("Exception occurred during pipe closing: %s", e);
                    }
                }
            };
        }

        // Linux or OSX
        new File(myPipe).delete(); // cleanup file remaining from prev process

        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(UnixDomainSocketAddress.of(myPipe));

        return new TServerTransport() {
            @Override
            public void listen() {}

            @Override
            public TTransport accept() throws TTransportException {
                try {
                    SocketChannel channel = serverChannel.accept();
                    InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
                    OutputStream os = new BufferedOutputStream(Channels.newOutputStream(channel));
                    return new TIOStreamTransport(is, os);
                } catch (IOException e) {
                    CefLog.Debug("Exception occurred during pipe listening: %s", e);
                    throw new TTransportException(TTransportException.UNKNOWN, e.getMessage());
                }
            }

            @Override
            public void close() {
                try {
                    serverChannel.close();
                } catch (IOException e) {
                    CefLog.Error("Exception occurred during pipe closing: %s", e);
                }
                try {
                    new File(myPipe).delete();
                } catch (RuntimeException e) {
                    CefLog.Error("RuntimeException occurred when trying to delete file of pipe '%s', error: %s", myPipe, e);
                }
            }
        };
    }

    public TIOStreamTransport openPipeTransport() throws TTransportException {
        try {
            InputStream is;
            OutputStream os;
            final Runnable closer;
            if (OS.isWindows()) {
                WindowsPipeSocket pipe = new WindowsPipeSocket(myPipe);
                is = pipe.getInputStream();
                os = pipe.getOutputStream();
                closer = ()->{
                    try {
                        pipe.close();
                    } catch (IOException e) {}
                };
            } else {
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(myPipe);
                channel.connect(socketAddress);
                is = Channels.newInputStream(channel);
                os = Channels.newOutputStream(channel);
                closer = ()->{
                    try {
                        channel.close();
                    } catch (IOException e) {}
                };
            }

            return new TIOStreamTransport(is, os) {
                @Override
                public void close() {
                    closer.run();
                }
            };
        } catch (IOException e) {
            throw new TTransportException(e.getMessage());
        }
    }

    public static File[] findPipes() {
        if (OS.isWindows()) {
            String[] pipes = WindowsPipe.findPipes(PIPENAME_CEF_SERVER + "*");
            if (pipes == null || pipes.length == 0)
                return null;
            File[] result = new File[pipes.length];
            for (int i = 0; i < pipes.length; i++)
                result[i] = new File(pipes[i]);
            return result;
        }

        return PIPE_DIR.toFile().listFiles((dir, name) -> name.startsWith(PIPENAME_CEF_SERVER));
    }

    private static String normalizePipeName(String pipeName) {
        if (OS.isWindows())
            return pipeName;

        // https://unix.stackexchange.com/questions/367008/why-is-socket-path-length-limited-to-a-hundred-chars
        if (pipeName == null || pipeName.isEmpty())
            return null;
        if (pipeName.length() < 100)
            return pipeName;

        final SimpleDateFormat f = new SimpleDateFormat("mm_ss_SSS");
        final String newShortName = "cc_" + f.format(new Date());
        String newName = Path.of(System.getProperty("java.io.tmpdir")).resolve(newShortName).toString();
        if (newName.length() < 100)
            return newName;

        return "/var/tmp/" + newShortName;
    }
}
