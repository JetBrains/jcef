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
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ProcessLister {

    public static List<RunningServerInfo> listRunningInstancesPorts() {
        final ArrayList<RunningServerInfo> result = new ArrayList<>();

        if (OS.isLinux() || OS.isMacintosh()) {
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

                        final int pid = parseIntSafe(line.substring(posSp0End, posSp1), -1);
                        final int ppid = parseIntSafe(line.substring(posSp1End, posSp2), -1);

                        final String cmdPrefix = OS.isMacintosh() ? "MacOS/cef_server" : "bin/cef_server";
                        final int prefixPos = line.indexOf(cmdPrefix);
                        final String cmdLine = prefixPos >= 0 ? line.substring(prefixPos + cmdPrefix.length()) : "";

                        final int pos0 = line.indexOf("--port=");
                        if (pos0 < 0) {
                            if (OS.isMacintosh() && line.contains("cef_server Helper"))
                                continue;
                            if (OS.isLinux() && line.contains("--type="))
                                continue;
                            defaultArgsServer = new RunningServerInfo(new ThriftTransport(9999), pid, ppid, cmdLine);
                        } else {
                            final int pos1 = line.indexOf(" ", pos0 + 7);
                            String sport = line.substring(pos0 + 7, pos1);
                            try {
                                result.add(new RunningServerInfo(new ThriftTransport(Integer.parseInt(sport)), pid, ppid, cmdLine));
                            } catch (NumberFormatException e) {
                                CefLog.Error("Can't parse port number: " + sport);
                            }
                        }
                    }
                }

                process.waitFor();
            } catch (IOException | InterruptedException e) {
                CefLog.Error("Failed to execute command: " + e.getMessage());
                e.printStackTrace();
            }

            if (defaultArgsServer != null)
                result.add(defaultArgsServer);

            return result;
        }

        // Windows
        Pattern p = Pattern.compile("--type=[^ ]+");
        List<WindowsProcessInfo> processes = null;
        try {
            processes = listWindowsProcesses(".*cef_server.exe", s -> !p.matcher(s).find());
            for (WindowsProcessInfo pi : processes) {
                final int pos0 = pi.commandLine.indexOf("--port=");
                if (pos0 >= 0) {
                    final int pos1 = pi.commandLine.indexOf(" ", pos0 + 7);
                    String sport = pi.commandLine.substring(pos0 + 7, pos1);
                    try {
                        result.add(new RunningServerInfo(new ThriftTransport(Integer.parseInt(sport)), pi.pid, pi.parentPid == null ? -1 : pi.parentPid, pi.commandLine, pi.parentName, pi.parentCommandLine));
                    } catch (NumberFormatException e) {
                        CefLog.Error("Can't parse port number: " + sport);
                    }
                } else {
                    CefLog.Debug("Found cef_server instance without --port parameter.");
                    result.add(new RunningServerInfo(new ThriftTransport(9999), pi.pid, -1, pi.commandLine, pi.parentName, pi.parentCommandLine));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
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
                    running.add(new RunningServerInfo(new ThriftTransport(pipe), -1, -1, ""));
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

    static class WindowsProcessInfo {
        public final int pid;
        public final String name;
        public final String commandLine;
        public final Integer parentPid;
        public final String parentName;
        public final String parentCommandLine;

        public WindowsProcessInfo(int pid, String name, String commandLine,
                                  Integer parentPid, String parentName, String parentCommandLine) {
            this.pid = pid;
            this.name = name;
            this.commandLine = commandLine;
            this.parentPid = parentPid;
            this.parentName = parentName;
            this.parentCommandLine = parentCommandLine;
        }

        @Override
        public String toString() {
            return String.format(
                    "[%d] %s — %s | parent[%s]: %s — %s",
                    pid, name, commandLine,
                    parentPid != null ? parentPid.toString() : "–",
                    parentName != null ? parentName : "–",
                    parentCommandLine != null ? parentCommandLine : "–"
            );
        }
    }

    private static List<WindowsProcessInfo> listWindowsProcesses(String regexNameFilter, Predicate<String> cmdFilter) throws IOException, InterruptedException {
        List<WindowsProcessInfo> result = new ArrayList<>();
        Pattern patternName = regexNameFilter == null || regexNameFilter.isEmpty() ? null : Pattern.compile(regexNameFilter);

        // Optimized PowerShell: fetch all once, join in memory, output CSV
        String psScript =
                "$procs = @{}; " +
                        "Get-CimInstance Win32_Process | ForEach-Object { $procs[$_.ProcessId] = $_ }; " +
                        "$output = foreach ($p in $procs.Values) { " +
                        "  $parent = $null; " +
                        "  if ($p.ParentProcessId -and $procs.ContainsKey($p.ParentProcessId)) { " +
                        "    $parent = $procs[$p.ParentProcessId] " +
                        "  } " +
                        "  [PSCustomObject]@{ " +
                        "    PID=$p.ProcessId; " +
                        "    Name=$p.Name; " +
                        "    CmdLine=$p.CommandLine; " +
                        "    ParentPID=($p.ParentProcessId -as [string]); " +
                        "    ParentName=($parent.Name -as [string]); " +
                        "    ParentCmdLine=($parent.CommandLine -as [string]) " +
                        "  } " +
                        "}; " +
                        "$output | ConvertTo-Csv -NoTypeInformation";

        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-Command", psScript
        );
        pb.redirectErrorStream(true); // merge stderr into stdout for easier handling

        Process process = pb.start();
        // NOTE: don't call process.waitFor() before reading stdout of the process (otherwise deadlock occurred).
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean firstLine = true; // Skip CSV header
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // CSV: "1234","java.exe","\"C:\\...\\java.exe\" -Xmx512m ..."
                line = line.trim();
                if (line.isEmpty() || line.equals("\"\"")) continue;

                // Expect: PID, Name, CmdLine, ParentPID, ParentName, ParentCmdLine
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 6) continue;

                final String name = unquote(fields.get(1));
                if (patternName != null && !patternName.matcher(name).find())
                    continue;

                final String cmdLine = unquote(fields.get(2));
                if (cmdFilter != null && !cmdFilter.test(cmdLine))
                    continue;

                final int pid = parseIntSafe(fields.get(0), -1);
                Integer parentPid = parseIntSafe(fields.get(3), null);

                    result.add(new WindowsProcessInfo(pid, name, cmdLine, parentPid,
                        unquote(fields.get(4)),
                        unquote(fields.get(5))));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            CefLog.Error("PowerShell command failed with exit code " + exitCode);
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                err.lines().forEach(CefLog::Error);
            }
        }

        return result;
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.replaceAll("^\"|\"$", "").replace("\"\"", "\"");
        return s;
    }

    private static Integer parseIntSafe(String s, Integer def) {
        s = unquote(s).trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return def;
        try { return Integer.valueOf(s); } catch (NumberFormatException e) { return def; }
    }

    // Simple CSV parser (handles quoted fields, escaped quotes)
    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++; // skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field.setLength(0); // clear
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());
        return result;
    }

    public static void main(String[] args) {
        try {
            Pattern p = Pattern.compile("--type=[^ ]+");
            List<WindowsProcessInfo> processes = listWindowsProcesses(".*cef_server.exe", s -> !p.matcher(s).find());
            for (WindowsProcessInfo processInfo : processes)
                System.out.println(processInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class RunningServerInfo {
        public final ThriftTransport transport;
        public final int pid;
        public final int ppid; // parent process id
        public final String commandLine;
        private String parentName;
        private String parentCommandLine;

        public RunningServerInfo(ThriftTransport transport, int pid, int ppid, String commandLine) {
            this.transport = transport;
            this.pid = pid;
            this.ppid = ppid;
            this.commandLine = commandLine;
        }

        public RunningServerInfo(ThriftTransport transport, int pid, int ppid, String commandLine, String parentName, String parentCommandLine) {
            this.transport = transport;
            this.pid = pid;
            this.ppid = ppid;
            this.commandLine = commandLine;
            this.parentName = parentName;
            this.parentCommandLine = parentCommandLine;
        }

        private static Map<Integer, String> ourPpid2Cmd = new ConcurrentHashMap<>();

        public String getParentProcessInfo() {
            if (parentName != null) return parentName + " (" + parentCommandLine + ")";
            if (parentCommandLine != null) return parentCommandLine;

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
                CefLog.Error("Failed to execute command: " + e.getMessage());
            }
            return null;
        }
    }
}
