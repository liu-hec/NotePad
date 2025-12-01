## 记事本应用

基于谷歌官方NotePad示例构建的高级Android记事本应用程序。该应用程序提供了完整的笔记功能，包括分类、主题、搜索功能和可自定义的笔记外观。

## 功能特性

对于功能的实现，我们通过项目的结构来划分类和功能的对应情况。

<img width="321" height="286" alt="image" src="https://github.com/user-attachments/assets/670acd43-bea1-45d6-a954-9cf20de998d3" />


​    NoteEditor主要涉及note编辑功能，NotePad契约类，主要涉及数据层操作的uri约定情况。 NotePadProvider涉及数据内容的传递，使得不同程序之间共享交互数据， NoteList类主要使用ContentResolver来调用Provider的方法并显示主界面的情况，其余的类涉及小组件的具体功能实现，如title的修改或者主题的改变等等。



### 1.**增删改查等基本功能**：

- 创建、读取、更新和删除笔记
- 自动保存笔记内容
- 时间戳跟踪笔记修改时间

####  查询note

首先我们肯定要先展示主界面的，

```
// 查询所有笔记数据
Cursor cursor = getContentResolver().query(
    getIntent().getData(),
    PROJECTION,
    null,
    null,
    NotePad.Notes.DEFAULT_SORT_ORDER
);
//........................
// 创建适配器显示笔记列表
adapter = new SimpleCursorAdapter(
    this,
    R.layout.noteslist_item,
    cursor,
    dataColumns,
    viewIDs
);
```

NoteList通过Resolver来调用Provider的方法和数据库交互，并通过 SimpleCursorAdapter 将数据显示在列表中。

其中，getIntent().getData()使用 intent 中传递的 uri，Provider动态接收uri。

<img width="568" height="509" alt="image" src="https://github.com/user-attachments/assets/a73178f3-4de3-447b-891c-d11860e5e980" />




#### 修改note

修改note主要在NoteEditor类实现，编辑完成后修改保存note

```
@Override
    protected void onPause() {
        super.onPause();
        saveNote();
    }
 private void saveNote() {
        if (mCursor != null) {

            // Get the current note text.
            String text = mText.getText().toString();
            int length = text.length();

            // If the Activity is in the midst of finishing and there is no text, sets the
            // result to CANCELED, as if the user had pressed the back button.
            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                return;
            }
            
            // Get the current type 获取类别
            String type = (String) mTypeSpinner.getSelectedItem();
            if (type != null && type.isEmpty()) {
                type = null;
            }
            
            // Get the current color 获取颜色
            String color = COLORS[mColorSpinner.getSelectedItemPosition()];

            // Creates a map to contain the new values for the columns
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

            
            // Update type and color
            values.put(NotePad.Notes.COLUMN_NAME_TYPE, type);
            values.put(NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR, color);

            // This puts the desired notes text into the map.
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
            //修改note内容
            getContentResolver().update(
                    mUri,    // The URI for the record to update.
                    values,  // The map of column names and new values to apply to them.
                    null,    // No selection criteria are used, so no where columns are necessary.
                    null     // No where columns are used, so no where arguments are necessary.
                );
        }
    }    
```

修改前的内容


<img width="534" height="620" alt="image" src="https://github.com/user-attachments/assets/b236c120-dce8-4009-a4b3-ec5d0267193f" />

修改后


<img width="542" height="484" alt="image" src="https://github.com/user-attachments/assets/105c1cd9-57c9-4ca2-94b4-4d9fb42a1ac1" />

同时时间紧随修改

<img width="286" height="71" alt="image" src="https://github.com/user-attachments/assets/744944fd-ccd0-4cd6-9301-4650fb2d4f82" />




#### 新增note

```
 ImageView addButton = findViewById(R.id.add_note);
        addButton.setOnClickListener(v -> {
            // 跳转到NoteEditor进行新建笔记
            Intent intent = new Intent(Intent.ACTION_INSERT, NotePad.Notes.CONTENT_URI);
            startActivity(intent);
        });
```

