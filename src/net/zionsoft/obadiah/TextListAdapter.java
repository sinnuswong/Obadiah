package net.zionsoft.obadiah;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextListAdapter extends ListBaseAdapter
{
    public TextListAdapter(Context context)
    {
        super(context);
    }

    public void clickItem(int position)
    {
        if (position < 0 || position >= m_texts.length)
            return;

        m_selected[position] ^= true;
        notifyDataSetChanged();
    }

    public void setTexts(String[] texts)
    {
        super.setTexts(texts);

        final int length = texts.length;
        if (m_selected == null || length > m_selected.length)
            m_selected = new boolean[length];
        for (int i = 0; i < length; ++i)
            m_selected[i] = false;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        LinearLayout linearLayout;
        if (convertView == null) {
            linearLayout = new LinearLayout(m_context);
            for (int i = 0; i < 2; ++i) {
                TextView textView = new TextView(m_context);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                textView.setPadding(10, 10, 10, 10);
                textView.setTextColor(Color.BLACK);
                linearLayout.addView(textView);
            }
        } else {
            linearLayout = (LinearLayout) convertView;
        }

        if (m_selected[position])
            linearLayout.setBackgroundColor(Color.LTGRAY);
        else
            linearLayout.setBackgroundColor(Color.WHITE);

        TextView verseIndex = (TextView) linearLayout.getChildAt(0);
        verseIndex.setText(Integer.toString(position + 1));

        TextView verse = (TextView) linearLayout.getChildAt(1);
        verse.setText(m_texts[position]);

        return linearLayout;
    }

    private boolean m_selected[];
}
