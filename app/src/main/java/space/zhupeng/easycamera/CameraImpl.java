package space.zhupeng.easycamera;

import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.v4.util.SparseArrayCompat;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import space.zhupeng.easycamera.utils.CameraHelper;

/**
 * Created by zhupeng on 2017/9/1.
 */
@SuppressWarnings("deprecation")
class CameraImpl extends CameraApi {

    private static final int INVALID_CAMERA_ID = -1;
    private static final SparseArrayCompat<String> FLASH_MODES = new SparseArrayCompat<>();

    static {
        FLASH_MODES.put(CameraView.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(CameraView.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(CameraView.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(CameraView.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(CameraView.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
    }

    private int mCameraId;
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    protected Camera mCamera;
    private Camera.Parameters mCameraParameters;
    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private boolean isShowingPreview;
    private boolean isAutoFocus;
    private int mFacing;
    private int mFlashMode;
    private int mDisplayOrientation;

    public CameraImpl(Callback callback, PreviewView preview) {
        super(callback, preview);

        preview.setCallback(new PreviewView.Callback() {
            @Override
            public void onSurfaceChanged() {
                if (mCamera != null) {
                    setPreviewMedium();
                    adjustCameraParameters();
                }
            }
        });
    }

    @Override
    boolean start() {
        chooseCamera();
        openCamera();
        if (mPreviewView.isReady()) {
            setPreviewMedium();
        }
        isShowingPreview = true;
        mCamera.startPreview();
        return true;
    }

    @Override
    void stop() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        isShowingPreview = false;
        releaseCamera();
    }

    @Override
    boolean isCameraOpened() {
        return this.mCamera != null;
    }

    @Override
    void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }

        mFacing = facing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    int getFacing() {
        return mFacing;
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (isAutoFocus == autoFocus) {
            return;
        }

        if (setAutoFocusInternal(autoFocus)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    boolean isAutoFocus() {
        if (!isCameraOpened()) {
            return isAutoFocus;
        }

        String focusMode = mCameraParameters.getFocusMode();
        return focusMode != null && focusMode.contains("continuous");
    }

    @Override
    void setFlashMode(final int flashMode) {
        if (flashMode == mFlashMode) {
            return;
        }
        if (setFlashModeInternal(flashMode)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    int getFlashMode() {
        return mFlashMode;
    }

    @Override
    void setDisplayOrientation(int orientation) {
        if (mDisplayOrientation == orientation) {
            return;
        }
        mDisplayOrientation = orientation;
        if (isCameraOpened()) {
            mCameraParameters.setRotation(calculateRotation(orientation));
            mCamera.setParameters(mCameraParameters);
            final boolean needsToStopPreview = isShowingPreview && Build.VERSION.SDK_INT < 14;
            if (needsToStopPreview) {
                mCamera.stopPreview();
            }
            mCamera.setDisplayOrientation(calculateOrientation(orientation));
            if (needsToStopPreview) {
                mCamera.startPreview();
            }
        }
    }

    @Override
    void takePicture() {
        if (!isCameraOpened()) {
            throw new IllegalStateException("Camera is not ready. Call start() before takePicture().");
        }
        if (isAutoFocus()) {
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    takePictureInternal();
                }
            });
        } else {
            takePictureInternal();
        }
    }

    void takePictureInternal() {
        if (!isCapturing.getAndSet(true)) {
            mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    isCapturing.set(false);
                    if (mCallback != null) {
                        mCallback.onPictureTaken(data);
                    }
                    camera.cancelAutoFocus();
                    camera.startPreview();
                }
            });
        }
    }


    @Override
    void startRecordVideo(final String dirPath, final String fileName) {
        if (!isCameraOpened()) {
            throw new IllegalStateException("Camera is not ready. Call start() before startRecordVideo(String).");
        }

        if (isRecording) return;

        if (prepareVideoRecorder(dirPath)) {
            mVideoRecorder.start();
            isRecording = true;
            if (mCallback != null) {
                mCallback.onStartRecord(dirPath, fileName);
            }
        }
    }

    @Override
    void stopRecordVideo() {
        if (isRecording) {
            try {
                if (mVideoRecorder != null) mVideoRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            isRecording = false;
            releaseVideoRecorder();

            if (mCallback != null) {
                mCallback.onStopRecord();
            }
        }
    }

    final void setPreviewMedium() {
        try {
            if (mPreviewView.getOutputClass() == SurfaceHolder.class) {
                final boolean needsToStopPreview = isShowingPreview && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
                if (needsToStopPreview) {
                    mCamera.stopPreview();
                }
                mCamera.setPreviewDisplay(mPreviewView.getSurfaceHolder());
                if (needsToStopPreview) {
                    mCamera.startPreview();
                }
            } else {
                mCamera.setPreviewTexture((SurfaceTexture) mPreviewView.getSurfaceTexture());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void adjustCameraParameters() {
        Camera.Size previewSize = chooseOptimalSize(mCameraParameters.getSupportedPreviewSizes());

        // Always re-apply camera parameters
        List<Camera.Size> sizes = mCameraParameters.getSupportedPictureSizes();
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                return Long.signum((long) o1.width * o1.height - (long) o2.width * o2.height);
            }
        });
        final Camera.Size pictureSize = sizes.get(sizes.size() - 1);
        if (isShowingPreview) {
            mCamera.stopPreview();
        }
        mCameraParameters.setPreviewSize(previewSize.width, previewSize.height);
        mCameraParameters.setPictureSize(pictureSize.width, pictureSize.height);
        mCameraParameters.setRotation(calculateRotation(mDisplayOrientation));
        setAutoFocusInternal(isAutoFocus);
        setFlashModeInternal(mFlashMode);
        mCameraParameters.setJpegQuality(100);
        mCameraParameters.setPictureFormat(PixelFormat.JPEG);
        mCamera.setParameters(mCameraParameters);
        if (isShowingPreview) {
            mCamera.startPreview();
        }
    }

    /**
     * This rewrites {@link #mCameraId} and {@link #mCameraInfo}.
     */
    private void chooseCamera() {
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == mFacing) {
                mCameraId = i;
                return;
            }
        }
        mCameraId = INVALID_CAMERA_ID;
    }

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }
        mCamera = Camera.open(mCameraId);
        mCamcorderProfile = CameraHelper.getCamcorderProfile(mCameraId, -1, -1);
        mCameraParameters = mCamera.getParameters();
        adjustCameraParameters();
        mCamera.setDisplayOrientation(calculateOrientation(mDisplayOrientation));
        if (mCallback != null) {
            mCallback.onCameraOpened();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            if (mCallback != null) {
                mCallback.onCameraClosed();
            }
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Camera.Size chooseOptimalSize(List<Camera.Size> sizes) {
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                return Long.signum((long) o1.width * o1.height - (long) o2.width * o2.height);
            }
        });

        if (!mPreviewView.isReady()) { // Not yet laid out
            return sizes.get(0); // Return the smallest size
        }
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = mPreviewView.getWidth();
        final int surfaceHeight = mPreviewView.getHeight();
        if (isLandscape(mDisplayOrientation)) {
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        } else {
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Camera.Size result = null;
        for (Camera.Size size : sizes) { // Iterate from small to large
            if (desiredWidth <= size.width && desiredHeight <= size.height) {
                return size;

            }
            result = size;
        }
        return result;
    }

    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
     * <p>
     * This calculation is used for orienting the preview
     * <p>
     * Note: This is not the same calculation as the camera rotation
     *
     * @param degrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calculateOrientation(int degrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + degrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - degrees + 360) % 360;
        }
    }

    /**
     * Calculate camera rotation
     * <p>
     * This calculation is applied to the output JPEG either via Exif Orientation tag
     * or by actually transforming the bitmap. (Determined by vendor camera API implementation)
     * <p>
     * Note: This is not the same calculation as the display orientation
     *
     * @param degrees Screen orientation in degrees
     * @return Number of degrees to rotate image in order for it to view correctly.
     */
    private int calculateRotation(int degrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + degrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(degrees) ? 180 : 0;
            return (mCameraInfo.orientation + degrees + landscapeFlip) % 360;
        }
    }

    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param degrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int degrees) {
        return (degrees == 90 || degrees == 270);
    }

    private boolean prepareVideoRecorder(final String path) {
        mVideoRecorder = new MediaRecorder();
        try {
            mCamera.lock();
            mCamera.unlock();
            mVideoRecorder.setCamera(mCamera);

            mVideoRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mVideoRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

            mVideoRecorder.setOutputFormat(mCamcorderProfile.fileFormat);
            mVideoRecorder.setVideoFrameRate(mCamcorderProfile.videoFrameRate);
            mVideoRecorder.setVideoSize(mPreviewView.getWidth(), mPreviewView.getHeight());
            mVideoRecorder.setVideoEncodingBitRate(mCamcorderProfile.videoBitRate);
            mVideoRecorder.setVideoEncoder(mCamcorderProfile.videoCodec);

            mVideoRecorder.setAudioEncodingBitRate(mCamcorderProfile.audioBitRate);
            mVideoRecorder.setAudioChannels(mCamcorderProfile.audioChannels);
            mVideoRecorder.setAudioSamplingRate(mCamcorderProfile.audioSampleRate);
            mVideoRecorder.setAudioEncoder(mCamcorderProfile.audioCodec);

            mVideoRecorder.setOutputFile(path);
            mVideoRecorder.setMaxFileSize(-1);
            mVideoRecorder.setMaxDuration(-1);
            mVideoRecorder.setOrientationHint(calculateRotation(mDisplayOrientation));
            mVideoRecorder.setPreviewDisplay(mPreviewView.getSurface());

            mVideoRecorder.prepare();
            return true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        releaseVideoRecorder();
        return false;
    }

    @Override
    protected void releaseVideoRecorder() {
        super.releaseVideoRecorder();

        try {
            mCamera.lock(); // lock camera for later use
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        isAutoFocus = autoFocus;
        if (isCameraOpened()) {
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setFlashModeInternal(int flashMode) {
        if (isCameraOpened()) {
            List<String> modes = mCameraParameters.getSupportedFlashModes();
            String mode = FLASH_MODES.get(flashMode);
            if (modes != null && modes.contains(mode)) {
                mCameraParameters.setFlashMode(mode);
                mFlashMode = flashMode;
                return true;
            }
            String currentMode = FLASH_MODES.get(flashMode);
            if (modes == null || !modes.contains(currentMode)) {
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mFlashMode = CameraView.FLASH_OFF;
                return true;
            }
            return false;
        } else {
            mFlashMode = flashMode;
            return false;
        }
    }
}
