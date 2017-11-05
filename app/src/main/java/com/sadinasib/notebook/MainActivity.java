package com.sadinasib.notebook;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.sadinasib.notebook.adapter.NotebookAdapter;
import com.sadinasib.notebook.data.NotebookContract;

import static com.sadinasib.notebook.data.NotebookContract.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INVENTORY_LOADER_ID = 35;

    private NotebookAdapter mAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
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

        mListView = (ListView) findViewById(R.id.listView);
        View emptyView = findViewById(R.id.empty_view);
        mListView.setEmptyView(emptyView);

        mAdapter = new NotebookAdapter(this, null);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent editIntent = new Intent(MainActivity.this, EditorActivity.class);
                Uri currentProductUri = ContentUris.withAppendedId(NotebookEntry.CONTENT_URI, id);
                editIntent.setData(currentProductUri);
                startActivity(editIntent);
                return true;
            }
        });
        getLoaderManager().initLoader(INVENTORY_LOADER_ID, null, this);
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
            case R.id.nav_share:
                break;
            case R.id.nav_send:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                null);
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
}
