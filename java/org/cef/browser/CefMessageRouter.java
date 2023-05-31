package org.cef.browser;

import org.cef.Disposable;
import org.cef.handler.CefMessageRouterHandler;

/**
 * The below classes implement support for routing aynchronous messages between
 * JavaScript running in the renderer process and C++ running in the browser
 * process. An application interacts with the router by passing it data from
 * standard CEF C++ callbacks (OnBeforeBrowse, OnProcessMessageRecieved,
 * OnContextCreated, etc). The renderer-side router supports generic JavaScript
 * callback registration and execution while the browser-side router supports
 * application-specific logic via one or more application-provided Handler
 * instances.
 *
 * The renderer-side router implementation exposes a query function and a cancel
 * function via the JavaScript 'window' object:
 *
 *    // Create and send a new query.
 *    var request_id = window.cefQuery({
 *        request: 'my_request',
 *        persistent: false,
 *        onSuccess: function(response) {},
 *        onFailure: function(error_code, error_message) {}
 *    });
 *
 *    // Optionally cancel the query.
 *    window.cefQueryCancel(request_id);
 *
 * When |window.cefQuery| is executed the request is sent asynchronously to one
 * or more C++ Handler objects registered in the browser process. Each C++
 * Handler can choose to either handle or ignore the query in the
 * Handler::OnQuery callback. If a Handler chooses to handle the query then it
 * should execute Callback::Success when a response is available or
 * Callback::Failure if an error occurs. This will result in asynchronous
 * execution of the associated JavaScript callback in the renderer process. Any
 * queries unhandled by C++ code in the browser process will be automatically
 * canceled and the associated JavaScript onFailure callback will be executed
 * with an error code of -1.
 *
 * Queries can be either persistent or non-persistent. If the query is
 * persistent than the callbacks will remain registered until one of the
 * following conditions are met:
 *
 * A. The query is canceled in JavaScript using the |window.cefQueryCancel|
 *    function.
 * B. The query is canceled in C++ code using the Callback::Failure function.
 * C. The context associated with the query is released due to browser
 *    destruction, navigation or renderer process termination.
 *
 * If the query is non-persistent then the registration will be removed after
 * the JavaScript callback is executed a single time. If a query is canceled for
 * a reason other than Callback::Failure being executed then the associated
 * Handler's OnQueryCanceled method will be called.
 *
 * Some possible usage patterns include:
 *
 * One-time Request. Use a non-persistent query to send a JavaScript request.
 *    The Handler evaluates the request and returns the response. The query is
 *    then discarded.
 *
 * Broadcast. Use a persistent query to register as a JavaScript broadcast
 *    receiver. The Handler keeps track of all registered Callbacks and executes
 *    them sequentially to deliver the broadcast message.
 *
 * Subscription. Use a persistent query to register as a JavaScript subscription
 *    receiver. The Handler initiates the subscription feed on the first request
 *    and delivers responses to all registered subscribers as they become
 *    available. The Handler cancels the subscription feed when there are no
 *    longer any registered JavaScript receivers.
 *
 * Message routing occurs on a per-browser and per-context basis. Consequently,
 * additional application logic can be applied by restricting which browser or
 * context instances are passed into the router. If you choose to use this
 * approach do so cautiously. In order for the router to function correctly any
 * browser or context instance passed into a single router callback must then
 * be passed into all router callbacks.
 *
 * There is generally no need to have multiple renderer-side routers unless you
 * wish to have multiple bindings with different JavaScript function names. It
 * can be useful to have multiple browser-side routers with different client-
 * provided Handler instances when implementing different behaviors on a per-
 * browser basis.
 *
 * This implementation places no formatting restrictions on payload content.
 * An application may choose to exchange anything from simple formatted
 * strings to serialized XML or JSON data.
 *
 *
 * EXAMPLE USAGE
 *
 * 1. Define the router configuration. You can optionally specify settings
 *    like the JavaScript function names. The configuration must be the same in
 *    both the browser and renderer processes. If using multiple routers in the
 *    same application make sure to specify unique function names for each
 *    router configuration.
 *
 *    // Example config object showing the default values.
 *    CefMessageRouterConfig config = new CefMessageRouterConfig();
 *    config.jsQueryFunction = "cefQuery";
 *    config.jsCancelFunction = "cefQueryCancel";
 *
 * 2. Create an instance of CefMessageRouter in the browser process.
 *
 *    messageRouter_ = CefMessageRouter.create(config);
 *
 * 3. Register one or more Handlers. The Handler instances must either outlive
 *    the router or be removed from the router before they're deleted.
 *
 *    messageRouter_.addHandler(myHandler);
 *
 * 4. Add your message router to all CefClient instances you want to get your
 *    JavaScript code be handled.
 *
 *    myClient.addMessageRouter(messageRouter_);
 *
 * 4. Execute the query function from JavaScript code.
 *
 *    window.cefQuery({request: 'my_request',
 *                     persistent: false,
 *                     onSuccess: function(response) { print(response); },
 *                     onFailure: function(error_code, error_message) {} });
 *
 * 5. Handle the query in your CefMessageRouterHandler.onQuery implementation
 *    and execute the appropriate callback either immediately or asynchronously.
 *
 *    public boolean onQuery(CefBrowser browser,
 *                           long query_id,
 *                           String request,
 *                           boolean persistent,
 *                           CefQueryCallback callback) {
 *      if (request.indexOf("my_request") == 0) {
 *        callback.success("my_response");
 *        return true;
 *      }
 *      return false;  // Not handled.
 *    }
 *
 * 6. Notice that the success callback is executed in JavaScript.
 */
