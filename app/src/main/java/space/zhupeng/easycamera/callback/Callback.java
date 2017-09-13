package space.zhupeng.easycamera.callback;

import space.zhupeng.easycamera.CameraView;

/**
 * Created by zhupeng on 2017/9/11.
 */

public interface Callback {
    /**
     * Called when camera is opened.
     *
     * @param cameraView The associated {@link CameraView}.
     */
    void onCameraOpened(CameraView cameraView);

    /**
     * Called when camera is closed.
     *
     * @param cameraView The associated {@link CameraView}.
     */
    void onCameraClosed(CameraView cameraView);

    /**
     * Called when a picture is taken.
     *
     * @param data       JPEG data.
     * @param cameraView The associated {@link CameraView}.
     */
    void onPictureTaken(byte[] data, CameraView cameraView);

    /**
     * Called when a video is recorded.
     *
     * @param cameraVie
     */
    void onVideoRecorded(CameraView cameraVie);
}
