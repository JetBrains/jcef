package com.jetbrains.cef.remote.menu;

import java.util.Vector;

public class ContextMenuParams implements org.cef.callback.CefContextMenuParams {
    private final com.jetbrains.cef.remote.thrift_codegen.ContextMenuParams myParams;
    public ContextMenuParams(com.jetbrains.cef.remote.thrift_codegen.ContextMenuParams params) {
        myParams = params;
    }

    @Override
    public int getXCoord() {
        return myParams.x;
    }

    @Override
    public int getYCoord() {
        return myParams.y;
    }

    @Override
    public int getTypeFlags() {
        return myParams.type_flags;
    }

    @Override
    public String getLinkUrl() {
        return myParams.link_url;
    }

    @Override
    public String getUnfilteredLinkUrl() {
        return myParams.unfiltered_link_url;
    }

    @Override
    public String getSourceUrl() {
        return myParams.source_url;
    }

    @Override
    public boolean hasImageContents() {
        return myParams.has_image_contents;
    }

    @Override
    public String getPageUrl() {
        return myParams.page_url;
    }

    @Override
    public String getFrameUrl() {
        return myParams.frame_url;
    }

    @Override
    public String getFrameCharset() {
        return myParams.frame_charset;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.values()[myParams.media_type] ;
    }

    @Override
    public int getMediaStateFlags() {
        return myParams.media_state_flags;
    }

    @Override
    public String getSelectionText() {
        return myParams.selected_text;
    }

    @Override
    public String getMisspelledWord() {
        return myParams.misspelled_word;
    }

    @Override
    public boolean getDictionarySuggestions(Vector<String> suggestions) {
        // not implemented
        return false;
    }

    @Override
    public boolean isEditable() {
        return myParams.is_editable;
    }

    @Override
    public boolean isSpellCheckEnabled() {
        return myParams.is_spellcheck_enabled;
    }

    @Override
    public int getEditStateFlags() {
        return myParams.edit_state_flags;
    }

    @Override
    public String toString() {
        return "ContextMenuParams{" +
                "myParams=" + myParams +
                '}';
    }
}
