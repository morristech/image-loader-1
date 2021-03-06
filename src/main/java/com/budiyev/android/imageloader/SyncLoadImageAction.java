/*
 * MIT License
 *
 * Copyright (c) 2018 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.budiyev.android.imageloader;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

final class SyncLoadImageAction<T> extends LoadImageAction<T> {
    private Bitmap mImage;

    public SyncLoadImageAction(@NonNull final DataDescriptor<T> descriptor, @NonNull final BitmapLoader<T> bitmapLoader,
            @Nullable final Size requiredSize, @Nullable final BitmapTransformation transformation,
            @Nullable final ImageCache memoryCache, @Nullable final ImageCache storageCache,
            @Nullable final LoadCallback loadCallback, @Nullable final ErrorCallback errorCallback,
            @NonNull final PauseLock pauseLock) {
        super(descriptor, bitmapLoader, requiredSize, transformation, memoryCache, storageCache, null, loadCallback,
                errorCallback, pauseLock);
    }

    @Nullable
    @WorkerThread
    public Bitmap load() {
        execute();
        return mImage;
    }

    @Override
    protected void onImageLoaded(@NonNull final Bitmap image) {
        mImage = image;
    }

    @Override
    protected void onError(@NonNull final Throwable error) {
        // Do nothing
    }

    @Override
    protected void onCancelled() {
        // Do nothing
    }
}