点击绿色添加按钮跳转到编辑页面，在noteEditor中根据传入的操作不同来进行不同的操作

```
if (Intent.ACTION_EDIT.equals(action)) {//编辑动作

    // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
    mState = STATE_EDIT;
    mUri = intent.getData();

    // For an insert or paste action:
} else if (Intent.ACTION_INSERT.equals(action) //插入动作
        || Intent.ACTION_PASTE.equals(action)) { //复制动作

    // Sets the Activity state to INSERT, gets the general note URI, and inserts an
    // empty record in the provider
    mState = STATE_INSERT;
    mUri = getContentResolver().insert(intent.getData(), null);//前面list类传递对应的uri 这里getData接收 执行插入指令
    if (mUri == null) {
    //......
    }
    setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

}
```

在Provider执行插入操作

```
long rowId = db.insert(
    NotePad.Notes.TABLE_NAME,        // The table to insert into.
    NotePad.Notes.COLUMN_NAME_NOTE,  // A hack, SQLite sets this column value to null
                                     // if values is empty.
    values                           // A map of column names, and the values to insert
                                     // into the columns.
);
```

这里插入的note内容为null,因此如果你创建note但是没有输入内容（无法触发editor动作）不会创建新的note.即有数据输入才创建新的note，避免存储空间浪费

<img width="548" height="120" alt="image" src="https://github.com/user-attachments/assets/244b2ed2-e965-4e9a-811b-c283b7cf86a0" />

点击新增note 如果有内容即可保存note.


#### 删除note

删除note，根据长按显示菜单布局，再通过菜单的选项来进行删除操作。

显示菜单：

```
  private void showNotePopupMenu(View anchor, long noteId) {
        // 1. 初始化PopupMenu并加载菜单布局
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.inflate(R.menu.list_context_menu); // 加载原context菜单的布局文件

        // 2. 强制菜单向下展开（避免向上挤压）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setGravity(Gravity.BOTTOM | Gravity.START);
        // 3. 处理菜单项点击事件
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), noteId);
                int itemId = item.getItemId();

                if (itemId == R.id.context_open) {
                    startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                    return true; //处理打开note
                } else if (itemId == R.id.context_copy) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newUri(
                            getContentResolver(), "Note", noteUri));
                    return true; //处理copy note
                } else if (itemId == R.id.context_delete) {
                    getContentResolver().delete(noteUri, null, null);
                    return true;
                } //处理删除note
                return false;
            }
        });
        // 4. 显示PopupMenu
        popupMenu.show();
    }
```

<img width="576" height="424" alt="image" src="https://github.com/user-attachments/assets/593be6e5-de6e-4511-ba90-c43c220162cf" />


长按note 弹出选择 选择delete


<img width="572" height="403" alt="image" src="https://github.com/user-attachments/assets/5fc40e9c-47b2-4d09-b780-38b0f680bed6" />


可以发现New Note被删除。



#### 修改标题

我们创建自定义的修改title的视图。

```
actionBar.setDisplayShowCustomEnabled(true); // 启用自定义标题视图
```

修改title 保持文本的颜色不变。同时我们设定，如果是新建的note且没有设定title,默认title为New Note

```java

  // 如果是新创建的笔记，标题为"New note"
            if(mState == STATE_INSERT){
                title = "New Note";
            }

            mCurrentTitle = title;//全局的title变量
ActionBar actionBar = getActionBar();
        if (actionBar != null && actionBar.getCustomView() instanceof TextView) {
            TextView titleView = (TextView) actionBar.getCustomView();
            titleView.setText(text); // 仅更新文本，颜色保持灰色不变
        }
```


在自定义的修改title的组件，我们点击ok后，调用TitleEditor的方法

```
Button okButton = (Button) findViewById(R.id.ok);
okButton.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        onClickOk(v);
    }
});
```



同时我们退出编辑页面，调用saveNote来修改数据库的对应字段。同理和updateNote一致的情况。