public interface CefMessageRouter extends Disposable {
    /**
     * Used to configure the query router. If using multiple router pairs make
     * sure to choose values that do not conflict.
     */
    class CefMessageRouterConfig {
        /**
         * Name of the JavaScript function that will be added to the 'window' object
         * for sending a query. The default value is "cefQuery".
         */
        public String jsQueryFunction;

        /**
         * Name of the JavaScript function that will be added to the 'window' object
         * for canceling a pending query. The default value is "cefQueryCancel".
         */
        public String jsCancelFunction;

        public CefMessageRouterConfig() {
            this("cefQuery", "cefQueryCancel");
        }

        public CefMessageRouterConfig(String queryFunction, String cancelFunction) {
            jsQueryFunction = queryFunction;
            jsCancelFunction = cancelFunction;
        }
    }

    /**
     * Create a new router with the default configuration. The addHandler() method should be called
     * to add a handler.
     */
    static CefMessageRouter create() {
        return CefMessageRouter.create(null, null);
    }

    /**
     * Create a new router with the specified configuration. The addHandler() method should be
     * called to add a handler.
     */
    static CefMessageRouter create(CefMessageRouterConfig config) {
        return CefMessageRouter.create(config, null);
    }

    /**
     * Create a new router with the specified handler and default configuration.
     */
    static CefMessageRouter create(CefMessageRouterHandler handler) {
        return CefMessageRouter.create(null, handler);
    }

    /**
     * Create a new router with the specified handler and configuration.
     */
    static CefMessageRouter create(
            CefMessageRouterConfig config, CefMessageRouterHandler handler) {
        CefMessageRouter_N router = new CefMessageRouter_N(config);
        if (handler != null) router.addHandler(handler, true);
        return router;
    }

    /**
     * Must be called if the CefMessageRouter instance isn't used any more.
     */
    void dispose();

    /**
     * Add a new query handler.
     *
     * @param handler The handler to be added.
     * @param first If true the handler will be added as the first handler, otherwise it will be
     *         added as the last handler.
     * @return True if the handler is added successfully.
     */
    boolean addHandler(CefMessageRouterHandler handler, boolean first);

    /**
     * Remove an existing query handler. Any pending queries associated with the handler will be
     * canceled. onQueryCanceled will be called and the associated JavaScript onFailure callback
     * will be executed with an error code of -1.
     *
     * @param handler The handler to be removed.
     * @return True if the handler is removed successfully.
     */
    boolean removeHandler(CefMessageRouterHandler handler);

    /**
     * Cancel all pending queries associated with either |browser| or |handler|. If both |browser|
     * and |handler| are NULL all pending queries will be canceled. onQueryCanceled will be called
     * and the associated JavaScript onFailure callback will be executed in all cases with an error
     * code of -1.
     *
     * @param browser The associated browser, or null.
     * @param handler The associated handler, or null.
     */
    void cancelPending(CefBrowser browser, CefMessageRouterHandler handler);
}
