package com.sadinasib.notebook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.Toast;

import com.sadinasib.notebook.adapter.NotebookAdapter;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import static com.sadinasib.notebook.data.NotebookContract.NotebookEntry;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , LoaderManager.LoaderCallbacks<Cursor>
        , AdapterView.OnItemLongClickListener
        , SearchView.OnQueryTextListener, FilterQueryProvider {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INVENTORY_LOADER_ID = 35;

    private NotebookAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editIntent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(editIntent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ListView mListView = (ListView) findViewById(R.id.listView);
        View emptyView = findViewById(R.id.empty_view);
        mListView.setEmptyView(emptyView);
        mAdapter = new NotebookAdapter(this, null);
        mAdapter.setFilterQueryProvider(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                if (scrollState == 2 || scrollState == 1) {
                    fab.setVisibility(View.GONE);
                } else {
                    fab.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });

        getLoaderManager().initLoader(INVENTORY_LOADER_ID, null, this);
    }

    private void showPopup(final View view, final long id) {
        Log.i(TAG, "showPopup");
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_popup, popupMenu.getMenu());
        final Uri currentProductUri = ContentUris.withAppendedId(NotebookEntry.CONTENT_URI, id);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                switch (itemId) {
                    case R.id.action_popup_edit:
                        Intent editIntent = new Intent(MainActivity.this, EditorActivity.class);
                        editIntent.setData(currentProductUri);
                        startActivity(editIntent);
                        return true;
                    case R.id.action_popup_delete:
                        deleteSingleWord(currentProductUri);
                        return true;
                    case R.id.action_popup_share:
                        return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mAdapter.getFilter().filter(null);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add:
                Intent editIntent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(editIntent);
                return true;
            case R.id.action_delete_all:
                deleteAllWords();
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSingleWord(Uri uri) {
        Log.i(TAG, "deleteSingleWord");
        int rowId = getContentResolver().delete(uri, null, null);
        if (rowId == 0) {
            Log.e(TAG, "deleteSingleWord failed for uri " + uri.toString());
            Toast.makeText(this, R.string.main_activity_update_failed, Toast.LENGTH_SHORT).show();
        } else {

            Toast.makeText(this, R.string.main_activity_update_succ, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteAllWords() {
        Log.i(TAG, "deleteAllProducts");
        @SuppressLint("Recycle")
        Cursor cursor = getContentResolver().query(NotebookEntry.CONTENT_URI, new String[]{NotebookEntry._ID}, null, null, null);
        if (cursor != null && cursor.getCount() == 0) {
            Toast.makeText(this, R.string.main_activity_list_already_empty, Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_all_words_dialog_title)
                    .setMessage(R.string.delete_all_words_dialog_msg)
                    .setPositiveButton(R.string.delete_all_words_dialog_positive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getContentResolver().delete(NotebookEntry.CONTENT_URI, null, null);
                        }
                    })
                    .setNegativeButton(R.string.delete_all_words_dialog_negative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_export:
                exportDbToExcel();
                break;
            case R.id.nav_import:
                startImportIntent();
                break;
            case R.id.nav_share:
                exportDbToExcel();
                break;
            case R.id.nav_send:
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void startImportIntent() {
        Log.i(TAG, "startImportIntent");
        Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.setType("application/vnd.ms-excel");
        try {
            startActivityForResult(fileIntent, 12);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Impossible to start import process", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: with request code " + requestCode);
        if (requestCode == 12 && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            HSSFWorkbook workbook = null;
            InputStream inputStream = null;
            if (data != null) {
                uri = data.getData();
                try {
                    inputStream = getContentResolver().openInputStream(uri);
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onActivityResult: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
                try {
                    workbook = new HSSFWorkbook(inputStream);
                } catch (IOException e) {
                    Toast.makeText(this, "File not supported. Please choose file with .xls format", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onActivityResult: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
                HSSFSheet sheet = workbook.getSheetAt(0);
                importExcelToDb(sheet);
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void importExcelToDb(HSSFSheet sheet) {
        HSSFRow row;
        Iterator<Row> iterator = sheet.rowIterator();
        while (iterator.hasNext()) {
            row = (HSSFRow) iterator.next();
            if (row.getCell(0) != null
                    && row.getCell(1) != null) {
                ContentValues contentValues = new ContentValues();
                String word = row.getCell(0).getStringCellValue();
                String trans = row.getCell(1).getStringCellValue();
                if (!isExist(word, trans)) {
                    contentValues.put(NotebookEntry.COLUMN_WORD, word);
                    contentValues.put(NotebookEntry.COLUMN_TRANSLATION, trans);
                    getContentResolver().insert(NotebookEntry.CONTENT_URI, contentValues);
                }
            }
        }
        Toast.makeText(this, "File imported", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i(TAG, "onCreateLoader");
        String[] projection = {
                NotebookEntry._ID,
                NotebookEntry.COLUMN_WORD,
                NotebookEntry.COLUMN_TRANSLATION};
        return new CursorLoader(this,
                NotebookEntry.CONTENT_URI,
                projection,
                null,
                null,
                NotebookEntry.SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.i(TAG, "onLoadFinished: ");
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(TAG, "onLoaderReset: ");
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        showPopup(view, id);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mAdapter.getFilter().filter(s);
        return false;
    }

    @Override
    public Cursor runQuery(CharSequence charSequence) {

        if (charSequence == null || TextUtils.isEmpty(charSequence)) {
            return getContentResolver().query(
                    NotebookEntry.CONTENT_URI,
                    new String[]{"*"},
                    null,
                    null,
                    NotebookEntry.SORT_ORDER
            );
        }

        String[] projection = {
                NotebookEntry._ID,
                NotebookEntry.COLUMN_WORD,
                NotebookEntry.COLUMN_TRANSLATION};
        String selection = NotebookEntry.COLUMN_WORD + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + String.valueOf(charSequence) + "%"};

        @SuppressLint("Recycle")
        Cursor cursor = getContentResolver().query(
                NotebookEntry.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
                null);
        return cursor;
    }

    private void exportDbToExcel() {
        final Cursor cursor = getContentResolver().query(
                NotebookEntry.CONTENT_URI,
                new String[]{NotebookEntry.COLUMN_WORD, NotebookEntry.COLUMN_TRANSLATION},
                null,
                null,
                NotebookEntry.SORT_ORDER
        );

        if (cursor == null || cursor.getCount() < 1) {
            Toast.makeText(this, "You have no words to export", Toast.LENGTH_SHORT).show();
            return;
        }

        File sd = new File(Environment.getExternalStorageDirectory() + "/Notebook");
        String csvFile = "NotebookData.xls";
        File directory = new File(sd.getAbsolutePath());
        if (!directory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
        try {
            File file = new File(directory, csvFile);
            WorkbookSettings wbSettings = new WorkbookSettings();
            wbSettings.setLocale(new Locale("en", "EN"));
            WritableWorkbook workbook;
            workbook = Workbook.createWorkbook(file, wbSettings);
            //Excel sheet name
            WritableSheet sheet = workbook.createSheet("words", 0);
            //Column and row
            sheet.addCell(new Label(0, 0, "Word"));
            sheet.addCell(new Label(1, 0, "Translation"));

            if (cursor.moveToFirst()) {
                do {
                    String word = cursor.getString(cursor.getColumnIndex(NotebookEntry.COLUMN_WORD));
                    String trans = cursor.getString(cursor.getColumnIndex(NotebookEntry.COLUMN_TRANSLATION));

                    int i = cursor.getPosition() + 1;
                    sheet.addCell(new Label(0, i, word));
                    sheet.addCell(new Label(1, i, trans));
                } while (cursor.moveToNext());
                //closing cursor
                cursor.close();
                workbook.write();
                workbook.close();
                Toast.makeText(getApplication(), "File exported successfully to " + directory.toString(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Exporting file failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean isExist(String word, String trans) {
        String[] projection = {NotebookEntry._ID};
        String selection = NotebookEntry.COLUMN_WORD + "=? OR " + NotebookEntry.COLUMN_TRANSLATION + " =?";
        String[] selectionArgs = new String[]{word, trans};
        @SuppressLint("Recycle")
        Cursor cursor = getContentResolver().query(
                NotebookEntry.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );
        return cursor != null && cursor.getCount() > 0;
    }
}