```
values.put(NotePad.Notes.COLUMN_NAME_TITLE, mText.getText().toString());
getContentResolver().update(
    mUri,    // The URI for the note to update.
    values,  // The values map containing the columns to update and the values to use.
    null,    // No selection criteria is used, so no "where" columns are needed.
    null     // No "where" columns are used, so no "where" values are needed.
);
```

退到主界面 我们可以发现
<img width="536" height="114" alt="image" src="https://github.com/user-attachments/assets/3fae8ecc-ff15-4ed2-9915-3c9966b6a8fb" />







### 2.基本要求实现

#### 时间戳显示

在NoteList类中，onCreate方法内初始化页面的情况，

```
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE } ;
          int[] viewIDs = { android.R.id.text1, R.id.text2 };//notelist_item的基本结构
```

dataColumns表示要展示的note数据项，同时准备要绑定到组件的id

创建适配器，连接Cursor和ListView 

```java
  // Creates the backing adapter for the ListView.
        adapter = new SimpleCursorAdapter(
                      this,                             // The Context for the ListView  上下文
                      R.layout.noteslist_item,          // Points to the XML for a list item 基本note结构
                      cursor,                           // The cursor to get items from  数据
                      dataColumns,                      //对应的数据组名 
                      viewIDs                           //对应组件的ids
              );
```

这样我们绑定页面显示和查询出来的数据情况，但是无法满足实际的显示情况，例如时间不配对，显示的时间非Asia/Shanghai时区。

因此我们`setViewBinder()` 重写部分绑定规则

```
 @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.text2) {
                    long date = cursor.getLong(columnIndex);
                    ((android.widget.TextView) view).setText(formatDate(date)); //转换时间显示
                    return true;
                }
```

同时时间转换方法formatDate，将zone设为亚洲上海时区，这样我们保存note，显示的最后修改的时间就和我们所在区域的时间一致。

```
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//时间的展示形式
private String formatDate(long timestamp) {
    if (timestamp == 0) {
        return "";
    }
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));//
    return DATE_FORMAT.format(new Date(timestamp));
}
```

当然，实际情况可能要通过用户的gps等定位位置来设置时区，而非固定的时区设定。同时使用固定的时间展示形式。



<img width="294" height="73" alt="image" src="https://github.com/user-attachments/assets/a4c91eb9-8fe8-4da2-9321-39395bebc3fe" />




#### 搜索功能

对于搜索，找到对于的搜索按钮，searchItem,并为其创建搜索输入控件，为控件创建监听。

监听的逻辑：

- 搜索框输入时（`onQueryTextChange`）：实时响应，空文本则显示全部笔记；
- 提交搜索时（`onQueryTextSubmit`）：执行最终搜索（避免输入过程中频繁查询）；

`SearchView` 是 Android 提供的现成搜索控件，自带输入框、清除按钮等，无需自定义。

```java
 MenuItem searchItem = menu.findItem(R.id.menu_search);
    // 3. 获取搜索项的“动作视图”—— SearchView（Android 自带的搜索输入控件）
    searchView = (SearchView) searchItem.getActionView();
    
    if (searchView != null) { // 防止空指针（若菜单中没有搜索项，searchView 为 null）
        // 4. 给 SearchView 设置“查询文本监听器”（监听输入变化、提交操作）
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // 回调1：用户点击“搜索按钮”或按回车（提交搜索时触发）
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query); // 执行搜索逻辑（核心方法）
                return true; // 返回 true 表示“已处理该事件”，不再向下传递
            }

            // 回调2：搜索框文本发生变化时触发（实时输入时）
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    performSearch(""); // 文本为空时，查询“所有数据”
                }
                return true; // 表示已处理
            }
        });
```
点击搜索按钮 弹出搜索框


<img width="527" height="168" alt="image" src="https://github.com/user-attachments/assets/defd8cc5-ab0d-4d18-bd5d-718a1ff87326" />



搜索方法 performSearch

