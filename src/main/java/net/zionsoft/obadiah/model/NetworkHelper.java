/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

package net.zionsoft.obadiah.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class NetworkHelper {
    static final String TRANSLATIONS_LIST_URL = "http://bible.zionsoft.net/translations/list.json";
    static final String TRANSLATION_URL_TEMPLATE = "http://bible.zionsoft.net/translations/%s.zip";

    static byte[] get(String url) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = getStream(url);

            int read;
            byte[] response = new byte[0];
            final byte[] buffer = new byte[2048];
            while ((read = bis.read(buffer)) > -1) {
                byte[] tmp = new byte[response.length + read];
                System.arraycopy(response, 0, tmp, 0, response.length);
                System.arraycopy(buffer, 0, tmp, response.length, read);
                response = tmp;
            }

            return response;
        } finally {
            if (bis != null)
                bis.close();
        }
    }

    static BufferedInputStream getStream(String url) throws IOException {
        final HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
        return new BufferedInputStream(httpConnection.getInputStream());
    }
}
