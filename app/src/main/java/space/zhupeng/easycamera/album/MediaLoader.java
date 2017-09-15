package space.zhupeng.easycamera.album;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by zhupeng on 2017/9/15.
 */

public class MediaLoader implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 1;
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");

    private final static String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
    };

    /**
     * 查询图片条件
     */
    private final static String CONDITION =
            "(" + MediaStore.Images.Media.MIME_TYPE + "=? or "
                    + MediaStore.Images.Media.MIME_TYPE + "=?" + " or "
                    + MediaStore.Images.Media.MIME_TYPE + "=?" + " or "
                    + MediaStore.Images.Media.MIME_TYPE + "=?)" + " AND "
                    + MediaStore.MediaColumns.WIDTH + ">0";

    private final static String[] MIME_TYPE = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    };

    private FragmentActivity mActivity;
    private LoadListener mLoadListener;

    public MediaLoader(FragmentActivity activity) {
        this.mActivity = activity;
    }

    public void loadMediaAsync(final LoadListener listener) {
        this.mLoadListener = listener;
        mActivity.getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mActivity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION, CONDITION, MIME_TYPE, IMAGE_PROJECTION[0] + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        try {
            List<MediaFolder> imageFolders = new ArrayList<>();
            MediaFolder allImageFolder = new MediaFolder();
            List<Media> latelyImages = new ArrayList<>();
            if (data != null) {
                int count = data.getCount();
                if (count > 0) {
                    data.moveToFirst();
                    do {
                        String path = data.getString
                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                        String pictureType = data.getString
                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[6]));
                        boolean eqImg = pictureType.startsWith("image");
                        int w = eqImg ? data.getInt
                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[4])) : 0;
                        int h = eqImg ? data.getInt
                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[5])) : 0;
                        Media image = new Media(path, pictureType, w, h);

                        MediaFolder folder = getImageFolder(path, imageFolders);
                        List<Media> images = folder.getImages();
                        images.add(image);
                        folder.setImageCount(folder.getImageCount() + 1);
                        latelyImages.add(image);
                        int imageCount = allImageFolder.getImageCount();
                        allImageFolder.setImageCount(imageCount + 1);
                    } while (data.moveToNext());

                    if (latelyImages.size() > 0) {
                        sortFolder(imageFolders);
                        imageFolders.add(0, allImageFolder);
                        allImageFolder.setFirstImagePath
                                (latelyImages.get(0).getMediaPath());
                        allImageFolder.setFolderName("所有图片");
                        allImageFolder.setImages(latelyImages);
                    }
                    if (this.mLoadListener != null) {
                        this.mLoadListener.onComplete(imageFolders);
                    }
                } else {
                    // 如果没有相册
                    if (this.mLoadListener != null) {
                        this.mLoadListener.onComplete(imageFolders);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    private void sortFolder(List<MediaFolder> folders) {
        // 文件夹按图片数量排序
        Collections.sort(folders, new Comparator<MediaFolder>() {
            @Override
            public int compare(MediaFolder o1, MediaFolder o2) {
                if (o1.getImages() == null || o2.getImages() == null) {
                    return 0;
                }

                return Integer.signum(o2.getImageCount() - o1.getImageCount());
            }
        });
    }

    private MediaFolder getImageFolder(String path, List<MediaFolder> folders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();
        for (MediaFolder folder : folders) {
            // 同一个文件夹下，返回自己，否则创建新文件夹
            if (folder.getFolderName().equals(folderFile.getName())) {
                return folder;
            }
        }
        MediaFolder newFolder = new MediaFolder();
        newFolder.setFolderName(folderFile.getName());
        newFolder.setFolderPath(folderFile.getAbsolutePath());
        newFolder.setFirstImagePath(path);
        folders.add(newFolder);
        return newFolder;
    }

    public interface LoadListener {
        void onComplete(List<MediaFolder> folders);
    }
}
