package space.zhupeng.easycamera;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by zhupeng on 2017/9/12.
 */

public class ImageSaver implements Runnable {

    interface SaveListener {
        void onSuccess(File file);

        void onError(Exception e);
    }

    private String mDirPath;
    private String mFileName;
    private byte[] mMetaData;

    private SaveListener mSaveListener;

    public ImageSaver(final byte[] data, String dirPath, String fileName, final SaveListener listener) {
        this.mDirPath = dirPath;
        this.mFileName = fileName;
        this.mMetaData = data;
        this.mSaveListener = listener;
    }

    @Override
    public void run() {
        if (null == mMetaData) return;

        if (TextUtils.isEmpty(mDirPath)) {
            mDirPath = Environment.getExternalStorageDirectory().getPath();
        }

        if (TextUtils.isEmpty(mFileName)) {
            mFileName = TextUtils.concat("IMG_", Long.toString(System.currentTimeMillis()), ".jpg").toString();
        }

        final File file = new File(mDirPath, mFileName);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(mMetaData);
            os.close();
            if (mSaveListener != null) {
                mSaveListener.onSuccess(file);
            }
        } catch (IOException e) {
            if (mSaveListener != null) {
                mSaveListener.onError(e);
            } else {
                e.printStackTrace();
            }
        } finally {
            this.mMetaData = null;
            this.mSaveListener = null;
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}
