package space.zhupeng.easycamera.callback;

import space.zhupeng.easycamera.CameraView;

/**
 * Created by zhupeng on 2017/9/11.
 */

public abstract class VideoCallback implements Callback {
    @Override
    public void onCameraOpened(CameraView cameraView) {
    }

    @Override
    public void onCameraClosed(CameraView cameraView) {
    }

    @Override
    public void onPictureTaken(byte[] data, CameraView cameraView) {
    }
}
