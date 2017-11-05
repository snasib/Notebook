package com.sadinasib.notebook.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.sadinasib.notebook.R;

import static com.sadinasib.notebook.data.NotebookContract.NotebookEntry;

/**
 * Created by sadin on 05-Nov-17.
 */

public class NotebookAdapter extends CursorAdapter {
    private static final String TAG = NotebookAdapter.class.getSimpleName();

    public NotebookAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_items, viewGroup, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final long row_id = cursor.getLong(cursor.getColumnIndex(NotebookEntry._ID));

        TextView tvWord = (TextView) view.findViewById(R.id.list_items_text_word);
        TextView tvTrans = (TextView) view.findViewById(R.id.list_items_text_trans);

        String word = cursor.getString(cursor.getColumnIndexOrThrow(NotebookEntry.COLUMN_WORD));
        String trans = cursor.getString(cursor.getColumnIndexOrThrow(NotebookEntry.COLUMN_TRANSLATION));

        tvWord.setText(word);
        tvTrans.setText(trans);
    }


}
