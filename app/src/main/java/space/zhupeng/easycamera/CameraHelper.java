package space.zhupeng.easycamera;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;

import space.zhupeng.easycamera.callback.Callback;
import space.zhupeng.easycamera.callback.PictureCallback;
import space.zhupeng.easycamera.compress.Compress;
import space.zhupeng.easycamera.compress.CompressEngine;
import space.zhupeng.easycamera.compress.CompressResultListener;

/**
 * Created by zhupeng on 2017/8/31.
 */

public class CameraHelper {

    private CameraView mCameraView;

    private Handler mSaveHandler;

    private String mDirPath;
    private String mFileName;

    private Callback mCallback;

    private Compress mCompress;
    private CompressResultListener mCompressResultListener;

    private static class CameraHelperHolder {
        private static final CameraHelper INSTANCE = new CameraHelper();
    }

    private CameraHelper() {
    }

    public static final CameraHelper of(CameraView cameraView) {
        CameraHelper helper = CameraHelperHolder.INSTANCE;
        helper.setCameraView(cameraView);
        return helper;
    }

    private void setCameraView(CameraView cameraView) {
        this.mCameraView = cameraView;
        if (mCameraView != null) {
            mCameraView.setCallback(mCallbackProxy);
        }
    }

    public void start() {
        if (mCameraView != null) {
            mCameraView.start();
        }
    }

    public void stop() {
        if (mCameraView != null) {
            mCameraView.stop();
        }
        mCameraView = null;
    }

    public void destory() {
        if (mSaveHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mSaveHandler.getLooper().quitSafely();
            } else {
                mSaveHandler.getLooper().quit();
            }
            mSaveHandler = null;
        }
    }

    public CameraHelper setCallback(Callback callback) {
        this.mCallback = callback;
        return this;
    }

    /**
     * @param dirPath  The directory path of the picture to save
     * @param fileName The picture name to save
     */
    public CameraHelper takePicture(@Nullable String dirPath, @Nullable String fileName) {
        if (TextUtils.isEmpty(dirPath)) {
            mDirPath = Environment.getExternalStorageDirectory().getPath();
        } else {
            mDirPath = dirPath;
        }

        if (TextUtils.isEmpty(fileName)) {
            mFileName = TextUtils.concat("IMG_", Long.toString(System.currentTimeMillis()), ".jpg").toString();
        } else {
            mFileName = fileName;
        }

        if (mCameraView != null) {
            mCameraView.takePicture();
        }

        return this;
    }

    /**
     * 压缩图片
     *
     * @param compress
     * @param listener
     */
    public void compress(final Compress compress, final CompressResultListener listener) {
        this.mCompress = compress;
        this.mCompressResultListener = listener;
    }

    public void crop() {
        // TODO
    }

    private Handler obtainSaveHandler() {
        if (mSaveHandler == null) {
            HandlerThread thread = new HandlerThread("background_save");
            thread.start();
            mSaveHandler = new Handler(thread.getLooper());
        }
        return mSaveHandler;
    }

    private final Callback mCallbackProxy = new PictureCallback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            if (mCallback != null) {
                mCallback.onCameraOpened(cameraView);
            }
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            if (mCallback != null) {
                mCallback.onCameraClosed(cameraView);
            }
        }

        @Override
        public void onPictureTaken(final byte[] data, CameraView cameraView) {
            obtainSaveHandler().post(new ImageSaver(data, mDirPath, mFileName, new ImageSaver.SaveListener() {
                @Override
                public void onSuccess(File file) {
                    CompressEngine.with(mCompress).file(file).compress(mCompressResultListener);
                }

                @Override
                public void onError(Exception e) {

                }
            }));

            if (mCallback != null) {
                mCallback.onPictureTaken(data, cameraView);
            }
        }
    };
}
