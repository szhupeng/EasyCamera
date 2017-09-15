package space.zhupeng.easycamera.album;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhupeng on 2017/9/15.
 */

public class MediaFolder implements Parcelable {

    private String folderName;
    private String folderPath;
    private String firstImagePath;
    private int imageCount;
    private int checkedCount;
    private boolean isChecked;
    private List<Media> images = new ArrayList<Media>();

    public MediaFolder() {
    }

    public MediaFolder(Parcel source) {
        this.folderName = source.readString();
        this.folderPath = source.readString();
        this.firstImagePath = source.readString();
        this.imageCount = source.readInt();
        this.checkedCount = source.readInt();
        this.isChecked = source.readByte() != 0;
        if (null == this.images) {
            this.images = new ArrayList<>();
        }
        source.readTypedList(this.images, Media.CREATOR);
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }

    public void setFirstImagePath(String firstImagePath) {
        this.firstImagePath = firstImagePath;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public int getCheckedCount() {
        return checkedCount;
    }

    public void setCheckedCount(int checkedCount) {
        this.checkedCount = checkedCount;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public List<Media> getImages() {
        return images;
    }

    public void setImages(List<Media> images) {
        this.images = images;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(folderName);
        dest.writeString(folderPath);
        dest.writeString(firstImagePath);
        dest.writeInt(imageCount);
        dest.writeInt(checkedCount);
        dest.writeByte((byte) (isChecked ? 1 : 0));
        dest.writeTypedList(images);
    }

    public static final Creator<MediaFolder> CREATOR = new Creator<MediaFolder>() {
        @Override
        public MediaFolder createFromParcel(Parcel source) {
            return new MediaFolder(source);
        }

        @Override
        public MediaFolder[] newArray(int size) {
            return new MediaFolder[size];
        }
    };
}
