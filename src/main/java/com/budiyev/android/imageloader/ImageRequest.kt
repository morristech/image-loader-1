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
package com.budiyev.android.imageloader

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.annotation.AnyThread
import android.support.annotation.DrawableRes
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import android.support.annotation.MainThread
import android.support.annotation.Px
import android.support.annotation.WorkerThread
import android.view.View
import java.util.*
import java.util.concurrent.ExecutorService

/**
 * Image request
 * <br></br>
 * Note that all methods of this class should be called on the same thread as [ImageLoader.from] method
 * that created this request, each request can be executed only once
 */
class ImageRequest<T> internal constructor(private val mResources: Resources,
        private val mLoadExecutor: ExecutorService, private val mCacheExecutor: ExecutorService,
        private val mPauseLock: PauseLock, private val mMainThreadHandler: Handler,
        private val mMemoryCache: ImageCache?, private val mStorageCache: ImageCache?,
        private val mBitmapLoader: BitmapLoader<T>, private val mDescriptor: DataDescriptor<T>) {
    private var mRequiredSize: Size? = null
    private var mLoadCallback: LoadCallback? = null
    private var mErrorCallback: ErrorCallback? = null
    private var mDisplayCallback: DisplayCallback? = null
    private var mTransformations: MutableList<BitmapTransformation>? = null
    private var mPlaceholder: Drawable? = null
    private var mErrorDrawable: Drawable? = null
    private var mFadeEnabled = true
    private var mFadeDuration = DEFAULT_FADE_DURATION
    private var mCornerRadius: Float = 0.toFloat()
    private var mMemoryCacheEnabled = true
    private var mStorageCacheEnabled = true
    private var mExecuted: Boolean = false

    private val transformation: BitmapTransformation?
        get() {
            val t = mTransformations
            return if (t != null && !t.isEmpty()) {
                if (t.size == 1) {
                    t[0]
                } else {
                    BitmapTransformationGroup(t)
                }
            } else {
                null
            }
        }

    private val memoryCache: ImageCache?
        get() = if (mMemoryCacheEnabled) mMemoryCache else null

    private val storageCache: ImageCache?
        get() = if (mStorageCacheEnabled) mStorageCache else null

    /**
     * Required image size
     */
    fun size(requiredSize: Size?) {
        if (requiredSize != null) {
            checkSize(requiredSize.width, requiredSize.height)
        }
        mRequiredSize = requiredSize
    }

    /**
     * Required image size
     */
    fun size(@Px requiredWidth: Int, @Px requiredHeight: Int) {
        checkSize(requiredWidth, requiredHeight)
        mRequiredSize = Size(requiredWidth, requiredHeight)
    }

    /**
     * Display image with rounded corners using maximum corner radius,
     * for square image, will lead to circle result
     */
    fun roundCorners() {
        mCornerRadius = RoundedDrawable.MAX_RADIUS
    }

    /**
     * Display image with rounded corners using specified corner radius (in pixels),
     * zero means that rounding is disabled; note that visible rounding depends on image size
     * and image view scale type
     */
    fun roundCorners(@FloatRange(from = 0.0) radius: Float) {
        if (radius < 0f) {
            throw IllegalArgumentException("Corner radius should be greater than or equal to zero")
        }
        mCornerRadius = radius
    }

    /**
     * Placeholder
     */
    fun placeholder(placeholder: Drawable?) {
        mPlaceholder = placeholder
    }

    /**
     * Placeholder
     */
    fun placeholder(@DrawableRes resId: Int) {
        mPlaceholder = mResources.getDrawable(resId)
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    fun errorDrawable(errorDrawable: Drawable?) {
        mErrorDrawable = errorDrawable
    }

    /**
     * Error drawable, that will be displayed when image, couldn't be loaded
     */
    fun errorDrawable(@DrawableRes resId: Int) {
        mErrorDrawable = mResources.getDrawable(resId)
    }

    /**
     * Add bitmap transformation
     *
     * @see BitmapTransformation
     */
    fun transform(transformation: BitmapTransformation) {
        transformations().add(transformation)
    }

    /**
     * Add bitmap transformations
     *
     * @see BitmapTransformation
     */
    fun transform(transformations: Collection<BitmapTransformation>) {
        transformations().addAll(transformations)
    }

    /**
     * Enable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    fun fade() {
        mFadeEnabled = true
        mFadeDuration = DEFAULT_FADE_DURATION
    }

    /**
     * Disable fade effect for images that isn't cached in memory,
     * supported on API 19+
     */
    fun noFade() {
        mFadeEnabled = false
    }

    /**
     * Enable fade effect for images that isn't cached in memory,
     * allows to specify fade effect duration,
     * supported on API 19+
     */
    fun fade(@IntRange(from = 0L) duration: Long) {
        if (duration < 0L) {
            throw IllegalArgumentException("Fade duration should be greater than or equal to zero")
        }
        mFadeEnabled = true
        mFadeDuration = duration
    }

    /**
     * Load callback
     */
    fun onLoaded(callback: LoadCallback?) {
        mLoadCallback = callback
    }

    /**
     * Error callback
     */
    fun onError(callback: ErrorCallback?) {
        mErrorCallback = callback
    }

    /**
     * Display callback
     */
    fun onDisplayed(callback: DisplayCallback?) {
        mDisplayCallback = callback
    }

    /**
     * Don't use memory cache in this request
     */
    fun noMemoryCache() {
        mMemoryCacheEnabled = false
    }

    /**
     * Don't use storage cache in this request
     */
    fun noStorageCache() {
        mStorageCacheEnabled = false
    }

    /**
     * Load image synchronously (on current thread)
     *
     * @return Loaded image or `null` if image could not be loaded
     * @throws IllegalStateException if request has already been executed
     */
    @WorkerThread
    fun loadSync(): Bitmap? {
        checkAndSetExecutedState()
        return SyncLoadImageAction(mDescriptor, mBitmapLoader, mRequiredSize, transformation, memoryCache, storageCache,
                mLoadCallback, mErrorCallback, mPauseLock).load()
    }

    /**
     * Load image asynchronously
     *
     * @return [ImageRequestDelegate] object, associated with execution of the request
     * @throws IllegalStateException if request has already been executed
     * @see .onLoaded
     *
     * @see LoadCallback
     */
    @AnyThread
    fun load(): ImageRequestDelegate {
        checkAndSetExecutedState()
        return AsyncLoadImageAction(mDescriptor, mBitmapLoader, mRequiredSize, transformation, memoryCache,
                storageCache, mCacheExecutor, mLoadCallback, mErrorCallback, mPauseLock).submit(mLoadExecutor)
    }

    /**
     * Load image asynchronously and display it into the specified `view`
     *
     * @return [ImageRequestDelegate] object, associated with execution of the request
     * @throws IllegalStateException if request has already been executed
     */
    @MainThread
    fun load(view: View): ImageRequestDelegate {
        checkAndSetExecutedState()
        val resources = mResources
        val descriptor = mDescriptor
        val requiredSize = mRequiredSize
        val transformation = transformation
        val loadCallback = mLoadCallback
        val displayCallback = mDisplayCallback
        val memoryCache = memoryCache
        val cornerRadius = mCornerRadius
        var image: Bitmap? = null
        val key = buildFullKey(descriptor.key, requiredSize, transformation)
        if (key != null && memoryCache != null) {
            image = memoryCache.get(key)
        }
        val currentAction = getDisplayImageAction(view)
        if (image != null) {
            currentAction?.cancel()
            loadCallback?.onLoaded(image)
            if (cornerRadius > 0 || cornerRadius == RoundedDrawable.MAX_RADIUS) {
                setDrawable(RoundedDrawable(resources, image, cornerRadius), view)
            } else {
                setBitmap(resources, image, view)
            }
            displayCallback?.onDisplayed(image, view)
            return EmptyImageRequestDelegate.INSTANCE
        }
        if (currentAction != null) {
            if (currentAction.hasSameKey(key) && !currentAction.isCancelled) {
                return currentAction
            }
            currentAction.cancel()
        }
        var placeholder = mPlaceholder
        if (placeholder == null) {
            placeholder = ColorDrawable(Color.TRANSPARENT)
        }
        val action = DisplayImageAction(resources, view, descriptor, mBitmapLoader, requiredSize, transformation,
                placeholder, mErrorDrawable, memoryCache, storageCache, mCacheExecutor, loadCallback, mErrorCallback,
                displayCallback, mPauseLock, mMainThreadHandler, mFadeEnabled, mFadeDuration, cornerRadius)
        setDrawable(PlaceholderDrawable(placeholder, action), view)
        return action.submit(mLoadExecutor)
    }

    /**
     * Delete all cached images for specified data asynchronously
     *
     * @return [ImageRequestDelegate] object, associated with execution of the request
     * @throws IllegalStateException if request has already been executed
     */
    @AnyThread
    fun invalidate(): ImageRequestDelegate {
        checkAndSetExecutedState()
        return InvalidateAction(mDescriptor, memoryCache, storageCache).submit(mCacheExecutor)
    }

    private fun transformations(): MutableList<BitmapTransformation> {
        var t = mTransformations
        if (t == null) {
            t = ArrayList(TRANSFORMATIONS_CAPACITY)
            mTransformations = t
        }
        return t
    }

    private fun checkAndSetExecutedState() {
        if (mExecuted) {
            throw IllegalStateException("Request can be executed only once")
        }
        mExecuted = true
    }

    private fun checkSize(width: Int, height: Int) {
        if (width < 1 || height < 1) {
            throw IllegalArgumentException("Width and height should be greater than zero")
        }
    }

    companion object {
        private const val DEFAULT_FADE_DURATION: Long = 200L
        private const val TRANSFORMATIONS_CAPACITY: Int = 4
    }
}
