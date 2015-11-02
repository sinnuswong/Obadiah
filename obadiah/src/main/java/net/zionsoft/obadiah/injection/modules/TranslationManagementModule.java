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

package net.zionsoft.obadiah.injection.modules;

import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.model.translations.TranslationManager;
import net.zionsoft.obadiah.mvp.models.TranslationManagementModel;
import net.zionsoft.obadiah.mvp.presenters.TranslationManagementPresenter;
import net.zionsoft.obadiah.network.BackendInterface;

import dagger.Module;
import dagger.Provides;

@Module
public class TranslationManagementModule {
    @Provides
    public TranslationManagementModel provideTranslationManagementModel(TranslationManager translationManager, BackendInterface backendInterface) {
        return new TranslationManagementModel(translationManager, backendInterface);
    }

    @Provides
    @ActivityScope
    public TranslationManagementPresenter provideTranslationManagementPresenter(TranslationManagementModel translationManagementModel) {
        return new TranslationManagementPresenter(translationManagementModel);
    }
}
