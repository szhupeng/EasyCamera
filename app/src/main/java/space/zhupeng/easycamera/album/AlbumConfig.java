package space.zhupeng.easycamera.album;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by zhupeng on 2017/9/14.
 */

@SuppressWarnings("all")
public class AlbumConfig implements Parcelable {

    public static final int MULTIPLE = 0;  //单选
    public static final int SINGLE = 1;   //多选

    @IntDef({MULTIPLE, SINGLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SelectMode {
    }

    private boolean isShowCamera;
    private int mSelectableCount = 1;  //可选图片数目
    private int mImageSpan = 4;       //每行显示的图片个数
    private int mBackgroundResId;  //相册背景
    @SelectMode
    private int mSelectMode = SINGLE;   //选择模式：单选，多选
    private boolean isPreviewEnable = true;  //是否允许预览
    private boolean isZoomEnable = true;   //是否允许缩放动画
    private boolean isShowCheckOrder = false; //是否显示勾选图片的数字顺序

    public AlbumConfig() {
    }

    public boolean isShowCamera() {
        return isShowCamera;
    }

    public void setShowCamera(boolean show) {
        isShowCamera = show;
    }

    public int getSelectableCount() {
        return mSelectableCount;
    }

    public void setSelectableCount(int count) {
        this.mSelectableCount = Math.max(count, 1);
    }

    public int getImageSpan() {
        return mImageSpan;
    }

    public void setImageSpan(int imageSpan) {
        if (imageSpan <= 0) {
            imageSpan = 4;
        }
        this.mImageSpan = imageSpan;
    }

    public int getBackgroundResId() {
        return mBackgroundResId;
    }

    public void setBackgroundResId(int resId) {
        this.mBackgroundResId = resId;
    }

    public int getSelectMode() {
        return mSelectMode;
    }

    public void setSelectMode(@SelectMode int selectMode) {
        this.mSelectMode = selectMode;
    }

    public boolean isPreviewEnable() {
        return isPreviewEnable;
    }

    public void setPreviewEnable(boolean enable) {
        isPreviewEnable = enable;
    }

    public boolean isZoomEnable() {
        return isZoomEnable;
    }

    public void setZoomEnable(boolean enable) {
        isZoomEnable = enable;
    }

    public boolean isShowCheckOrder() {
        return isShowCheckOrder;
    }

    public void setShowCheckOrder(boolean show) {
        isShowCheckOrder = show;
    }

    private AlbumConfig(Parcel source) {
        isShowCamera = source.readByte() != 0;
        mSelectableCount = source.readInt();
        mImageSpan = source.readInt();
        mBackgroundResId = source.readInt();
        mSelectMode = source.readInt();
        isPreviewEnable = source.readByte() != 0;
        isZoomEnable = source.readByte() != 0;
        isShowCheckOrder = source.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isShowCamera ? 1 : 0));
        dest.writeInt(mSelectableCount);
        dest.writeInt(mImageSpan);
        dest.writeInt(mBackgroundResId);
        dest.writeInt(mSelectMode);
        dest.writeByte((byte) (isPreviewEnable ? 1 : 0));
        dest.writeByte((byte) (isZoomEnable ? 1 : 0));
        dest.writeByte((byte) (isShowCheckOrder ? 1 : 0));
    }

    public static final Creator<AlbumConfig> CREATOR = new Creator<AlbumConfig>() {
        @Override
        public AlbumConfig createFromParcel(Parcel source) {
            return new AlbumConfig(source);
        }

        @Override
        public AlbumConfig[] newArray(int size) {
            return new AlbumConfig[size];
        }
    };
}
