package space.zhupeng.easycamera.callback;

import space.zhupeng.easycamera.CameraView;

/**
 * Created by zhupeng on 2017/9/11.
 */

public abstract class PictureCallback implements Callback {
    @Override
    public void onCameraOpened(CameraView cameraView) {
    }

    @Override
    public void onCameraClosed(CameraView cameraView) {
    }

    @Override
    public void onStartRecord(String dirPath, String fileName, CameraView cameraVie) {
    }

    @Override
    public void onStopRecord(CameraView cameraVie) {
    }
}
