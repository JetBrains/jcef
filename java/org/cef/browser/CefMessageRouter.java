package org.cef.browser;

import org.cef.handler.CefMessageRouterHandler;

public interface CefMessageRouter {
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
