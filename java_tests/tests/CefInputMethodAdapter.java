package tests;

import org.cef.browser.CefBrowser;
import org.cef.input.CefCompositionUnderline;
import org.cef.misc.CefLog;
import org.cef.misc.CefRange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CefInputMethodAdapter implements InputMethodRequests, InputMethodListener {
    volatile private CefBrowser myBrowser;
    private final JComponent myComponent;

    private volatile String mySelectedText = "";
    private volatile Rectangle[] myCharacterBounds = {};

    public CefInputMethodAdapter(CefBrowser browser, JComponent component) {
        this.myBrowser = browser;
        this.myComponent = component;
    }

    @Override
    public void inputMethodTextChanged(InputMethodEvent event) {
        if (myBrowser == null) {
            return;
        }

        CefLog.Debug("InputMethodListener::inputMethodTextChanged(" + event + ")");
        int committedCharacterCount = event.getCommittedCharacterCount();

        AttributedCharacterIterator text = event.getText();
        char c = text.first();
        if (committedCharacterCount > 0) {
            StringBuilder textBuffer = new StringBuilder();
            while (committedCharacterCount-- > 0) {
                textBuffer.append(c);
                c = text.next();
            }

            String committedText = textBuffer.toString();
            CefLog.Debug("Browser::ImeCommitText(text=" + committedText + ", selectionRange=" + new CefRange(-1, -1) + ", relativeCursorPos=0)");
            myBrowser.ImeCommitText(committedText, new CefRange(-1, -1), 0);
        }

        StringBuilder textBuffer = new StringBuilder();
        while (c != CharacterIterator.DONE) {
            textBuffer.append(c);
            c = text.next();
        }

        var compositionText = textBuffer.toString();
        if (!compositionText.isEmpty()) {
            CefRange selectionRange = new CefRange(compositionText.length(), compositionText.length());
            CefLog.Debug("Browser::ImeSetComposition(text='" + compositionText + "', replacementRange=" + new CefRange(-1, -1) + ", selectionRange=" + new CefRange(-1, -1) + ")");
            Color color = new Color(0, true);
            List<CefCompositionUnderline> underlines = new ArrayList<>();
            underlines.add(new CefCompositionUnderline(
                    new CefRange(0, compositionText.length()), color, color, 0, CefCompositionUnderline.Style.SOLID));
            myBrowser.ImeSetComposition(compositionText, underlines, new CefRange(-1, -1), new CefRange(-1, -1));
        }
        event.consume();
    }

    @Override
    public void caretPositionChanged(InputMethodEvent event) {
        CefLog.Debug("InputMethodListener::caretPositionChanged(" + event + ")");
    }

    void setBrowser(CefBrowser cefBrowser) {
        myBrowser = cefBrowser;
    }

    @Override
    public Rectangle getTextLocation(TextHitInfo offset) {
        Rectangle[] boxes = myCharacterBounds;
        Rectangle result;
        if (boxes == null || boxes.length < 1) {
            result = new Rectangle(0, 0);
        } else {
            result = new Rectangle(boxes[0]);
        }

        var componentLocation = myComponent.getLocationOnScreen();
        CefLog.Debug("InputMethodRequests::getTextLocation(offset=" + offset + ") -> " + result + "+" + componentLocation);
        result.translate(componentLocation.x, componentLocation.y);
        return result;
    }

    @Override
    public TextHitInfo getLocationOffset(int x, int y) {
        var componentLocation = myComponent.getLocationOnScreen();
        Point p = new Point(x, y);
        p.translate(componentLocation.x, componentLocation.y);

        TextHitInfo res = null;
        for (int i = 0; i < myCharacterBounds.length; i++) {
            Rectangle r = myCharacterBounds[i];
            if (r.contains(p)) {
                res = TextHitInfo.leading(i);
                break;
            }
        }

        CefLog.Debug("InputMethodRequests::getLocationOffset(x=" + x + ", y=" + y + ") -> " + res);
        return res;
    }

    @Override
    public int getInsertPositionOffset() {
        CefLog.Debug("InputMethodRequests::getInsertPositionOffset() -> " + 0);
        return 0;
    }

    @Override
    public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
        CefLog.Debug("InputMethodRequests::getCommittedText() -> ''");
        return new AttributedString("").getIterator();
    }

    @Override
    public int getCommittedTextLength() {
        CefLog.Debug("InputMethodRequests::getCommittedTextLength() -> 0");
        return 0;
    }

    @Override
    public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
        CefLog.Debug("InputMethodRequests::cancelLatestCommittedText() -> 0");
        return new AttributedString("").getIterator();
    }

    @Override
    public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
        CefLog.Debug("InputMethodRequests::getSelectedText() -> '" + mySelectedText + "'");
        return new AttributedString(mySelectedText).getIterator();
    }

    public void OnImeCompositionRangeChanged(CefRange selectionRange, Rectangle[] characterBounds) {
        CefLog.Debug("CefRenderHandler::OnImeCompositionRangeChanged(selectionRange=" + selectionRange + ", characterBounds=" + Arrays.toString(characterBounds) + ")");
        this.myCharacterBounds = characterBounds;
    }

    public void OnTextSelectionChanged(String selectedText, CefRange selectionRange) {
        CefLog.Debug("CefRenderHandler::OnTextSelectionChanged(selectionText=" + selectedText + ", " + "selectionRange=" + selectionRange + ")");
        this.mySelectedText = selectedText;
    }
}
