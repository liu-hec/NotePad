/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.SearchView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2
            NotePad.Notes.COLUMN_NAME_TYPE, // 3
            NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR // 4
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2;
    private static final int COLUMN_INDEX_TYPE = 3;
    private static final int COLUMN_INDEX_BACKGROUND_COLOR = 4;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private SimpleCursorAdapter adapter;
    private SearchView searchView;
    private String currentFilterType = null;

    // 定义分类菜单项的ID常量
    private static final int MENU_ITEM_ALL = 1;
    private static final int MENU_ITEM_PERSONAL = 2;
    private static final int MENU_ITEM_WORK = 3;
    private static final int MENU_ITEM_IDEAS = 4;
    private static final int MENU_ITEM_TASKS = 5;
    private static final int MENU_ITEM_OTHER = 6;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用主题
        ThemeManager.applyTheme(this);
        
        super.onCreate(savedInstanceState);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // 设置自定义ActionBar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);

            // 创建自定义视图
            View customView = getLayoutInflater().inflate(R.layout.actionbar_custom_view, null);
            actionBar.setCustomView(customView);

            // 查找汉堡菜单按钮并设置点击事件
            ImageButton hamburgerButton = (ImageButton) customView.findViewById(R.id.actionbar_menu);
            if (hamburgerButton != null) {
                hamburgerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCategoriesPopupMenu(v);
                    }
                });
            }
        }

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showNotePopupMenu(view, id); // 触发自定义PopupMenu
                return true; // 消费事件，避免系统默认行为
            }
        });

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );//  TODO managedQuery方法而导致的闪退问题 导致搜索框 没有进行搜索进入编辑页面退出后直接闪退
        //

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE } ;

        // The view IDs that will display the cursor columns, initialized to the TextView in
        // noteslist_item.xml
        int[] viewIDs = { android.R.id.text1, R.id.text2 };

        // Creates the backing adapter for the ListView.
        adapter = new SimpleCursorAdapter(
                      this,                             // The Context for the ListView
                      R.layout.noteslist_item,          // Points to the XML for a list item
                      cursor,                           // The cursor to get items from
                      dataColumns,
                      viewIDs
              );

        // Add date formatting
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.text2) {
                    long date = cursor.getLong(columnIndex);
                    ((android.widget.TextView) view).setText(formatDate(date));
                    return true;
                } else if (view.getId() == android.R.id.text1) {
                    // Apply background color if available
                    String backgroundColor = cursor.getString(COLUMN_INDEX_BACKGROUND_COLOR);
                    if (backgroundColor != null && !backgroundColor.isEmpty()) {
                        try {
                            view.setBackgroundColor(Color.parseColor(backgroundColor));
                        } catch (IllegalArgumentException e) {
                            // Invalid color, use default
                            view.setBackgroundColor(Color.TRANSPARENT);
                        }
                    } else {
                        view.setBackgroundColor(Color.TRANSPARENT);
                    }
                    return false;
                }
                return false;
            }
        });

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);
    }
    
    /**
     * Format date for display in China timezone
     */
    private String formatDate(long timestamp) {
        if (timestamp == 0) {
            return "";
        }
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu_extended, menu);


        // Add search functionality
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    performSearch(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText.isEmpty()) {
                        performSearch("");
                    }
                    return true;
                }
            });
        }

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    private void performSearch(String query) {

        // 检查Activity是否处于有效状态
        if (isFinishing() || isDestroyed()) {
            return;
        }

        String selection = null;
        String[] selectionArgs = null;
        
        if (!query.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + 
                        NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        }
        
        // Add type filter if we have one
        if (currentFilterType != null && !currentFilterType.isEmpty()) {
            String typeSelection = NotePad.Notes.COLUMN_NAME_TYPE + " = ?";
            if (selection != null) {
                selection = "(" + selection + ") AND " + typeSelection;
                String[] newArgs = new String[selectionArgs.length + 1];
                System.arraycopy(selectionArgs, 0, newArgs, 0, selectionArgs.length);
                newArgs[selectionArgs.length] = currentFilterType;
                selectionArgs = newArgs;
            } else {
                selection = typeSelection;
                selectionArgs = new String[]{currentFilterType};
            }
        }

        // 使用 getContentResolver().query() 替代 managedQuery
        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 安全地更换 adapter 的 cursor
        if (adapter != null && cursor != null) {
            adapter.changeCursor(cursor);
        }
    }

    private void filterByCategory(String category) {
        currentFilterType = category;
        String selection = null;
        String[] selectionArgs = null;
        
        if (category != null && !category.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_TYPE + " = ?";
            selectionArgs = new String[]{category};
        }

        // 使用 getContentResolver().query() 替代 managedQuery
        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 安全地更换 adapter 的 cursor
        if (adapter != null && cursor != null) {
            adapter.changeCursor(cursor);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理分类菜单项
        switch (item.getItemId()) {
            case MENU_ITEM_ALL:
                currentFilterType = null;
                performSearch("");
                return true;
            case MENU_ITEM_PERSONAL:
                filterByCategory("Personal");
                return true;
            case MENU_ITEM_WORK:
                filterByCategory("Work");
                return true;
            case MENU_ITEM_IDEAS:
                filterByCategory("Ideas");
                return true;
            case MENU_ITEM_TASKS:
                filterByCategory("Tasks");
                return true;
            case MENU_ITEM_OTHER:
                filterByCategory("Other");
                return true;
        }
        
        // 处理其他菜单项
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (itemId == R.id.menu_paste) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (itemId == R.id.menu_theme_white) {
            // 设置浅色主题
            ThemeManager.setTheme(this, ThemeManager.THEME_LIGHT);
            recreate(); // 重新创建Activity以应用新主题
            return true;
        } else if (itemId == R.id.menu_theme_gray) {
            // 设置深色主题
            ThemeManager.setTheme(this, ThemeManager.THEME_DARK);
            recreate(); // 重新创建Activity以应用新主题
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                Menu.NONE,                  // A unique item ID is not required.
                Menu.NONE,                  // The alternatives don't need to be in order.
                null,                       // The caller's name is not excluded from the group.
                specifics,                  // These specific options must appear first.
                intent,                     // These Intent objects map to the options in specifics.
                Menu.NONE,                  // No flags are required.
                items                       // The menu items generated from the specifics-to-
                                            // Intents mapping
            );
                // If the Edit menu item exists, adds shortcuts for it.
                if (items[0] != null) {

                    // Sets the Edit menu item shortcut to numeric "1", letter "e"
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // If the list is empty, removes any existing alternative actions from the menu
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    //点击note进入编辑页面
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

    private void showNotePopupMenu(View anchor, long noteId) {
        // 1. 初始化PopupMenu并加载菜单布局
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.inflate(R.menu.list_context_menu); // 加载原context菜单的布局文件

        // 2. 强制菜单向下展开（避免向上挤压）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setGravity(Gravity.BOTTOM | Gravity.START);
        } else {
            // 低版本兼容：通过反射强制设置显示方向（可选）
            try {
                Field field = popupMenu.getClass().getDeclaredField("mPopup");
                field.setAccessible(true);
                Object popup = field.get(popupMenu);
                Method setGravity = popup.getClass().getMethod("setGravity", int.class);
                setGravity.invoke(popup, Gravity.BOTTOM | Gravity.START);
            } catch (Exception e) {
                Log.e(TAG, "设置PopupMenu方向失败", e);
            }
        }

        // 3. 处理菜单项点击事件
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), noteId);
                int itemId = item.getItemId();

                if (itemId == R.id.context_open) {
                    startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                    return true;
                } else if (itemId == R.id.context_copy) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newUri(
                            getContentResolver(), "Note", noteUri));
                    return true;
                } else if (itemId == R.id.context_delete) {
                    getContentResolver().delete(noteUri, null, null);
                    return true;
                }
                return false;
            }
        });

        // 4. 显示PopupMenu
        popupMenu.show();
    }

    private void showCategoriesPopupMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        
        // 手动添加菜单项，使用自定义ID而不是R.id
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, MENU_ITEM_ALL, Menu.NONE, "All Notes");
        menu.add(Menu.NONE, MENU_ITEM_PERSONAL, Menu.NONE, "Personal");
        menu.add(Menu.NONE, MENU_ITEM_WORK, Menu.NONE, "Work");
        menu.add(Menu.NONE, MENU_ITEM_IDEAS, Menu.NONE, "Ideas");
        menu.add(Menu.NONE, MENU_ITEM_TASKS, Menu.NONE, "Tasks");
        menu.add(Menu.NONE, MENU_ITEM_OTHER, Menu.NONE, "Other");
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        
        popupMenu.show();
    }
}