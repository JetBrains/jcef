// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.handler;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.callback.CefCommandLine;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.misc.CefLog;

import java.util.*;

/**
 * An abstract adapter class for managing app handler events.
 * The methods in this class are using a default implementation.
 * This class exists as convenience for creating handler objects.
 */
public abstract class CefAppHandlerAdapter implements CefAppHandler {
    private String[] args_;

    public CefAppHandlerAdapter(String[] args) {
        args_ = args;
    }

    public void updateArgs(String[] args) {
        Set<String> keysToRemove = new HashSet<>();
        for (String arg: args) {
            keysToRemove.add(arg.split("=", 2)[0]);
        }

        List<String> result = new ArrayList<>();
        for (String arg: args_) {
            String key = arg.split("=", 2)[0];
            if (!keysToRemove.contains(key)) {
                result.add(arg);
            }
        }

        Collections.addAll(result, args);
        args_ = result.toArray(new String[0]);
    }

    public String[] getArgs() {
        return args_ == null ? null : Arrays.copyOf(args_, args_.length);
    }

    @Override
    public void onBeforeCommandLineProcessing(String process_type, CefCommandLine command_line) {
        if (process_type.isEmpty() && args_ != null) {
            // Forward switches and arguments from Java to Cef
            boolean parseSwitchesDone = false;
            for (String arg : args_) {
                if (parseSwitchesDone || arg.length() < 2) {
                    command_line.appendArgument(arg);
                    continue;
                }
                // Arguments with '--', '-' and, on Windows, '/' prefixes are considered switches.
                int switchCnt = arg.startsWith("--") ? 2
                        : arg.startsWith("/")        ? 1
                        : arg.startsWith("-")        ? 1
                                                     : 0;
                switch (switchCnt) {
                    case 2:
                        // An argument of "--" will terminate switch parsing with all subsequent
                        // tokens
                        if (arg.length() == 2) {
                            parseSwitchesDone = true;
                            continue;
                        }
                    // FALL THRU
                    case 1: {
                        // Switches can optionally have a value specified using the '=' delimiter
                        // (e.g. "-switch=value").
                        String[] switchVals = arg.substring(switchCnt).split("=");
                        if (switchVals.length == 2) {
                            command_line.appendSwitchWithValue(switchVals[0], switchVals[1]);
                        } else {
                            command_line.appendSwitch(switchVals[0]);
                        }
                        break;
                    }
                    case 0:
                        command_line.appendArgument(arg);
                        break;
                }
            }
        }
    }

    @Override
    public boolean onBeforeTerminate() {
        // The default implementation does nothing
        return false;
    }

    @Override
    public void stateHasChanged(CefAppState state) {
        // The default implementation does nothing
    }

    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        // The default implementation does nothing
    }

    @Override
    public void onContextInitialized() {
        // The default implementation does nothing
    }

    @Override
    public void onScheduleMessagePumpWork(long delay_ms) {
        if (CefApp.getState() == CefAppState.TERMINATED) {
            CefLog.Debug("CefApp is terminated, skip doMessageLoopWork");
            return;
        }
        CefApp.getInstance().doMessageLoopWork(delay_ms);
    }

    @Override
    public void onBeforeChildProcessLaunch(String command_line) {
        // The default implementation does nothing
    }
}
