package space.zhupeng.easycamera;

/**
 * Created by zhupeng on 2017/8/31.
 */

abstract class CameraApi {

    interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPictureTaken(byte[] data);

        void onVideoRecorded();
    }

    protected final Callback mCallback;

    protected final PreviewView mPreviewView;

    public CameraApi(Callback callback, PreviewView preview) {
        this.mCallback = callback;
        this.mPreviewView = preview;
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
     * 录制视频
     */
    abstract void recordVideo();
}
