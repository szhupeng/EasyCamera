package space.zhupeng.easycamera.album;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by zhupeng on 2017/9/15.
 */

class Media implements Parcelable {

    public static final String DEFAULT_MIME_TYPE = "image/jpeg";

    private String mediaPath;
    private boolean isChecked;
    public int position;
    private String mimeType;
    private int width;
    private int height;
    private int checkOrder;

    public Media() {
    }

    public Media(String path, String mimeType, int width, int height) {
        this.mediaPath = path;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    private Media(Parcel source) {
        this.mediaPath = source.readString();
        this.isChecked = source.readByte() != 0;
        this.position = source.readInt();
        this.mimeType = source.readString();
        this.width = source.readInt();
        this.height = source.readInt();
        this.checkOrder = source.readInt();
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String path) {
        this.mediaPath = path;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getMimeType() {
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = DEFAULT_MIME_TYPE;
        }
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCheckOrder() {
        return checkOrder;
    }

    public void setCheckOrder(int checkOrder) {
        this.checkOrder = checkOrder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mediaPath);
        dest.writeByte((byte) (isChecked ? 1 : 0));
        dest.writeInt(position);
        dest.writeString(mimeType);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(checkOrder);
    }

    public static final Creator<Media> CREATOR = new Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel source) {
            return new Media(source);
        }

        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }
    };
}
