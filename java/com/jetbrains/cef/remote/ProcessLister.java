package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift.transport.TTransportException;
import org.cef.OS;
import org.cef.misc.CefLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: remove System.out err
public class ProcessLister {

    public static List<RunningServerInfo> listRunningInstancesPorts() {
        if (OS.isLinux() || OS.isMacintosh()) {
            final ArrayList<RunningServerInfo> result = new ArrayList<>();
            final String cmd = "ps -Af | grep -E 'cef_server .*'";
            RunningServerInfo defaultArgsServer = null;

            try {
                Process process = new ProcessBuilder("bash", "-c", cmd).redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    //   UID PID   PPID ....
                    //   501 80516 80489   0 Fri07AM ??         1:52.31 /Users/..../Contents/Frameworks/cef_server.app/Contents/MacOS/cef_server --port=6188 --logfile=/Users/.../jcef_80489.log --loglevel=5 --params=/var/folders/1k/hmmg06wx2bn4dwfq53wy6c_c0000gn/T/cef_server_params.txt
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("grep -E"))
                            continue;

                        line = line.trim();
                        final int posSp0 = line.indexOf(" ");
                        int posSp0End = posSp0 + 1;
                        while (posSp0End < line.length() && line.charAt(posSp0End) == ' ') ++posSp0End;
                        final int posSp1 = line.indexOf(" ", posSp0End);
                        int posSp1End = posSp1 + 1;
                        while (posSp1End < line.length() && line.charAt(posSp1End) == ' ') ++posSp1End;
                        final int posSp2 = line.indexOf(" ", posSp1End);
                        int posSp2End = posSp2 + 1;
                        while (posSp2End < line.length() && line.charAt(posSp2End) == ' ') ++posSp2End;

                        final int pid = Integer.parseInt(line.substring(posSp0End, posSp1));
                        final int ppid = Integer.parseInt(line.substring(posSp1End, posSp2));

                        final int pos0 = line.indexOf("--port=");
                        if (pos0 < 0) {
                            if (OS.isMacintosh() && line.contains("cef_server Helper"))
                                continue;
                            defaultArgsServer = new RunningServerInfo(new ThriftTransport(9999), pid, ppid);
                        } else {
                            final int pos1 = line.indexOf(" ", pos0 + 7);
                            String sport = line.substring(pos0 + 7, pos1);
                            try {
                                result.add(new RunningServerInfo(new ThriftTransport(Integer.parseInt(sport)), pid, ppid));
                            } catch (NumberFormatException e) {
                                System.out.println("Can't parse port number: " + sport);
                            }
                        }
                    }
                }

                process.waitFor();
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to execute command: " + e.getMessage());
                e.printStackTrace();
            }

            if (defaultArgsServer != null)
                result.add(defaultArgsServer);

            return result;
        }

        // Windows
        System.out.println("listAllRunningInstances: not implemented for Windows yet.");
        return new ArrayList<>();
    }

    public static List<RunningServerInfo> listRunningInstances() {
        final List<RunningServerInfo> running = new ArrayList<>();
        if (ThriftTransport.isTcpUsed()) {
            final List<RunningServerInfo> ports = listRunningInstancesPorts();
            if (!ports.isEmpty()) {
                CefLog.Debug("Found %d running instances (ports).", ports.size());
                for (RunningServerInfo s : ports)
                    running.add(s);
            }
        } else {
            File[] pipes = ThriftTransport.findPipes();
            if (pipes != null && pipes.length > 0) {
                CefLog.Debug("Found %d running instances (pipes).", pipes.length);
                for (File pipe : pipes)
                    running.add(new RunningServerInfo(new ThriftTransport(pipe), -1, -1));
            }
        }
        return running;
    }

    public static List<String> findRunningInstancesRoots() {
        final List<RunningServerInfo> running = listRunningInstances();
        if (running == null || running.isEmpty())
            return null;

        List<String> existingRoots = new ArrayList<>();
        for (RunningServerInfo server: running) {
            RpcExecutor exec = new RpcExecutor();
            try {
                exec.openPipeTransport(server.transport);
                String newRoot = exec.execObj(s -> s.getServerInfo("root"));
                if (newRoot != null) {
                    existingRoots.add(newRoot);
                    CefLog.Info("Found cef_server instance root_cache_path '%s' (transport=%s).", newRoot, server.transport);
                } else
                    CefLog.Debug("cef_server instance (transport=%s) returns null root", server.transport);
                exec.closeTransport();
            } catch (TTransportException e) {
                CefLog.Debug("getServerInfo (with transport '%s') failed with exception: %s", server.transport  , e.getMessage());
            }
        }

        return existingRoots.isEmpty() ? null : existingRoots;
    }

    public static class ProcessInfo {
        public final int pid;
        public final String name;
        public final String commandLine;

        public ProcessInfo(int pid, String name, String commandLine) {
            this.pid = pid;
            this.name = name;
            this.commandLine = commandLine != null ? commandLine.trim() : "";
        }

        @Override
        public String toString() {
            return String.format("[%5d] %-20s %s", pid, name, commandLine);
        }
    }

    private static List<ProcessInfo> getAllProcessesWithCommandLine() throws IOException, InterruptedException {
        List<ProcessInfo> processes = new ArrayList<>();

        // PowerShell command to get PID, Name, and CommandLine
        String psCommand = "Get-CimInstance Win32_Process | Select-Object ProcessId, Name, CommandLine | ConvertTo-Csv -NoTypeInformation";

        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand);
        pb.redirectErrorStream(true);
        //pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0)
            throw new IOException("PowerShell command failed with exit code " + exitCode);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean firstLine = true; // Skip CSV header
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Parse CSV line (handle quoted fields with commas)
                String[] fields = parseCsvLine(line);
                if (fields.length >= 3) {
                    try {
                        int pid = Integer.parseInt(stripQuotes(fields[0]));
                        String name = stripQuotes(fields[1]);
                        String cmd = stripQuotes(fields[2]);
                        processes.add(new ProcessInfo(pid, name, cmd));
                    } catch (NumberFormatException ignored) { }
                }
            }
        }

        return processes;
    }

    // Simple CSV parser (handles basic quoted strings)
    private static String[] parseCsvLine(String line) {
        // This is a simplified parser — for robustness, consider using OpenCSV
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    // 🔍 Example: Find processes containing "chrome"
    public static void main(String[] args) {
        try {
            List<ProcessInfo> processes = getAllProcessesWithCommandLine();

            System.out.println("PID     Name                 Command Line");
            System.out.println("------- -------------------- --------------------------------------------------");
            for (ProcessInfo p : processes) {
                // Optional: filter
                // if (p.name.toLowerCase().contains("chrome")) {
                System.out.println(p);
                // }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class RunningServerInfo {
        public final ThriftTransport transport;
        public final int pid;
        public final int ppid; // parent process id

        public RunningServerInfo(ThriftTransport transport, int pid, int ppid) {
            this.transport = transport;
            this.pid = pid;
            this.ppid = ppid;
        }

        private static Map<Integer, String> ourPpid2Cmd = new ConcurrentHashMap<>();

        public String getParentProcessCmd() {
            if (ourPpid2Cmd.containsKey(ppid))
                return ourPpid2Cmd.get(ppid);

            final String cmd = "ps -o command -p " + ppid;
            try {
                Process process = new ProcessBuilder("bash", "-c", cmd).redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("COMMAND"))
                            continue;
                        line = line.trim();
                        if (!line.isEmpty()) {
                            ourPpid2Cmd.put(ppid, line);
                            return line;
                        }
                    }
                }
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to execute command: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
}