```java
private void performSearch(String query) {
   // 检查Activity是否处于有效状态
        if (isFinishing() || isDestroyed()) {
            return;
        }

        String selection = null;
        String[] selectionArgs = null;
```

进行查询的初始化，初始化sql语句的where条件判断和预防sql注入的占位符

查询要求：根据note内容和title进行模糊查询，因此使用or来连接查询语句和 “ like ”进行模糊查询。

```
 if (!query.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + 
                        NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        }
```

查询显示，查询数据库并更新列表数据显示情况。查询的数据直接调用changeCursor进行列表数据显示更新。

```
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
```
<img width="595" height="1112" alt="image" src="https://github.com/user-attachments/assets/f0ff3f4c-9516-4cee-844e-281939c8f400" />

搜索含有“1”的note
搜索“Dan”的note，


<img width="563" height="719" alt="image" src="https://github.com/user-attachments/assets/be7e4386-2487-442d-9167-ca9d9521d675" />

其中，note-1存在对应的内容。不区分大小写。


<img width="535" height="436" alt="image" src="https://github.com/user-attachments/assets/fea06096-3d5a-494b-ad63-bea2aad22297" />





### 3.高级组织管理

#### 笔记分类

对于note的分类，在notelist类，我们添加汉堡菜单用来显示所有的分类，包含固定的分类，（个人、工作、想法、任务、其他）

自定义的actionbar，绑定汉按钮。

```
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
```

<img width="1059" height="995" alt="image" src="https://github.com/user-attachments/assets/8e9f2026-68b6-42b3-948b-55e1c7376163" />


点击按钮，执行showCategoriesPopupMenu方法，显示所有类别，并设定监听。

```
   popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
```

同时根据不同的选项执行不同的sql查询。默认空查询所有note,否则根据点击的类别来使用 filterByCategory进行查询，同理查询要更新页面（和搜索一样的逻辑实现）

```
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
```


<img width="550" height="461" alt="image" src="https://github.com/user-attachments/assets/4eee99ec-2a60-47d9-91ad-cbe5c1581cfc" />


Personal:


<img width="568" height="231" alt="image" src="https://github.com/user-attachments/assets/13605584-6350-49b9-b714-5929f50b0441" />
All note显示所有的note.



当进入编辑页面选择不同的分类时，即可修改note的属性  Notes.COLUMN_NAME_TYPE。

在NoteEditor类，

初始化类别：

```
 private static final String[] CATEGORIES = {"All", "Personal", "Work", "Ideas", "Tasks", "Other"};
```

这里我们的编辑页面的基本结构如下：


  <img width="517" height="109" alt="image" src="https://github.com/user-attachments/assets/e9b11289-6a60-4cae-b655-cd8ae46d3152" />




在NoteEditor类中初始化方法setupSpinners，供使用者下拉选择框选择。为了避免后期主题变为灰色时造成的颜色的冲突，我们统一颜色为灰色。

```
private void setupSpinners() {
    // Setup type spinner
    ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_item, CATEGORIES) {
        //  控制“选中项”的字体颜色
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view;
            // 设置选中项字体颜色（比如和标题一致的灰色）
            textView.setTextColor(Color.GRAY);
            return view;
        }

        //  控制“下拉项”的字体颜色
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            TextView textView = (TextView) view;
            // 设置下拉项字体颜色
            textView.setTextColor(Color.GRAY);
            return view;
        }
    };
    typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mTypeSpinner.setAdapter(typeAdapter);

}


<img width="301" height="478" alt="image" src="https://github.com/user-attachments/assets/57f65007-d23c-41c3-9708-f1766783af79" />


重写当前选中getView() 和 下拉项getDropDownView()，统一修改为灰色显示。

随后点击保存或者回退到主界面，触发保存note方法saveNote，部分代码如下



<img width="553" height="590" alt="image" src="https://github.com/user-attachments/assets/ea50ead6-b5d8-4af9-8bb2-274411db5f5e" />


```
  // Get the current type
            String type = (String) mTypeSpinner.getSelectedItem();
            if (type != null && type.isEmpty()) {
                type = null;
            }
            
            // Get the current color
            String color = COLORS[mColorSpinner.getSelectedItemPosition()];
            
             getContentResolver().update(
                    mUri,    // The URI for the record to update.
                    values,  // The map of column names and new values to apply to them.
                    null,    // No selection criteria are used, so no where columns are necessary.
                    null     // No where columns are used, so no where arguments are necessary.
                );
