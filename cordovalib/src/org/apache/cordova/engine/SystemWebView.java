package org.apache.cordova.engine;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;

public class SystemWebView extends WebView implements CordovaWebViewEngine.EngineView {

  private SystemWebViewClient viewClient;
  private int type = -1;
  private int NO_SUGGESTIONS = 0;
  private int NO_SUGGESTIONS_AGGRESSIVE = 1;
  private boolean nativeContextMenuDisabled = false;
  SystemWebChromeClient chromeClient;
  private SystemWebViewEngine parentEngine;
  private CordovaInterface cordova;

  public SystemWebView(Context context) {
    this(context, null);
  }

  public void setInputType(int type) {
    this.type = type;
  }

  public void setNativeContextMenuDisabled(boolean disabled) {
    this.nativeContextMenuDisabled = disabled;
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    InputConnection ic = super.onCreateInputConnection(outAttrs);
    if (type == NO_SUGGESTIONS) {
      outAttrs.inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    } else if (type == NO_SUGGESTIONS_AGGRESSIVE) {
      outAttrs.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    } else {
      outAttrs.inputType |= InputType.TYPE_NULL;
    }

    return ic;
  }

  @Override
  public ActionMode startActionMode(ActionMode.Callback callback) {
    return suppressActionMode(super.startActionMode(wrapActionModeCallback(callback)));
  }

  @Override
  public ActionMode startActionMode(ActionMode.Callback callback, int type) {
    return suppressActionMode(super.startActionMode(wrapActionModeCallback(callback), type));
  }

  @Override
  public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
    return suppressActionMode(super.startActionModeForChild(originalView, wrapActionModeCallback(callback)));
  }

  @Override
  public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type) {
    return suppressActionMode(super.startActionModeForChild(originalView, wrapActionModeCallback(callback), type));
  }

  private ActionMode.Callback wrapActionModeCallback(ActionMode.Callback callback) {
    if (!nativeContextMenuDisabled || callback == null) {
      return callback;
    }
    return new ActionMode.Callback2() {
      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        boolean created = callback.onCreateActionMode(mode, menu);
        if (created) {
          suppressActionModeUi(mode, menu);
        }
        return created;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean prepared = callback.onPrepareActionMode(mode, menu);
        suppressActionModeUi(mode, menu);
        return prepared;
      }

      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return callback.onActionItemClicked(mode, item);
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {
        callback.onDestroyActionMode(mode);
      }

      @Override
      public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
        if (callback instanceof ActionMode.Callback2) {
          ((ActionMode.Callback2) callback).onGetContentRect(mode, view, outRect);
          return;
        }
        super.onGetContentRect(mode, view, outRect);
      }
    };
  }

  private ActionMode suppressActionMode(ActionMode mode) {
    if (mode == null || !nativeContextMenuDisabled) {
      return mode;
    }
    suppressActionModeUi(mode, mode.getMenu());
    return mode;
  }

  private void suppressActionModeUi(ActionMode mode, Menu menu) {
    if (mode == null || !nativeContextMenuDisabled || menu == null) {
      return;
    }
    menu.clear();
    mode.setTitle(null);
    mode.setSubtitle(null);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      post(() -> {
        if (!nativeContextMenuDisabled) {
          return;
        }
        try {
          mode.hide(0);
        } catch (Throwable ignored) {}
      });
    }
  }

  public SystemWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  void init(SystemWebViewEngine parentEngine, CordovaInterface cordova) {
    this.cordova = cordova;
    this.parentEngine = parentEngine;
    if (this.viewClient == null) {
      setWebViewClient(new SystemWebViewClient(parentEngine));
    }

    if (this.chromeClient == null) {
      setWebChromeClient(new SystemWebChromeClient(parentEngine));
    }
  }

  @Override
  public CordovaWebView getCordovaWebView() {
    return parentEngine != null ? parentEngine.getCordovaWebView() : null;
  }

  @Override
  public void setWebViewClient(WebViewClient client) {
    viewClient = (SystemWebViewClient) client;
    super.setWebViewClient(client);
  }

  @Override
  public void setWebChromeClient(WebChromeClient client) {
    chromeClient = (SystemWebChromeClient) client;
    super.setWebChromeClient(client);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    Boolean ret = parentEngine.client.onDispatchKeyEvent(event);
    if (ret != null) {
      return ret.booleanValue();
    }
    return super.dispatchKeyEvent(event);
  }
}
