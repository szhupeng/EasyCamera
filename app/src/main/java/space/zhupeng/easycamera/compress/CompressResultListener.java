package space.zhupeng.easycamera.compress;

import java.util.List;

/**
 * Created by zhupeng on 2017/8/31.
 */

public interface CompressResultListener {
    void onSuccess(final List<String> paths);

    void onFailure(final String path, final Throwable throwable);
}
