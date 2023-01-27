import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReorder {
    public static void main(String[] args) throws IOException {
        if (args.length < 1)
            return;
        File f = new File(args[0]);
        if (f.isDirectory()) {
            System.out.println("Process all log files in directory " + f);
            File[] all = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(".log");
                }
            });
            for (File log: all)
                sortRecords(log, log.getPath() + "_sorted.txt");
        } else {
            sortRecords(f, args[1] + "_sorted.txt");
        }
    }
    private static void sortRecords(File input, String outputPath) throws IOException {
        if (!input.exists()) {
            System.out.println("File " + input + " doesn't exist");
            return;
        }
        System.out.println("Process log file " + input);

        // JCEF example: JCEF_V(36:28:088 | AWT-EventQueue-0): CefBrowser_N: org.cef.browser.CefBrowserWr@3bc31457: started native creation
        Pattern patternJcef = Pattern.compile("JCEF_.\\(");

        // Chromium example: [0124/083631.538892:WARNING:gpu_process_host.cc(988)] Reinitialized the GPU process after a crash. The reported initialization time was 47 ms
        //Pattern patternChromium = Pattern.compile("\\[\\d\\d\\d\\d/\\d\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d\\d\\d\\d");
        Pattern patternChromium = Pattern.compile("\\[\\d\\d\\d\\d\\/\\d\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d\\d\\d");

        int totalLines = 0;
        Map<Long, String> time2line = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            String line;
            while ((line = br.readLine()) != null) {
                totalLines++;
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // check whether current line is from JCEF log
                Matcher m = patternJcef.matcher(line);
                if (m.find()) {
                    String stime = line.substring(m.end(), m.end() + 9); // 37:01:601
                    String sm = stime.substring(0, 2);
                    String ss = stime.substring(3, 5);
                    String sS = stime.substring(6, 9);
                    int min = Integer.parseInt(sm);
                    int sec = Integer.parseInt(ss);
                    int ms = Integer.parseInt(sS);
                    long time = ((min*60 + sec)*1000 + ms)*1000l;
                    while (time2line.containsKey(time))
                        time++;
                    time2line.put(time, line);
                    continue;
                }

                // check whether current line is from chromium log
                m = patternChromium.matcher(line);
                if (m.find()) {
                    String stime = line.substring(m.start(), m.end()); // [0124/083701.603322
                    String sm = stime.substring(8, 10);
                    String ss = stime.substring(10, 12);
                    String sS = stime.substring(13, 19);
                    int min = Integer.parseInt(sm);
                    int sec = Integer.parseInt(ss);
                    int ms = Integer.parseInt(sS);
                    long time = (min*60 + sec)*1000000l + ms;
                    while (time2line.containsKey(time))
                        time++;
                    time2line.put(time, line);
                }
            }
        }

        if (time2line.isEmpty())
            return;

        List<Long> times = new ArrayList<>();
        times.addAll(time2line.keySet());
        times.sort((t1, t2) -> (int)(t1 - t2));

        PrintStream ps = new PrintStream(new File(outputPath));
        for (Long t: times) {
            ps.println(time2line.get(t));
        }

        System.out.println("\t\t totalLines=" + totalLines + ", filtered=" + time2line.size());
    }
}