```






#### 彩色笔记

```
提供6种预设背景色

对于NoteList类，我们提供多个浅色的颜色供使用者选择，插入note时默认没有颜色，为白色。当进入note的编辑页面时，可以自主选择需要的颜色。

    // Setup color spinner
    ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_item, COLOR_NAMES) {
        // ① 控制“选中项”的字体颜色
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view;
            // 设置选中项字体颜色（灰色）
            textView.setTextColor(Color.GRAY);
            return view;
        }
    
        // 控制“下拉项”的字体颜色
        @Override
        public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            TextView textView = (TextView) view;
            // 设置下拉项字体颜色（灰色）
            textView.setTextColor(Color.GRAY);
            // 原有背景色逻辑保留
                view.setBackgroundColor(Color.parseColor(COLORS[position]));
            return view;
        }
    };
    colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mColorSpinner.setAdapter(colorAdapter);

同上，点击保存或者回退到主界面，触发保存note方法saveNote，进行修改操作。


```

```

截图测试颜色情况：

  <img width="577" height="472" alt="image" src="https://github.com/user-attachments/assets/1e7a08ee-1ce8-4cce-a338-073bcec03d0e" /> 

  
修改note-1 为粉色


  <img width="598" height="124" alt="image" src="https://github.com/user-attachments/assets/11c45013-6980-4f15-a16c-abbe570d693a" />


```





#### 快捷键按钮

我们设想在主界面创建一个可以替代新增note按钮的美观的按钮。

首先绘制一个图标，使用`<layer-list>`：来实现图层的重叠，创建“十”字型的图形。

图



其次，将图标绑定到主界面的中，创建容器组件。这样我们可以根据id找到对应的容器组件,为其适配图标。

```
<!-- 列表视图（原列表内容） -->
<ListView
    android:id="@android:id/list"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_alignParentTop="true" />

<ImageView
    android:id="@+id/add_note"
    android:layout_width="60dp"
    android:layout_height="60dp"
    android:layout_alignParentEnd="true"
    android:layout_alignParentBottom="true"
    android:layout_marginStart="16dp"
    android:layout_marginTop="16dp"
    android:layout_marginEnd="60dp"
    android:layout_marginBottom="120dp"
    android:contentDescription="Single green circle" />
```

ListView是列表主界面，下面单独的圆圈图标

```


  <img width="553" height="1041" alt="image" src="https://github.com/user-attachments/assets/abf81dc6-5910-48f4-9a62-a7a893e50e16" />



```

最后，将其创建监听方法，点击进入编辑页面即可。

```
 ImageView addButton = findViewById(R.id.add_note);
addButton.setOnClickListener(v -> {
    // 跳转到NoteEditor进行新建笔记
    Intent intent = new Intent(Intent.ACTION_INSERT, NotePad.Notes.CONTENT_URI);
    startActivity(intent);
});
```


点击进入编辑页面
```


<img width="518" height="874" alt="image" src="https://github.com/user-attachments/assets/231c8830-97de-4d5e-be46-d80e83a38780" />

```


#### 输入美化

我们对于note主体内容的输入显示进行优化，增加适配主题的横线。

```
public LinedEditText(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
    mRect = new Rect();
    mPaint = new Paint();
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setColor(getLineColor(context)); // 根据主题设置线条颜色

}
```



```
默认情况


 <img width="566" height="444" alt="image" src="https://github.com/user-attachments/assets/f085fe0f-9afa-4be4-bbbb-537e586f90b8" /> 


