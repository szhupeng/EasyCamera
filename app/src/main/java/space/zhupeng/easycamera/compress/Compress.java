package space.zhupeng.easycamera.compress;

import java.io.File;

/**
 * Created by zhupeng on 2017/8/31.
 */

public abstract class Compress {
    private File mOriginFile;

    public Compress file(final File file) {
        this.mOriginFile = file;
        return this;
    }

    public void compress(final CompressResultListener listener) {
        compress(this.mOriginFile, listener);
    }

    protected abstract void compress(final File file, final CompressResultListener listener);
}
