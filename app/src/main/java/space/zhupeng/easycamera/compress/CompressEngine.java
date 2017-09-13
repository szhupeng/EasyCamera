package space.zhupeng.easycamera.compress;

import java.io.File;

/**
 * Created by zhupeng on 2017/8/31.
 */

public final class CompressEngine {

    private Compress mCompress;

    private CompressEngine(Compress compress) {
        this.mCompress = compress;
    }

    public static final CompressEngine with(final Compress compress) {
        return new CompressEngine(compress);
    }

    public CompressEngine file(final File file) {
        if (mCompress != null) {
            mCompress.file(file);
        }

        return this;
    }

    public void compress(final CompressResultListener listener) {
        if (null == mCompress) return;

        mCompress.compress(listener);
    }
}
