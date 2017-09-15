package space.zhupeng.easycamera;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;

/**
 * Created by zhupeng on 2017/8/31.
 */

abstract class CameraApi {

    interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPictureTaken(byte[] data);

        void onStartRecord(final String dirPath, final String fileName);

        void onStopRecord();
    }

    protected final Callback mCallback;

    protected final PreviewView mPreviewView;

    protected MediaRecorder mVideoRecorder;
    protected CamcorderProfile mCamcorderProfile;
    protected boolean isRecording;

    public CameraApi(Callback callback, PreviewView preview) {
        this.mCallback = callback;
        this.mPreviewView = preview;
    }

    protected void releaseVideoRecorder() {
        try {
            if (mVideoRecorder != null) {
                mVideoRecorder.reset();
                mVideoRecorder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mVideoRecorder = null;
        }
    }

    abstract boolean start();

    abstract void stop();

    abstract boolean isCameraOpened();

    abstract void setFacing(int facing);

    abstract int getFacing();

    abstract void setAutoFocus(boolean autoFocus);

    abstract boolean isAutoFocus();

    abstract void setFlashMode(int flashMode);

    abstract int getFlashMode();

    abstract void setDisplayOrientation(int orientation);

    /**
     * 拍照
     */
    abstract void takePicture();

    /**
     * 开始录制视频
     *
     * @param dirPath  视频存储路径
     * @param fileName 视频文件名称
     */
    abstract void startRecordVideo(final String dirPath, final String fileName);

    /**
     * 停止录制视频
     */
    abstract void stopRecordVideo();
}
