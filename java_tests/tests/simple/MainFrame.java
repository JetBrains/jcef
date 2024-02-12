// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.simple;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.*;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.security.CefCertStatus;
import org.cef.callback.CefCallback;
import org.cef.handler.*;
import org.cef.security.CefSSLInfo;
import tests.OsrSupport;
import tests.detailed.dialog.CertErrorDialog;
import tests.detailed.util.DataUri;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;


/**
 * This is a simple example application using JCEF.
 * It displays a JFrame with a JTextField at its top and a CefBrowser in its
 * center. The JTextField is used to enter and assign an URL to the browser UI.
 * No additional handlers or callbacks are used in this example.
 *
 * The number of used JCEF classes is reduced (nearly) to its minimum and should
 * assist you to get familiar with JCEF.
 *
 * For a more feature complete example have also a look onto the example code
 * within the package "tests.detailed".
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = -5570653778104813836L;
    private final JTextField address_;
    private final CefApp cefApp_;
    private final CefClient client_;
    private final CefBrowser browser_;
    private final Component browserUI_;
    private boolean browserFocus_ = true;
    private JFrame fullscreenFrame_;

    /**
     * To display a simple browser window, it suffices completely to create an
     * instance of the class CefBrowser and to assign its UI component to your
     * application (e.g. to your content pane).
     * But to be more verbose, this CTOR keeps an instance of each object on the
     * way to the browser UI.
     */
    private MainFrame(String[] args, String startURL, boolean useOSR, boolean isTransparent) {
        JCefAppConfig config = JCefAppConfig.getInstance();
        List<String> appArgs = new ArrayList<>(Arrays.asList(args));
        appArgs.addAll(config.getAppArgsAsList());
        args = appArgs.toArray(new String[0]);

        // (1) The entry point to JCEF is always the class CefApp. There is only one
        //     instance per application and therefore you have to call the method
        //     "getInstance()" instead of a CTOR.
        //
        //     CefApp is responsible for the global CEF context. It loads all
        //     required native libraries, initializes CEF accordingly, starts a
        //     background task to handle CEF's message loop and takes care of
        //     shutting down CEF after disposing it.
        CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
            @Override
            public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                // Shutdown the app if the native CEF part is terminated
                if (state == CefAppState.TERMINATED) System.exit(0);
            }
        });
        CefSettings settings = config.getCefSettings();
        cefApp_ = CefApp.getInstance(settings);

        // (2) JCEF can handle one to many browser instances simultaneous. These
        //     browser instances are logically grouped together by an instance of
        //     the class CefClient. In your application you can create one to many
        //     instances of CefClient with one to many CefBrowser instances per
        //     client. To get an instance of CefClient you have to use the method
        //     "createClient()" of your CefApp instance. Calling an CTOR of
        //     CefClient is not supported.
        //
        //     CefClient is a connector to all possible events which come from the
        //     CefBrowser instances. Those events could be simple things like the
        //     change of the browser title or more complex ones like context menu
        //     events. By assigning handlers to CefClient you can control the
        //     behavior of the browser. See tests.detailed.MainFrame for an example
        //     of how to use these handlers.
        client_ = cefApp_.createClient();

        // (3) One CefBrowser instance is responsible to control what you'll see on
        //     the UI component of the instance. It can be displayed off-screen
        //     rendered or windowed rendered. To get an instance of CefBrowser you
        //     have to call the method "createBrowser()" of your CefClient
        //     instances.
        //
        //     CefBrowser has methods like "goBack()", "goForward()", "loadURL()",
        //     and many more which are used to control the behavior of the displayed
        //     content. The UI is held within a UI-Compontent which can be accessed
        //     by calling the method "getUIComponent()" on the instance of CefBrowser.
        //     The UI component is inherited from a java.awt.Component and therefore
        //     it can be embedded into any AWT UI.
        if (useOSR) {
            browser_ = OsrSupport.createBrowser(client_, startURL);
        } else {
            browser_ = client_.createBrowser(startURL, useOSR, isTransparent);
        }

        browserUI_ = browser_.getUIComponent();

        // (4) For this minimal browser, we need only a text field to enter an URL
        //     we want to navigate to and a CefBrowser window to display the content
        //     of the URL. To respond to the input of the user, we're registering an
        //     anonymous ActionListener. This listener is performed each time the
        //     user presses the "ENTER" key within the address field.
        //     If this happens, the entered value is passed to the CefBrowser
        //     instance to be loaded as URL.
        address_ = new JTextField(startURL, 100);
        address_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browser_.loadURL(address_.getText());
            }
        });

        // Update the address field when the browser URL changes.
        client_.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                address_.setText(url);
            }
            @Override
            public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {
                setBrowserFullscreen(fullscreen);
            }
        });

        client_.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onCertificateError(CefBrowser browser, CefLoadHandler.ErrorCode cert_error,
                                              String request_url, CefSSLInfo sslInfo, CefCallback callback) {
                CertErrorDialog dialog = new CertErrorDialog(MainFrame.this, cert_error, request_url, new CefCallback() {
                    @Override
                    public void Continue() {
                        callback.Continue();
                    }

                    @Override
                    public void cancel() {
                        callback.cancel();
                        browser_.loadURL(DataUri.create("text/html", MakeErrorPage(request_url, cert_error, sslInfo)));
                    }
                });
                SwingUtilities.invokeLater(dialog);
                return true;
            }
        });

        // Clear focus from the browser when the address field gains focus.
        address_.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!browserFocus_) return;
                browserFocus_ = false;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                address_.requestFocus();
            }
        });

        // Clear focus from the address field when the browser gains focus.
        client_.addFocusHandler(new CefFocusHandlerAdapter() {
            @Override
            public void onGotFocus(CefBrowser browser) {
                if (browserFocus_) return;
                browserFocus_ = true;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                browser.setFocus(true);
            }

            @Override
            public void onTakeFocus(CefBrowser browser, boolean next) {
                browserFocus_ = false;
            }
        });

        // (5) All UI components are assigned to the default content pane of this
        //     JFrame and afterwards the frame is made visible to the user.
        getContentPane().add(address_, BorderLayout.NORTH);
        getContentPane().add(browserUI_, BorderLayout.CENTER);
        pack();
        setSize(800, 600);
        setVisible(true);

        // (6) To take care of shutting down CEF accordingly, it's important to call
        //     the method "dispose()" of the CefApp instance if the Java
        //     application will be closed. Otherwise you'll get asserts from CEF.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CefApp.getInstance().dispose();
                dispose();
            }
        });
    }

    private static String normalize(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String DumpCertData(X509Certificate certificate) {
        StringBuilder builder = new StringBuilder();
        builder.append("<b>Subject: </b>");
        builder.append(certificate.getSubjectX500Principal());
        builder.append("<br/><b>Issuer: </b>");
        builder.append(certificate.getIssuerX500Principal());
        builder.append("<br/><b>Validity: </b>");
        builder.append(certificate.getNotBefore());
        builder.append(" - ");
        builder.append(certificate.getNotAfter());
        builder.append("<br/><b>DER Encoded: </b>");
        try {
            builder.append(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static String MakeErrorPage(String request_url, CefLoadHandler.ErrorCode cert_error, CefSSLInfo info) {
        StringBuilder page = new StringBuilder();
        page.append("<html><head><title>Page failed to load</title></head><body bgcolor=\"white\">");
        page.append("<h3>Page failed to load.</h3>URL: <a href=\"");
        page.append(request_url);
        page.append("\">");
        page.append(request_url);
        page.append("</a><br/>Error: ");
        page.append(cert_error);
        page.append("(");
        page.append(cert_error.getCode());
        page.append(")<br/><h3>X.509 Certificate Information:</h3>Certificate status: ");
        page.append(Arrays.stream(CefCertStatus.values())
                .skip(1) // skip CERT_STATUS_NONE
                .filter(status -> status.hasStatus(info.statusBiset))
                .map(Enum::toString)
                .collect(Collectors.joining(", ")));
        page.append("<h4>Certificated chain(from subject to issuers):</h4>");
        page.append("<table border=1 width=\"100%\">");
        page.append("<tr><td style=\"max-width:500px;overflow:scroll;word-wrap:break-word;\">");
        page.append(Arrays.stream(info.certificate.getCertificatesChain())
                .map(MainFrame::DumpCertData)
                .collect(Collectors.joining("</td></tr><tr><td style=\"max-width:500px;overflow:scroll;word-wrap:break-word;\">")));
        page.append("</td></tr></table></body></html>");
        return page.toString();
    }

    public void setBrowserFullscreen(boolean fullscreen) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (fullscreen) {
                    if (fullscreenFrame_ == null) {
                        fullscreenFrame_ = new JFrame();
                        fullscreenFrame_.setUndecorated(true);
                        fullscreenFrame_.setResizable(true);
                    }
                    GraphicsConfiguration gc = MainFrame.this.getGraphicsConfiguration();
                    fullscreenFrame_.setBounds(gc.getBounds());
                    gc.getDevice().setFullScreenWindow(fullscreenFrame_);

                    getContentPane().remove(browserUI_);
                    fullscreenFrame_.add(browserUI_);
                    fullscreenFrame_.setVisible(true);
                    fullscreenFrame_.validate();
                } else {
                    fullscreenFrame_.remove(browserUI_);
                    fullscreenFrame_.setVisible(false);
                    getContentPane().add(browserUI_, BorderLayout.CENTER);
                    getContentPane().validate();
                }
            }
        });
    }

    public static void main(String[] args) {
        // Perform startup initialization on platforms that require it.
        CefApp.startup(args);

        new MainFrame(args, "http://www.google.com", OsrSupport.isEnabled(), false);
    }
}
