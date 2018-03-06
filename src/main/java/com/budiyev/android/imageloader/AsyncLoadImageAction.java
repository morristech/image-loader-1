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

import java.util.concurrent.ExecutorService;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class AsyncLoadImageAction<T> extends LoadImageAction<T> {
    public AsyncLoadImageAction(@NonNull DataDescriptor<T> descriptor, @NonNull BitmapLoader<T> bitmapLoader,
            @Nullable Size requiredSize, @Nullable BitmapTransformation transformation,
            @Nullable ImageCache memoryCache, @Nullable ImageCache storageCache,
            @Nullable ExecutorService cacheExecutor, @Nullable LoadCallback loadCallback,
            @Nullable ErrorCallback errorCallback, @NonNull PauseLock pauseLock) {
        super(descriptor, bitmapLoader, requiredSize, transformation, memoryCache, storageCache, cacheExecutor,
                loadCallback, errorCallback, pauseLock);
    }

    @Override
    protected void onImageLoaded(@NonNull Bitmap image) {
        // Do nothing
    }

    @Override
    protected void onError(@NonNull Throwable error) {
        // Do nothing
    }

    @Override
    protected void onCancelled() {
        // Do nothing
    }
}