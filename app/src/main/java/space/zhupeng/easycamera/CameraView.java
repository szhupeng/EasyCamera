package space.zhupeng.easycamera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import space.zhupeng.easycamera.callback.Callback;

public class CameraView extends FrameLayout implements CheckPermissionListener {

    public static final int FACING_BACK = 0;
    public static final int FACING_FRONT = 1;

    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /**
     * Flash will not be fired.
     */
    public static final int FLASH_OFF = 0;

    /**
     * Flash will always be fired during snapshot.
     */
    public static final int FLASH_ON = 1;

    /**
     * Constant emission of light during preview, auto-focus and snapshot.
     */
    public static final int FLASH_TORCH = 2;

    /**
     * Flash will be fired automatically when required.
     */
    public static final int FLASH_AUTO = 3;

    /**
     * Flash will be fired in red-eye reduction mode.
     */
    public static final int FLASH_RED_EYE = 4;

    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface FlashMode {
    }

    private CameraApi mCameraApi;

    private CallbackBridge mCallbackBridge;

    private DisplayOrientationDetector mDisplayOrientationDetector;

    public CameraView(@NonNull Context context) {
        this(context, null);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        if (isInEditMode()) {
            mCallbackBridge = null;
            mDisplayOrientationDetector = null;
            return;
        }

        PreviewView preview = obtainPreviewView(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mCameraApi = new CameraImpl(mCallbackBridge, preview);
        } else {
            mCameraApi = new Camera2Impl(context, mCallbackBridge, preview);
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                R.style.Widget_CameraView);
        //noinspection WrongConstant
        setFacing(a.getInt(R.styleable.CameraView_facing, FACING_BACK));
        setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true));
        //noinspection WrongConstant
        setFlashMode(a.getInt(R.styleable.CameraView_flash, FLASH_AUTO));
        a.recycle();

        // Display orientation detector
        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int orientation) {
                mCameraApi.setDisplayOrientation(orientation);
            }
        };
    }

    private PreviewView obtainPreviewView(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new SurfacePreviewView(context, this);
        } else {
            return new TexturePreviewView(context, this);
        }
    }

    /**
     * Open a camera device and start showing camera preview. This is typically called from
     * {@link Activity#onResume()}.
     */
    public void start() {
        if (!mCameraApi.start()) {
            //store the state ,and restore this state after fall back to Camera1
            Parcelable state = onSaveInstanceState();
            // Camera2 uses legacy hardware layer; fall back to Camera1
            mCameraApi = new CameraImpl(mCallbackBridge, obtainPreviewView(getContext()));
            onRestoreInstanceState(state);
            mCameraApi.start();
        }
    }

    /**
     * Stop camera preview and close the device. This is typically called from
     * {@link Activity#onPause()}.
     */
    public void stop() {
        mCameraApi.stop();
    }

    public void setCallback(Callback callback) {
        this.mCallbackBridge = new CallbackBridge(callback);
    }

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either {@link #FACING_BACK} or
     *               {@link #FACING_FRONT}.
     */
    public void setFacing(@Facing int facing) {
        mCameraApi.setFacing(facing);
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    public int getFacing() {
        //noinspection WrongConstant
        return mCameraApi.getFacing();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        mCameraApi.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
    public boolean isAutoFocus() {
        return mCameraApi.isAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flashMode The desired flash mode.
     */
    public void setFlashMode(@FlashMode int flashMode) {
        mCameraApi.setFlashMode(flashMode);
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @FlashMode
    public int getFlashMode() {
        //noinspection WrongConstant
        return mCameraApi.getFlashMode();
    }

    /**
     * Take a picture. The result will be returned to {@link Callback#onPictureTaken(byte[], CameraView)}.
     */
    public void takePicture() {
        mCameraApi.takePicture();
    }

    /**
     * Start record a video. The result will be returned to {@link Callback#onStartRecord(String, String, CameraView)}.
     *
     * @param dirPath  The path of the video to save
     * @param fileName The name of the video file
     */
    public void startRecordVideo(final String dirPath, final String fileName) {
        mCameraApi.startRecordVideo(dirPath, fileName);
    }

    /**
     * Stop record a video. The result will be returned to {@link Callback#onStopRecord(CameraView)}.
     */
    public void stopRecordVideo() {
        mCameraApi.stopRecordVideo();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getFacing();
        state.autoFocus = isAutoFocus();
        state.flash = getFlashMode();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setFacing(ss.facing);
        setAutoFocus(ss.autoFocus);
        setFlashMode(ss.flash);
    }

    @Override
    public boolean isPermissionGranted() {
        return (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
    }

    private class CallbackBridge implements CameraApi.Callback {

        private Callback mCallback;

        CallbackBridge(Callback callback) {
            this.mCallback = callback;
        }

        @Override
        public void onCameraOpened() {
            if (mCallback != null) {
                mCallback.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            if (mCallback != null) {
                mCallback.onCameraClosed(CameraView.this);
            }
        }

        @Override
        public void onPictureTaken(byte[] data) {
            if (mCallback != null) {
                mCallback.onPictureTaken(data, CameraView.this);
            }
        }

        @Override
        public void onStartRecord(String dirPath, String fileName) {
            if (mCallback != null) {
                mCallback.onStartRecord(dirPath, fileName, CameraView.this);
            }
        }

        @Override
        public void onStopRecord() {
            if (mCallback != null) {
                mCallback.onStopRecord(CameraView.this);
            }
        }
    }

    protected static class SavedState extends BaseSavedState {
        @Facing
        int facing;
        boolean autoFocus;
        @FlashMode
        int flash;

        @SuppressWarnings("WrongConstant")
        public SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });
    }
}
