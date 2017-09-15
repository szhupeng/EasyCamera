package space.zhupeng.easycamera.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhupeng on 2017/9/13.
 */

public final class CameraHelper {

    public static final int FLASH_MODE_OFF = 0;
    public static final int FLASH_MODE_ALWAYS_ON = 1;
    public static final int FLASH_MODE_AUTO = 2;

    public static final int QUALITY_HIGH = CamcorderProfile.QUALITY_HIGH;
    public static final int QUALITY_LOW = CamcorderProfile.QUALITY_LOW;
    public static final int QUALITY_480P = CamcorderProfile.QUALITY_480P;
    public static final int QUALITY_720P = CamcorderProfile.QUALITY_720P;
    public static final int QUALITY_1080P = CamcorderProfile.QUALITY_1080P;

    private CameraHelper() {
    }

    /**
     * 格式化时间为00:00
     *
     * @param timeMillis 毫秒数
     * @return
     */
    public static String formatDuration(long timeMillis) {
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(timeMillis),
                TimeUnit.MILLISECONDS.toSeconds(timeMillis)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeMillis)));
    }

    /**
     * 是否有相机
     *
     * @param context
     * @return
     */
    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    /**
     * 是否支持Camera2的api
     *
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isSupportCamera2(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] idList = manager.getCameraIdList();
            boolean notNull = true;
            if (idList.length == 0) {
                notNull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notNull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);

                    final int supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false;
                        break;
                    }
                }
            }
            return notNull;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 生成存储路径
     *
     * @param context
     * @param dirPath
     * @return
     */
    public static File generateStorageDir(Context context, @Nullable String dirPath) {
        File mediaStorageDir = null;
        if (dirPath != null) {
            mediaStorageDir = new File(dirPath);
        } else {
            mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), context.getPackageName());
        }

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        return mediaStorageDir;
    }

    /**
     * 获取支持的闪光灯模式
     *
     * @param context
     * @param parameters
     * @return
     */
    public static List<Integer> getSupportedFlashModes(
            Context context, Camera.Parameters parameters) {
        //check has system feature for flash
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            List<String> modes = parameters.getSupportedFlashModes();
            if (modes == null
                    || (modes.size() == 1 && modes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF))) {
                return null; //not supported
            } else {
                ArrayList<Integer> flashModes = new ArrayList<>();
                for (String mode : modes) {
                    switch (mode) {
                        case Camera.Parameters.FLASH_MODE_AUTO:
                            if (!flashModes.contains(FLASH_MODE_AUTO))
                                flashModes.add(FLASH_MODE_AUTO);
                            break;
                        case Camera.Parameters.FLASH_MODE_ON:
                            if (!flashModes.contains(FLASH_MODE_ALWAYS_ON))
                                flashModes.add(FLASH_MODE_ALWAYS_ON);
                            break;
                        case Camera.Parameters.FLASH_MODE_OFF:
                            if (!flashModes.contains(FLASH_MODE_OFF))
                                flashModes.add(FLASH_MODE_OFF);
                            break;
                        default:
                            break;
                    }
                }
                return flashModes;
            }
        } else {
            return null; //not supported
        }
    }

    /**
     * 获取Camera2支持的闪关灯模式
     *
     * @param context
     * @param characteristics
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<Integer> getSupportedFlashModes(Context context, CameraCharacteristics characteristics) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        } else if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (flashAvailable == null || !flashAvailable) return null;

            int[] modes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            if (modes == null || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AE_MODE_OFF)) {
                return null;
            } else {
                ArrayList<Integer> flashModes = new ArrayList<>(3);
                for (int mode : modes) {
                    switch (mode) {
                        case CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH:
                            if (!flashModes.contains(FLASH_MODE_AUTO))
                                flashModes.add(FLASH_MODE_AUTO);
                            break;
                        case CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH:
                            if (!flashModes.contains(FLASH_MODE_ALWAYS_ON))
                                flashModes.add(FLASH_MODE_ALWAYS_ON);
                            break;
                        case CameraCharacteristics.CONTROL_AE_MODE_ON:
                            if (!flashModes.contains(FLASH_MODE_OFF))
                                flashModes.add(FLASH_MODE_OFF);
                        default:
                            break;
                    }
                }
                return flashModes;
            }
        }
        return null; //not supported
    }

    @SuppressWarnings("deprecation")
    public static Size getPictureSize(List<Size> sizes) {
        if (sizes == null || sizes.isEmpty()) return null;
        if (sizes.size() == 1) return sizes.get(0);

        Size maxPictureSize = Collections.max(sizes, new SizeComparator());

        return maxPictureSize;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size getPictureSize(Size[] sizes) {
        if (sizes == null || sizes.length == 0) return null;

        List<Size> choices = Arrays.asList(sizes);

        if (choices.size() == 1) return choices.get(0);

        Size maxPictureSize = Collections.max(choices, new SizeComparator());

        return maxPictureSize;
    }

    @SuppressWarnings("deprecation")
    public static Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.width - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @SuppressWarnings("deprecation")
    public static Size getSizeWithClosestRatio(List<Size> sizes, int width, int height) {

        if (sizes == null) return null;

        double MIN_TOLERANCE = 100;
        double targetRatio = (double) height / width;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Size size : sizes) {
            if (size.width == width && size.height == height)
                return size;

            double ratio = (double) size.height / size.width;

            if (Math.abs(ratio - targetRatio) < MIN_TOLERANCE) {
                MIN_TOLERANCE = Math.abs(ratio - targetRatio);
                minDiff = Double.MAX_VALUE;
            } else continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size getOptimalPreviewSize(Size[] sizes, int width, int height) {

        if (sizes == null) return null;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size getSizeWithClosestRatio(Size[] sizes, int width, int height) {

        if (sizes == null) return null;

        double MIN_TOLERANCE = 100;
        double targetRatio = (double) height / width;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Size size : sizes) {
            double ratio = (double) size.height / size.width;

            if (Math.abs(ratio - targetRatio) < MIN_TOLERANCE) {
                MIN_TOLERANCE = Math.abs(ratio - targetRatio);
                minDiff = Double.MAX_VALUE;
            } else continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.width;
        int h = aspectRatio.height;
        for (Size option : choices) {
            if (option.height == option.width * h / w &&
                    option.width >= width && option.height >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new SizeComparator());
        } else {
            return null;
        }
    }

    private static double calculateApproximateVideoSize(CamcorderProfile camcorderProfile, int seconds) {
        return ((camcorderProfile.videoBitRate / (float) 1 + camcorderProfile.audioBitRate / (float) 1) * seconds) / (float) 8;
    }

    public static double calculateApproximateVideoDuration(CamcorderProfile camcorderProfile, long maxFileSize) {
        return 8 * maxFileSize / (camcorderProfile.videoBitRate + camcorderProfile.audioBitRate);
    }

    private static long calculateMinimumRequiredBitRate(CamcorderProfile camcorderProfile, long maxFileSize, int seconds) {
        return 8 * maxFileSize / seconds - camcorderProfile.audioBitRate;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(String cameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(cameraIdInt, maximumFileSize, minimumDurationInSeconds);
    }

    public static CamcorderProfile getCamcorderProfile(int currentCameraId, long maximumFileSize, int minimumDurationInSeconds) {
        if (maximumFileSize <= 0)
            return CamcorderProfile.get(currentCameraId, QUALITY_HIGH);

        int[] qualities = new int[]{QUALITY_HIGH,
                QUALITY_1080P, QUALITY_720P,
                QUALITY_480P, QUALITY_LOW};

        CamcorderProfile camcorderProfile;
        for (int i = 0; i < qualities.length; ++i) {
            camcorderProfile = CameraHelper.getCamcorderProfile(qualities[i], currentCameraId);
            double fileSize = CameraHelper.calculateApproximateVideoSize(camcorderProfile, minimumDurationInSeconds);

            if (fileSize > maximumFileSize) {
                long minimumRequiredBitRate = calculateMinimumRequiredBitRate(camcorderProfile, maximumFileSize, minimumDurationInSeconds);

                if (minimumRequiredBitRate >= camcorderProfile.videoBitRate / 4 && minimumRequiredBitRate <= camcorderProfile.videoBitRate) {
                    camcorderProfile.videoBitRate = (int) minimumRequiredBitRate;
                    return camcorderProfile;
                }
            } else return camcorderProfile;
        }
        return CameraHelper.getCamcorderProfile(QUALITY_LOW, currentCameraId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CamcorderProfile getCamcorderProfile(int mediaQuality, String cameraId) {
        if (TextUtils.isEmpty(cameraId)) {
            return null;
        }
        int cameraIdInt = Integer.parseInt(cameraId);
        return getCamcorderProfile(mediaQuality, cameraIdInt);
    }

    public static CamcorderProfile getCamcorderProfile(int mediaQuality, int cameraId) {
        if (Build.VERSION.SDK_INT > 10) {
            if (mediaQuality == QUALITY_HIGH) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            } else if (mediaQuality == QUALITY_1080P) {
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
                } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
                }
            } else if (mediaQuality == QUALITY_720P) {
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
                } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
                }
            } else if (mediaQuality == QUALITY_480P) {
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
                }
            } else if (mediaQuality == QUALITY_LOW) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            }
        } else {
            if (mediaQuality == QUALITY_HIGH) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            } else if (mediaQuality == QUALITY_1080P) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            } else if (mediaQuality == QUALITY_720P) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            } else if (mediaQuality == QUALITY_480P) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            } else if (mediaQuality == QUALITY_LOW) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
            }
        }
    }

    @ColorInt
    public static int darkenColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        color = Color.HSVToColor(hsv);
        return color;
    }

    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    public static int adjustAlpha(int color, @SuppressWarnings("SameParameterValue") float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class SizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.width * o1.height -
                    (long) o2.width * o2.height);
        }
    }

    public static class Size {
        public int width;
        public int height;
    }
}
