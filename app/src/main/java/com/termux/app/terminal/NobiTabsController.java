package com.termux.app.terminal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NobiTerminal session tabs bar controller.
 *
 * Builds and manages the tab strip shown in the header of {@link TermuxActivity}.
 * The strip mirrors the very same sessions that the navigation drawer lists:
 * both operate on {@link TermuxService#getTermuxSessions()}, so there is no
 * duplicate session state. The tab display order is kept at the activity level
 * only (the service keeps its own creation order); the arrangement is persisted
 * as a list of {@link TerminalSession#mHandle} values so it survives activity
 * recreation, and newly created sessions are appended at the end.
 */
public class NobiTabsController {

    private static final String PREFS_NAME = "nobi_tabs";
    private static final String PREF_KEY_ORDER = "tab_order";
    private static final String PREF_KEY_NAMES = "tab_names";
    // v6 (9/10): per-tab note + colour, same "handle<TAB>value" encoding.
    private static final String PREF_KEY_NOTES = "tab_notes";
    private static final String PREF_KEY_COLORS = "tab_colors";

    // v27: spec section 3 tabs — fully rounded cards (12dp ALL corners)
    // with a terminal icon, 1dp outline and a restrained blue glow on
    // the active tab. This supersedes v26's top-only rounded Chrome
    // tabs (the reference screenshot's tabs are individual rounded
    // cards, not flush strips). Order, persistence, drag-reorder,
    // rename, notes, colours and running/exited states are all untouched.

    private final TermuxActivity mActivity;
    private final HorizontalScrollView mTabsScrollView;
    private final LinearLayout mTabsContainer;

    /** Sessions in tab display order. Never the service's own list object. */
    private final ArrayList<TerminalSession> mOrderedSessions = new ArrayList<>();

    // Drag-to-rearrange state.
    private View mDragView;
    private TerminalSession mDragSession;
    private float mDragDownRawX;
    private float mDragStartRawX;
    private boolean mDragActive;

    public NobiTabsController(TermuxActivity activity) {
        mActivity = activity;
        mTabsScrollView = activity.findViewById(R.id.nobi_tabs_scroll_view);
        mTabsContainer = activity.findViewById(R.id.nobi_tabs_container);

        TextView addButton = activity.findViewById(R.id.nobi_tab_add);
        if (addButton != null) {
            addButton.setOnClickListener(v -> {
                mActivity.getTermuxTerminalSessionClient().addNewSession(false, null);
                refreshTabs();
            });
        }
    }

    /** Sessions in tab display order (copy). Used by the session tools dialogs. */
    public List<TerminalSession> getOrderedSessions() {
        return new ArrayList<>(mOrderedSessions);
    }

    /** Display label for a session (custom name, session name, title, index). */
    public String getTabLabel(TerminalSession session) {
        return resolveTabLabel(session, mOrderedSessions.indexOf(session));
    }

    // v6 (9/10): notes + colours, shared with TermuxActivity's dialogs.
    public static Map<String, String> loadTabNotes(Context context) {
        Map<String, String> notes = new HashMap<>();
        Set<String> saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(PREF_KEY_NOTES, null);
        if (saved != null) {
            for (String entry : saved) {
                int separator = entry.indexOf('\t');
                if (separator > 0)
                    notes.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
        }
        return notes;
    }

    public static void saveTabNote(Context context, String handle, String note) {
        Map<String, String> notes = loadTabNotes(context);
        if (note == null || note.isEmpty())
            notes.remove(handle);
        else
            notes.put(handle, note);
        Set<String> entries = new HashSet<>();
        for (Map.Entry<String, String> entry : notes.entrySet())
            entries.add(entry.getKey() + "\t" + entry.getValue());
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(PREF_KEY_NOTES, entries).apply();
    }

    public static Map<String, Integer> loadTabColors(Context context) {
        Map<String, Integer> colors = new HashMap<>();
        Set<String> saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(PREF_KEY_COLORS, null);
        if (saved != null) {
            for (String entry : saved) {
                int separator = entry.indexOf('\t');
                if (separator > 0) {
                    try {
                        colors.put(entry.substring(0, separator),
                            Integer.parseInt(entry.substring(separator + 1)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return colors;
    }

    public static void saveTabColor(Context context, String handle, Integer color) {
        Map<String, Integer> colors = loadTabColors(context);
        if (color == null)
            colors.remove(handle);
        else
            colors.put(handle, color);
        Set<String> entries = new HashSet<>();
        for (Map.Entry<String, Integer> entry : colors.entrySet())
            entries.add(entry.getKey() + "\t" + entry.getValue());
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(PREF_KEY_COLORS, entries).apply();
    }

    /** Rebuild the strip from the service's current session list. Safe to call at any time. */
    public void refreshTabs() {
        if (mTabsContainer == null) return;

        List<TerminalSession> liveSessions = new ArrayList<>();
        TermuxService service = mActivity.getTermuxService();
        if (service != null) {
            for (TermuxSession termuxSession : service.getTermuxSessions()) {
                if (termuxSession == null) continue;
                TerminalSession session = termuxSession.getTerminalSession();
                if (session != null) liveSessions.add(session);
            }
        }

        reconcileOrder(liveSessions);
        rebuildTabViews();
    }

    /**
     * Merge the persisted tab order with the live session list: sessions that
     * are gone are dropped, sessions with a saved position keep it, and new
     * sessions are appended at the end in service order.
     */
    private void reconcileOrder(List<TerminalSession> liveSessions) {
        ArrayList<TerminalSession> ordered = new ArrayList<>();
        for (String handle : loadSavedOrder()) {
            TerminalSession session = findSessionByHandle(liveSessions, handle);
            if (session != null && !ordered.contains(session)) ordered.add(session);
        }
        for (TerminalSession session : liveSessions) {
            if (!ordered.contains(session)) ordered.add(session);
        }
        mOrderedSessions.clear();
        mOrderedSessions.addAll(ordered);
        saveOrder();
    }

    private TerminalSession findSessionByHandle(List<TerminalSession> sessions, String handle) {
        if (handle == null) return null;
        for (TerminalSession session : sessions) {
            if (handle.equals(session.mHandle)) return session;
        }
        return null;
    }

    private List<String> loadSavedOrder() {
        ArrayList<String> handles = new ArrayList<>();
        String saved = getPrefs().getString(PREF_KEY_ORDER, null);
        if (!TextUtils.isEmpty(saved)) {
            for (String handle : saved.split(",")) {
                if (!handle.isEmpty()) handles.add(handle);
            }
        }
        return handles;
    }

    private void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (TerminalSession session : mOrderedSessions) {
            if (sb.length() > 0) sb.append(',');
            sb.append(session.mHandle);
        }
        getPrefs().edit().putString(PREF_KEY_ORDER, sb.toString()).apply();
    }

    private SharedPreferences getPrefs() {
        return mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void rebuildTabViews() {
        mTabsContainer.removeAllViews();
        mDragActive = false;
        mDragView = null;
        mDragSession = null;

        TerminalSession currentSession = mActivity.getCurrentSession();
        int activeIndex = -1;

        for (int i = 0; i < mOrderedSessions.size(); i++) {
            TerminalSession session = mOrderedSessions.get(i);
            boolean isActive = session == currentSession;
            if (isActive) activeIndex = i;
            mTabsContainer.addView(createTabView(session, i, isActive));
        }

        if (activeIndex >= 0) {
            final int indexToShow = activeIndex;
            mTabsContainer.post(() -> {
                if (indexToShow < mTabsContainer.getChildCount()) {
                    View tab = mTabsContainer.getChildAt(indexToShow);
                    if (tab != null && mTabsScrollView != null)
                        mTabsScrollView.smoothScrollTo(tab.getLeft(), 0);
                }
            });
        }

        // v6 (9): active tab's note under the tabs bar (hidden when empty).
        TextView noteView = mActivity.findViewById(R.id.nobi_tab_note);
        if (noteView != null) {
            String note = null;
            if (currentSession != null)
                note = loadTabNotes(mActivity).get(currentSession.mHandle);
            if (note == null || note.isEmpty()) {
                noteView.setVisibility(View.GONE);
            } else {
                noteView.setText(note);
                noteView.setVisibility(View.VISIBLE);
            }
        }
    }

    /** Resolve a mode-aware design-system colour. */
    private int dsColor(int resId) {
        return ContextCompat.getColor(mActivity, resId);
    }

    private View createTabView(final TerminalSession session, int displayIndex, boolean isActive) {
        LinearLayout tab = new LinearLayout(mActivity);
        tab.setOrientation(LinearLayout.HORIZONTAL);
        tab.setGravity(Gravity.CENTER_VERTICAL);
        // v7 (71/84/87): tab style + density.
        SharedPreferences v7Prefs = mActivity.getSharedPreferences("nobi_v7", Context.MODE_PRIVATE);
        String tabStyle = v7Prefs.getString("tab_style", "rounded");
        int activeText = isActive ? dsColor(R.color.nobi_ds_text_primary)
            : dsColor(R.color.nobi_ds_text_secondary);
        int failedText = dsColor(R.color.nobi_ds_error);
        // v31.1 (item 7): Acode-style tabs — SAME TO SAME as the
        // editor's .open-file-list (acode-web src/main.scss): 30px
        // strip, 120px-wide square tabs (no rounding, flush against
        // each other), active tab = 2px top border in the accent
        // colour + darkened body. The v27 rounded cards (12dp
        // corners, 1dp outline, blue halo) and the v31 36dp floating
        // card are gone. The session NUMBER (v31) still stands where
        // the ">_" icon was. Order, persistence, drag-reorder,
        // rename, notes, colours and running/exited states are all
        // untouched.
        android.graphics.drawable.Drawable tabBackground;
        if (isActive) {
            // Layer 0 = accent (full rect), layer 1 = selected body
            // inset 2dp from the top -> an exact 2dp top border.
            android.graphics.drawable.GradientDrawable border =
                new android.graphics.drawable.GradientDrawable();
            border.setColor(dsColor(R.color.nobi_ds_accent));
            android.graphics.drawable.GradientDrawable body =
                new android.graphics.drawable.GradientDrawable();
            body.setColor(dsColor(R.color.nobi_ds_selected));
            android.graphics.drawable.LayerDrawable layered =
                new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{border, body});
            layered.setLayerInset(1, 0, dp(2), 0, 0);
            tabBackground = layered;
        } else {
            android.graphics.drawable.GradientDrawable tabBg =
                new android.graphics.drawable.GradientDrawable();
            tabBg.setColor(dsColor(R.color.nobi_ds_surface));
            tabBackground = tabBg;
        }
        tab.setBackground(tabBackground);
        // Square Acode tile: no vertical padding (children are centred
        // by the tab's CENTER_VERTICAL gravity), 10dp start / 4dp end.
        tab.setPadding(dp(10), 0, dp(4), 0);

        // Fixed 120dp width, MATCH_PARENT height (30dp strip), flush
        // against neighbouring tabs (0 margins) — Acode tiles sit edge
        // to edge in the strip, no gaps.
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
            dp(120), LinearLayout.LayoutParams.MATCH_PARENT);
        tabParams.setMargins(0, 0, 0, 0);
        tab.setLayoutParams(tabParams);

        // v6 (10): per-tab colour dot, shown before the title when set.
        Integer tabColor = loadTabColors(mActivity).get(session.mHandle);
        if (tabColor != null) {
            View dot = new View(mActivity);
            dot.setBackgroundColor(tabColor);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
            dotParams.setMargins(0, 0, dp(6), 0);
            dot.setLayoutParams(dotParams);
            tab.addView(dot);
        }

        // v31: session NUMBER before the title (1, 2, 3...) — the ">_"
        // terminal icon is gone. Bold, monospace, accent on the active
        // tab so the number reads at a glance.
        TextView numberView = new TextView(mActivity);
        numberView.setText(String.valueOf(displayIndex + 1));
        numberView.setTextSize(13);
        numberView.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        numberView.setTextColor(isActive ? dsColor(R.color.nobi_ds_accent) : activeText);
        numberView.setPadding(0, 0, dp(6), 0);
        tab.addView(numberView);
        // Exited session: strike through the number too (the title,
        // which used to carry this, is hidden when it equals the
        // number — see below).
        if (!session.isRunning()) {
            numberView.setPaintFlags(numberView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            if (session.getExitStatus() != 0)
                numberView.setTextColor(failedText);
        }

        // v31.1 (item 3): the title is shown ONLY when it says
        // something the number doesn't — resolveTabLabel falls back
        // to the session number itself ("1"), so showing both gave
        // "1 1 ×" in v31. Number-only tabs show just the number.
        // v31.1 (item 4): the title (or a spacer when there is no
        // title) takes weight 1, so the × is always pinned to the
        // tab's far right end — before this the tab packed its
        // children from the left and the × sat mid-tab whenever the
        // tab was wider than its content (minWidth 88dp).
        String tabLabel = resolveTabLabel(session, displayIndex);
        if (!tabLabel.equals(String.valueOf(displayIndex + 1))) {
            TextView titleView = new TextView(mActivity);
            titleView.setText(tabLabel);
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setTextSize(13);
            titleView.setTextColor(activeText);
            // v7 (71): underline style underlines the active tab's title;
            // dots style prefixes the active tab with an accent dot.
            if ("underline".equals(tabStyle) && isActive) {
                titleView.setPaintFlags(titleView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            } else if ("dots".equals(tabStyle) && isActive) {
                titleView.setText("● " + tabLabel);
            }
            if (!session.isRunning()) {
                titleView.setPaintFlags(titleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                if (session.getExitStatus() != 0)
                    titleView.setTextColor(failedText);
            }
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            titleView.setLayoutParams(titleParams);
            tab.addView(titleView);
        } else {
            View spacer = new View(mActivity);
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0, 0, 1f);
            spacer.setLayoutParams(spacerParams);
            tab.addView(spacer);
        }

        // v31: close button pinned to the tab's RIGHT corner — 28dp
        // square tap target, 18sp ×, no side padding (the tab's own
        // 4dp end padding is the corner gap). Before this the × was a
        // 15sp glyph with 8dp padding, glued inside the tab.
        TextView closeView = new TextView(mActivity);
        closeView.setText("×");
        closeView.setTextSize(18);
        closeView.setGravity(Gravity.CENTER);
        closeView.setTextColor(activeText);
        closeView.setContentDescription("Close session");
        closeView.setPadding(0, 0, 0, 0);
        closeView.setMinWidth(dp(28));
        closeView.setMinimumWidth(dp(28));
        closeView.setMinHeight(dp(28));
        closeView.setMinimumHeight(dp(28));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            dp(28), dp(28));
        closeParams.setMargins(dp(4), 0, 0, 0);
        closeView.setLayoutParams(closeParams);
        closeView.setOnClickListener(v -> closeSession(session));
        tab.addView(closeView);

        tab.setOnClickListener(v -> {
            mActivity.getTermuxTerminalSessionClient().setCurrentSession(session);
            refreshTabs();
        });
        tab.setOnLongClickListener(v -> startDrag(tab, session));
        final GestureDetector tabGestureDetector = new GestureDetector(mActivity,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    showRenameDialog(session);
                    return true;
                }
            });
        tab.setOnTouchListener((v, event) -> {
            tabGestureDetector.onTouchEvent(event);
            return onTabTouch(tab, session, event);
        });
        return tab;
    }

    private String resolveTabLabel(TerminalSession session, int displayIndex) {
        String customName = loadTabNames().get(session.mHandle);
        if (!TextUtils.isEmpty(customName)) return customName;
        if (!TextUtils.isEmpty(session.mSessionName)) return session.mSessionName;
        String title = session.getTitle();
        if (!TextUtils.isEmpty(title)) return title;
        return String.valueOf(displayIndex + 1);
    }

    // region Tab rename (double-tap a tab)

    /**
     * Custom tab names keyed by session handle, persisted in the same
     * "nobi_tabs" preferences file as the tab order, as a string set of
     * "handle<TAB>name" entries.
     */
    private Map<String, String> loadTabNames() {
        Map<String, String> names = new HashMap<>();
        Set<String> saved = getPrefs().getStringSet(PREF_KEY_NAMES, null);
        if (saved != null) {
            for (String entry : saved) {
                int separator = entry.indexOf('\t');
                if (separator > 0)
                    names.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
        }
        return names;
    }

    /** Save (or, for an empty name, clear) the custom name of a session tab. */
    private void saveTabName(TerminalSession session, String name) {
        Map<String, String> names = loadTabNames();
        if (TextUtils.isEmpty(name))
            names.remove(session.mHandle);
        else
            names.put(session.mHandle, name);

        Set<String> entries = new HashSet<>();
        for (Map.Entry<String, String> entry : names.entrySet())
            entries.add(entry.getKey() + "\t" + entry.getValue());
        getPrefs().edit().putStringSet(PREF_KEY_NAMES, entries).apply();
    }

    private void showRenameDialog(final TerminalSession session) {
        String customName = loadTabNames().get(session.mHandle);

        final EditText input = new EditText(mActivity);
        input.setSingleLine(true);
        input.setHint("Tab name");
        input.setText(customName != null ? customName :
            resolveTabLabel(session, mOrderedSessions.indexOf(session)));
        input.setSelectAllOnFocus(true);
        int horizontalPadding = dp(20);
        input.setPadding(horizontalPadding, dp(12), horizontalPadding, dp(4));

        new AlertDialog.Builder(mActivity)
            .setTitle("Rename tab")
            .setView(input)
            .setPositiveButton("Save", (dialog, which) -> {
                saveTabName(session, input.getText().toString().trim());
                refreshTabs();
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset", (dialog, which) -> {
                saveTabName(session, null);
                refreshTabs();
            })
            .show();
    }

    // endregion

    /**
     * Close a session from its tab. A running session is killed first (the same
     * primitive the app's own "kill process" action uses); the actual removal
     * and focus handling then go through the app's existing code paths, so
     * closing the active tab lands on a neighbouring session and closing the
     * last session keeps the app's existing finish behaviour.
     */
    private void closeSession(final TerminalSession session) {
        if (session.isRunning()) {
            session.finishIfRunning();
            // Let the process actually die before the removal bookkeeping runs.
            mTabsContainer.postDelayed(() -> finishCloseSession(session), 350);
        } else {
            finishCloseSession(session);
        }
    }

    private void finishCloseSession(TerminalSession session) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;
        if (service.getIndexOfSession(session) < 0) {
            // Already removed by the session-exit machinery.
            refreshTabs();
            return;
        }
        if (session.isRunning()) {
            // Kill did not take effect yet; leave the tab in place.
            refreshTabs();
            return;
        }
        if (session == mActivity.getCurrentSession()) {
            mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
        } else {
            service.removeTermuxSession(session);
        }
        refreshTabs();
    }

    // region Drag to rearrange

    private boolean startDrag(View tabView, TerminalSession session) {
        if (mOrderedSessions.size() < 2) return false;
        mDragView = tabView;
        mDragSession = session;
        mDragStartRawX = mDragDownRawX;
        mDragActive = true;
        if (mTabsScrollView != null)
            mTabsScrollView.requestDisallowInterceptTouchEvent(true);
        mTabsContainer.bringChildToFront(tabView);
        tabView.setAlpha(0.85f);
        return true;
    }

    private boolean onTabTouch(View tabView, TerminalSession session, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDragDownRawX = event.getRawX();
                return false;
            case MotionEvent.ACTION_MOVE:
                if (mDragActive && tabView == mDragView) {
                    tabView.setTranslationX(event.getRawX() - mDragStartRawX);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDragActive && tabView == mDragView) {
                    finishDrag(tabView);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private void finishDrag(View tabView) {
        int draggedCenter = tabView.getLeft() + (int) tabView.getTranslationX() + tabView.getWidth() / 2;

        // Insertion index among the other tabs: number of other tabs whose
        // center lies to the left of the dragged tab's center.
        int targetIndex = 0;
        for (int i = 0; i < mTabsContainer.getChildCount(); i++) {
            View child = mTabsContainer.getChildAt(i);
            if (child == tabView) continue;
            int childCenter = child.getLeft() + child.getWidth() / 2;
            if (childCenter < draggedCenter) targetIndex++;
        }

        mOrderedSessions.remove(mDragSession);
        if (targetIndex > mOrderedSessions.size()) targetIndex = mOrderedSessions.size();
        if (targetIndex < 0) targetIndex = 0;
        mOrderedSessions.add(targetIndex, mDragSession);
        saveOrder();

        tabView.setTranslationX(0);
        tabView.setAlpha(1f);
        if (mTabsScrollView != null)
            mTabsScrollView.requestDisallowInterceptTouchEvent(false);
        mDragActive = false;
        mDragView = null;
        mDragSession = null;
        rebuildTabViews();
    }

    // endregion

    private int dp(int value) {
        return Math.round(value * mActivity.getResources().getDisplayMetrics().density);
    }

}