黑色模式


<img width="524" height="521" alt="image" src="https://github.com/user-attachments/assets/84bc4937-ada8-472b-9caf-04633a5391c7" />


```



同时优化输入的情况，安装中文输入法，可以进行中文的输入。





#### 双主题支持

在ThemeManager统一管理主题，全局控制应用的 “浅色 / 深色主题” 切换、持久化保存主题设置、并提供统一的主题应用接口。

依赖：使用 `SharedPreferences` 持久化主题设置（确保重启应用后仍保留上次选择的主题）。



定义主题常量

```java
// 主题常量：标记两种主题类型（浅色/深色）
public static final int THEME_LIGHT = 0;
public static final int THEME_DARK = 1;
```

给activity应用主题，这样我们直接在NoteList调用此方法就可切换主题。ThemeManager.applyTheme(this);

`applyTheme(Activity activity)` —— 给 Activity 应用主题

```
public static void applyTheme(Activity activity) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    int theme = prefs.getInt("theme", THEME_LIGHT); // 默认使用浅色主题

    switch (theme) {
        case THEME_DARK:
            activity.setTheme(R.style.NotePadTheme_Dark);
            break;
        case THEME_LIGHT:
        default:
            activity.setTheme(R.style.NotePadTheme_Light);
            break;
    }
}
```

核心为使用SharedPreferences持久化保存主题内容，同时将theme.xml定义的主题样式应用。



`setTheme(Activity activity, int theme)` —— 保存主题设置

解决使用则点击按钮实现切换主题的功能。

```
public static void setTheme(Activity activity, int theme) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    prefs.edit().putInt("theme", theme).apply();
}


```



`getCurrentTheme(Activity activity)` —— 获取当前主题

```
public static int getCurrentTheme(Activity activity) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    return prefs.getInt("theme", THEME_LIGHT);
}
```



查看定义的主题样式

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Light Theme -->
    <style name="NotePadTheme.Light" parent="@android:style/Theme.Holo.Light">
        <item name="android:listPopupWindowStyle">@style/PopupMenuStyle</item>
        <item name="android:windowBackground">@color/background_color</item>
        <item name="android:textColorPrimary">@color/text_primary</item>
        <item name="android:textColorSecondary">@color/text_secondary</item>
    </style>
    
    <!-- Dark Theme -->
    <style name="NotePadTheme.Dark" parent="@android:style/Theme.Holo">
        <item name="android:listPopupWindowStyle">@style/PopupMenuStyle</item>
        <item name="android:windowBackground">@color/dark_background</item>
        <item name="android:textColorPrimary">@color/dark_text_primary</item>
        <item name="android:textColorSecondary">@color/dark_text_secondary</item>
    </style>
</resources>
```

我们定义了不同的样式，使用不同的颜色搭配。因此我们要对组件进行适配颜色显示。

实际上，我们实现不同主题的颜色表现样式，可以通过两种不同的方式。



##### 属性引用

例如，在编辑note的页面的主体内容的字体显示上，我们要日间主体为黑色字体，夜间模式为白色字体，否则字体可能无法正常显示。

```
android:textColor="?android:attr/textColorPrimary"
```

因此我们使用属性引用，动态绑定textColorPrimary，同时在theme,xml中，我们定义不同主体的样式，在不同的主题显示不同的页面



##### 相似样式创建

例如，对于某些想要自定义的组件样式，在主题数量少的情况下，我们可以定义一个默认组件和dark组件。

以快捷键为例。创建两个相似的icon,在不同的主题下进行设置即可。
白色模式
<img width="1103" height="1068" alt="image" src="https://github.com/user-attachments/assets/534bb8d0-1360-41b0-b0f2-22ea8ff18716" />


黑色模式
<img width="1069" height="995" alt="image" src="https://github.com/user-attachments/assets/139e4114-5b6a-4691-be18-053adf1228dd" />



这样我们在NoteList主界面显示的中根据目前的主题来进行使用不同的组件即可。

