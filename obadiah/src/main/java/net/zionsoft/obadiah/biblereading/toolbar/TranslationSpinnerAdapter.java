/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
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

package net.zionsoft.obadiah.biblereading.toolbar;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.translations.TranslationManagementActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

class TranslationSpinnerAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener {
    static class DropDownViewHolder {
        @BindView(R.id.checkbox)
        AppCompatCheckBox checkbox;

        @BindView(R.id.title)
        TextView title;

        DropDownViewHolder(View root) {
            ButterKnife.bind(this, root);
        }
    }

    private final ToolbarPresenter toolbarPresenter;
    private final LayoutInflater inflater;
    private final List<String> translations;

    TranslationSpinnerAdapter(Context context, ToolbarPresenter toolbarPresenter, List<String> translations) {
        this.inflater = LayoutInflater.from(context);
        this.toolbarPresenter = toolbarPresenter;
        this.translations = translations;
    }

    @Override
    public int getCount() {
        return translations.size();
    }

    @Override
    public String getItem(int position) {
        return translations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView textView = (TextView) (convertView == null
                ? inflater.inflate(R.layout.item_drop_down_selected, parent, false) : convertView);
        textView.setText(getItem(position));
        return textView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        final View view;
        final DropDownViewHolder viewHolder;
        if (convertView == null) {
            view = inflater.inflate(R.layout.item_drop_down, parent, false);

            viewHolder = new DropDownViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (DropDownViewHolder) view.getTag();
        }

        viewHolder.checkbox.setOnCheckedChangeListener(null);

        final String text = getItem(position);
        if (position < getCount() - 1) {
            viewHolder.checkbox.setTag(text);
            if (text.equals(toolbarPresenter.loadCurrentTranslation())) {
                viewHolder.checkbox.setEnabled(false);
                viewHolder.checkbox.setChecked(true);
            } else {
                viewHolder.checkbox.setEnabled(true);
                viewHolder.checkbox.setChecked(toolbarPresenter.isParallelTranslation(text));

                // should call this after calling setChecked()
                // otherwise the callback will be triggered unexpectedly
                viewHolder.checkbox.setOnCheckedChangeListener(this);
            }
            viewHolder.checkbox.setVisibility(View.VISIBLE);
        } else {
            // last item ("More"), hide the checkbox
            viewHolder.checkbox.setTag(null);
            viewHolder.checkbox.setVisibility(View.INVISIBLE);
        }

        viewHolder.title.setText(text);

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton checkbox, boolean isChecked) {
        final String translation = (String) checkbox.getTag();
        if (TextUtils.isEmpty(translation)) {
            return;
        }
        if (isChecked) {
            toolbarPresenter.loadParallelTranslation(translation);
        } else {
            toolbarPresenter.removeParallelTranslation(translation);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == getCount() - 1) {
            // last item ("More") selected, opens the translation management activity
            final Context context = parent.getContext();
            context.startActivity(TranslationManagementActivity.newStartIntent(context));
            return;
        }

        String currentTranslation = toolbarPresenter.loadCurrentTranslation();
        final String selected = getItem(position);
        if (TextUtils.isEmpty(selected) || selected.equals(currentTranslation)) {
            return;
        }

        currentTranslation = selected;
        toolbarPresenter.saveCurrentTranslation(currentTranslation);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }
}
