/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah.biblereading.verse;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseWithParallelTranslations;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class VerseListAdapter extends RecyclerView.Adapter {
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final Settings settings;
        private final Resources resources;

        @Bind(R.id.index)
        TextView index;

        @Bind(R.id.text)
        TextView text;

        private ViewHolder(View itemView, Settings settings, Resources resources) {
            super(itemView);

            this.settings = settings;
            this.resources = resources;
            ButterKnife.bind(this, itemView);
        }

        private void bind(Verse verse, int totalVerseCount, boolean selected) {
            itemView.setSelected(selected);

            final int textColor = settings.getTextColor();
            final float textSize = resources.getDimension(settings.getTextSize().textSize);
            index.setTextColor(textColor);
            index.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            if (totalVerseCount < 10) {
                index.setText(Integer.toString(verse.index.verse + 1));
            } else if (totalVerseCount < 100) {
                index.setText(String.format("%2d", verse.index.verse + 1));
            } else {
                index.setText(String.format("%3d", verse.index.verse + 1));
            }
            index.setVisibility(View.VISIBLE);

            text.setTextColor(textColor);
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            text.setText(verse.verseText);
        }

        private void bind(VerseWithParallelTranslations verse) {
            itemView.setSelected(false);

            index.setVisibility(View.GONE);

            text.setTextColor(settings.getTextColor());
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().textSize));

            StringBuilder sb = new StringBuilder();
            sb.append(verse.verseIndex.chapter + 1).append(':').append(verse.verseIndex.verse + 1);
            final int size = verse.texts.size();
            for (int i = 0; i < size; ++i) {
                final VerseWithParallelTranslations.Text text = verse.texts.get(i);
                sb.append('\n').append(text.translation).append(": ").append(text.text);
            }
            text.setText(sb.toString());
        }
    }

    private final Settings settings;
    private final LayoutInflater inflater;
    private final Resources resources;

    private List<Verse> verses;
    private List<VerseWithParallelTranslations> versesWithParallelTranslations;
    private boolean[] selected;
    private int selectedCount;

    VerseListAdapter(Context context, Settings settings) {
        this.settings = settings;
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }

    @Override
    public int getItemCount() {
        if (verses != null) {
            return verses.size();
        }
        if (versesWithParallelTranslations != null) {
            return versesWithParallelTranslations.size();
        }
        return 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_text, parent, false), settings, resources);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (verses != null) {
            ((ViewHolder) holder).bind(verses.get(position), getItemCount(), selected[position]);
        } else {
            ((ViewHolder) holder).bind(versesWithParallelTranslations.get(position));
        }
    }

    void setVerses(List<Verse> verses) {
        this.verses = verses;
        this.versesWithParallelTranslations = null;

        final int size = this.verses.size();
        if (selected == null || selected.length < size) {
            selected = new boolean[size];
        }
        deselectVerses();

        notifyDataSetChanged();
    }

    void setVersesWithParallelTranslations(List<VerseWithParallelTranslations> versesWithParallelTranslations) {
        this.verses = null;
        this.versesWithParallelTranslations = versesWithParallelTranslations;

        deselectVerses();

        notifyDataSetChanged();
    }

    void select(int position) {
        if (verses == null) {
            // TODO supports selection for verses with parallel translations
            return;
        }

        selected[position] ^= true;
        if (selected[position]) {
            ++selectedCount;
        } else {
            --selectedCount;
        }
    }

    boolean hasSelectedVerses() {
        return selectedCount > 0;
    }

    List<Verse> getSelectedVerses() {
        final List<Verse> selectedVerses = new ArrayList<>(selectedCount);
        for (int i = 0; i < selected.length; ++i) {
            if (selected[i]) {
                selectedVerses.add(verses.get(i));
            }
        }
        return selectedVerses;
    }

    void deselectVerses() {
        if (selected != null) {
            for (int i = 0; i < selected.length; ++i) {
                selected[i] = false;
            }
        }
        selectedCount = 0;
    }
}
