package space.zhupeng.easycamera;

/**
 * Created by zhupeng on 2017/9/14.
 */

public final class VideoConfig {
    public String mDirPath;  //The path of the video to save
    public String mFileName;  //The name of the video file
    public long mMaxSize;   //The max size of the video to record
    public long mLimitTime;  //The limit time of the video to record

    public VideoConfig(String dirPath, String fileName) {
        this.mDirPath = dirPath;
        this.mFileName = fileName;
    }
}