```
ImageView addButton = findViewById(R.id.add_note);
//如果是white模式 选择icon_add_note_fab_white 否则icon_add_note_fab
int drawableId;
if (ThemeManager.getCurrentTheme(this) == ThemeManager.THEME_DARK) {
    drawableId = R.drawable.ic_add_note_fab;
} else {
    drawableId = R.drawable.ic_add_note_fab_white;
}
addButton.setImageDrawable(AppCompatResources.getDrawable(this, drawableId));
```



  <img width="560" height="305" alt="image" src="https://github.com/user-attachments/assets/e0307b55-34f1-4e1f-9fae-7e29b877aeab" />



##### 日间模式

默认情况下，就是日间模式。

在NoteList中，实现menu的切换操作。通过调用ThemeManager的方法来实现主题的切换。

```
if (itemId == R.id.menu_theme_white) {
    // 设置浅色主题
    ThemeManager.setTheme(this, ThemeManager.THEME_LIGHT);
    recreate(); // 重新创建Activity以应用新主题
    return true;
} else if (itemId == R.id.menu_theme_gray) {
    // 设置深色主题
    ThemeManager.setTheme(this, ThemeManager.THEME_DARK);
    recreate(); // 重新创建Activity以应用新主题
    return true;
}
```


<img width="555" height="1006" alt="image" src="https://github.com/user-attachments/assets/74e46e1a-9ff4-4440-973c-dc1a21de119e" />



##### 夜间模式

对于夜间模式，我们调整对于的显示颜色即可，在theme中定义两种不同的颜色即可。

因此对于一些组件的颜色显示，我们仅需使用引用属性来切换其在不同主题下的表现情况即可。因此无需特别关注主题的变化带来的其他重大改动。




<img width="545" height="1086" alt="image" src="https://github.com/user-attachments/assets/29b813d5-9db8-4f46-aeb6-c864c1c01cc4" />



## 其他

### 系统架构

本应用遵循标准的Android架构模式，采用Content Provider进行数据管理：

```
┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   Activities    │    │   ContentProvider│    │   SQLite DB      │
├─────────────────┤    ├──────────────────┤    ├──────────────────┤
│  NotesList      │◄──►│  NotePadProvider │◄──►│  Notes Table     │
│  NoteEditor     │    │                  │    │                  │
│  TitleEditor    │    │                  │    │ Columns:         │
└─────────────────┘    └──────────────────┘    │ _id              │
                                                │ title            │
                                                │ note             │
                                                │ created_date     │
                                                │ modification_date│
                                                │ type             │
                                                │ background_color │
                                                └──────────────────┘
```

### 数据库表结构

笔记存储在由[NotePadProvider](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePadProvider.java#L50-L50)管理的SQLite数据库中：

| 字段 | 类型 | 描述 |
|--------|------|-------------|
| [_id](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePad.java#L90-L91) | INTEGER | 主键 |
| [title](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePad.java#L93-L94) | TEXT | 笔记标题 |
| [note](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePad.java#L96-L97) | TEXT | 笔记内容 |
| [created_date](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePad.java#L99-L100) | INTEGER | 创建时间戳 |
| [modification_date](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePad.java#L102-L103) | INTEGER | 最后修改时间戳 |
| [type](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePad.java#L108-L109) | TEXT | 笔记分类 |
| [background_color](file:///C:/Users/86152/AndroidStudioProjects/NotePad_origin/app/src/main/java/com/example/android/notepad/NotePad.java#L111-L113) | TEXT | 背景颜色十六进制码 |





## 总结

  本应用以官方示例为基础，在保留原生应用轻量、流畅优势的同时，通过功能扩展与细节打磨，满足了用户对笔记管理的进阶需求。技术上遵循 Android 原生开发规范，架构清晰、代码可维护，既适合作为 Android 初学者学习 ContentProvider、主题适配、自定义 View 等核心技术的案例，也具备一定的实际使用价值。
