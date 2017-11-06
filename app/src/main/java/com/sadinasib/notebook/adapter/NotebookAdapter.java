package com.sadinasib.notebook.adapter;

import android.content.Context;
import android.database.Cursor;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sadinasib.notebook.R;

import java.util.Locale;

import static com.sadinasib.notebook.data.NotebookContract.NotebookEntry;

/**
 * Created by sadin on 05-Nov-17.
 */

public class NotebookAdapter extends CursorAdapter {
    private static final String TAG = NotebookAdapter.class.getSimpleName();
    private TextToSpeech mTextToSpeech;

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

        final TextView tvWord = (TextView) view.findViewById(R.id.list_items_text_word);
        final TextView tvTrans = (TextView) view.findViewById(R.id.list_items_text_trans);

        String word = cursor.getString(cursor.getColumnIndexOrThrow(NotebookEntry.COLUMN_WORD));
        final String trans = cursor.getString(cursor.getColumnIndexOrThrow(NotebookEntry.COLUMN_TRANSLATION));

        tvWord.setText(word);
        tvTrans.setText(trans);

        ImageButton buttonPlay = (ImageButton) view.findViewById(R.id.list_items_button_play);
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speak(context, trans);
            }
        });
    }

    private void speak(Context context, final String word) {
        mTextToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTextToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, " speak onInit: Language is not supported");
                    }
                    mTextToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    Log.e(TAG, "speak onInit: Initialization failed");
                }
            }
        });
    }

}
