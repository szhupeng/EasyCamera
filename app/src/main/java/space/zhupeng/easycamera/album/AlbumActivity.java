package space.zhupeng.easycamera.album;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import space.zhupeng.easycamera.R;
import space.zhupeng.easycamera.SystemBarTintManager;
import space.zhupeng.easycamera.utils.PictureUtils;

/**
 * Created by zhupeng on 2017/9/14.
 */

public class AlbumActivity extends AppCompatActivity implements View.OnClickListener, AlbumAdapter.OnAlbumActionListener, AlbumDirectoryAdapter.OnItemClickListener {

    public static void toHere(Activity activity, AlbumConfig config, int requestCode) {
        Intent intent = new Intent(activity, AlbumActivity.class);
        if (config != null) {
            intent.putExtra("config", config);
        }
        activity.startActivityForResult(intent, requestCode);
    }

    private AlbumConfig mAlbumConfig;

    private FolderPopWindow mFolderPop;

    private View rlAlbumTitle;
    private ImageView ivBack;
    private TextView tvTitle;
    private TextView tvRight;

    private RecyclerView rvPictures;

    private RelativeLayout rlBottom;
    private TextView tvPreview;
    private TextView tvSelectedCount;
    private TextView tvDone;

    private TextView tvEmpty;

    private MediaLoader mLoader;

    private AlbumAdapter mAdapter;

    private List<Media> mImages = new ArrayList<>();
    private List<MediaFolder> mMediaFolders = new ArrayList<>();

    private String mCameraPath;

    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_album;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        SystemBarTintManager.tint(this);

        if (null == savedInstanceState) {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("config")) {
                mAlbumConfig = intent.getParcelableExtra("config");
            }
        } else {
            mAlbumConfig = savedInstanceState.getParcelable("config");
        }

        if (null == mAlbumConfig) {
            mAlbumConfig = new AlbumConfig();
        }

        initView();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable("config", mAlbumConfig);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void initView() {
        rlAlbumTitle = findViewById(R.id.rl_album_title);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvRight = (TextView) findViewById(R.id.tv_right);
        rvPictures = (RecyclerView) findViewById(R.id.rv_pictures);
        rlBottom = (RelativeLayout) findViewById(R.id.rl_bottom);
        tvPreview = (TextView) findViewById(R.id.tv_preview);
        tvSelectedCount = (TextView) findViewById(R.id.tv_selected_count);
        tvDone = (TextView) findViewById(R.id.tv_done);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);

        if (mAlbumConfig.getBackgroundResId() != 0) {
            findViewById(R.id.rl_root).setBackgroundResource(mAlbumConfig.getBackgroundResId());
        }

        ivBack.setOnClickListener(this);
        tvTitle.setOnClickListener(this);
        tvRight.setOnClickListener(this);
        rlBottom.setVisibility(mAlbumConfig.getSelectMode() == AlbumConfig.SINGLE ? View.GONE : View.VISIBLE);
        if (mAlbumConfig.isPreviewEnable()) {
            tvPreview.setOnClickListener(this);
        }

        mFolderPop = new FolderPopWindow(this);
        mFolderPop.setPictureTitleView(tvTitle);
        mFolderPop.setOnItemClickListener(this);

        rvPictures.setHasFixedSize(true);
        rvPictures.addItemDecoration(new SpaceItemDecoration(mAlbumConfig.getImageSpan(), dp2px(2), false));
        rvPictures.setLayoutManager(new GridLayoutManager(this, mAlbumConfig.getImageSpan()));
        ((SimpleItemAnimator) rvPictures.getItemAnimator()).setSupportsChangeAnimations(false);

        mLoader = new MediaLoader(this);

        mAdapter = new AlbumAdapter(this, mAlbumConfig);
        mAdapter.setOnAlbumActionListener(this);
        rvPictures.setAdapter(mAdapter);

        loadMediaData();
    }

    protected void loadMediaData() {
        mLoader.loadMediaAsync(new MediaLoader.LoadListener() {
            @Override
            public void onComplete(List<MediaFolder> folders) {
                if (folders.size() > 0) {
                    mMediaFolders = folders;
                    MediaFolder folder = folders.get(0);
                    folder.setChecked(true);
                    List<Media> images = folder.getImages();
                    // 这里解决有些机型会出现拍照完，相册列表不及时刷新问题
                    // 因为onActivityResult里手动添加拍照后的照片，
                    // 如果查询出来的图片大于或等于当前adapter集合的图片则取更新后的，否则就取本地的
                    if (images.size() >= mImages.size()) {
                        mImages = images;
                        mFolderPop.setFolders(folders);
                    }
                }
                if (mAdapter != null) {
                    if (null == mImages) {
                        mImages = new ArrayList<>();
                    }
                    mAdapter.setImages(mImages);
                    tvEmpty.setVisibility(mImages.size() > 0 ? View.INVISIBLE : View.VISIBLE);
                }
            }
        });
    }

    /**
     * 手动添加拍照后的相片到图片列表，并设为选中
     *
     * @param media
     */
    private void manualSaveFolder(Media media) {
        try {
            createNewFolder(mMediaFolders);
            MediaFolder folder = getImageFolder(media.getMediaPath(), mMediaFolders);
            MediaFolder cameraFolder = mMediaFolders.size() > 0 ? mMediaFolders.get(0) : null;
            if (cameraFolder != null && folder != null) {
                // 相机
                cameraFolder.setFirstImagePath(media.getMediaPath());
                cameraFolder.setImages(mImages);
                cameraFolder.setImageCount(cameraFolder.getImageCount() + 1);
                // 拍照相册
                int num = folder.getImageCount() + 1;
                folder.setImageCount(num);
                folder.getImages().add(0, media);
                folder.setFirstImagePath(mCameraPath);
                mFolderPop.setFolders(mMediaFolders);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将图片插入到相机文件夹中
     *
     * @param path
     * @param folders
     * @return
     */
    protected MediaFolder getImageFolder(String path, List<MediaFolder> folders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();

        for (MediaFolder folder : folders) {
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_back || id == R.id.tv_right) {
            if (mFolderPop.isShowing()) {
                mFolderPop.dismiss();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }

        if (id == R.id.tv_title) {
            if (mFolderPop.isShowing()) {
                mFolderPop.dismiss();
            } else {
                if (mImages != null && mImages.size() > 0) {
                    mFolderPop.showAsDropDown(rlAlbumTitle);
                    List<Media> selectedImages = mAdapter.getSelectedImages();
                    mFolderPop.notifyCheckedChanged(selectedImages);
                }
            }
        }

        if (id == R.id.tv_preview) {
            ArrayList<Media> selectedImages = (ArrayList<Media>) mAdapter.getSelectedImages();

            ArrayList<Media> medias = new ArrayList<>();
            for (Media media : selectedImages) {
                medias.add(media);
            }

            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("previewSelect", medias);
            bundle.putSerializable("selectedImages", selectedImages);
            bundle.putBoolean("showBottom", true);
            PicturePreviewActivity.toHere(AlbumActivity.this, bundle, 1);
        }

        if (id == R.id.ll_ok) {
            List<Media> images = mAdapter.getSelectedImages();
            // 如果设置了图片最小选择数量，则判断是否满足条件
            int size = images.size();
        }
    }

    @Override
    public void onTakePhoto() {

    }

    @Override
    public void onChange(List<Media> selectImages) {
        changeImageNumber(selectImages);
    }

    @Override
    public void onPictureClick(Media media, int position) {
        List<Media> images = mAdapter.getImages();
//        startPreview(images, position);
    }

    @Override
    public void onItemClick(String folderName, List<Media> images) {
        tvTitle.setText(folderName);
        mAdapter.setImages(images);
        mFolderPop.dismiss();
    }

    public void changeImageNumber(List<Media> selectImages) {
        boolean enable = selectImages.size() != 0;
        if (enable) {
            tvPreview.setEnabled(true);
            tvSelectedCount.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(Integer.toString(selectImages.size()));
            tvDone.setText("完成");
        } else {
        }
    }

    /**
     * 如果没有任何相册，先创建一个最近相册出来
     *
     * @param folders
     */
    protected void createNewFolder(List<MediaFolder> folders) {
        if (folders.size() == 0) {
            // 没有相册 先创建一个最近相册出来
            MediaFolder folder = new MediaFolder();
            folder.setFolderName("相机");
            folder.setFolderPath("");
            folder.setFirstImagePath("");
            folders.add(folder);
        }
    }

    protected int dp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            List<Media> medias = new ArrayList<>();
            Media media = new Media();
            String imageType;
            // on take photo success
            final File file = new File(mCameraPath);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            int degree = PictureUtils.readPictureDegree(file.getAbsolutePath());
            rotateImage(degree, file);
            // 生成新拍照片
            media.setMediaPath(mCameraPath);
            media.setMimeType(createImageType(mCameraPath));

            // 因为加入了单独拍照功能，所有如果是单独拍照的话也默认为单选状态
            if (AlbumConfig.SINGLE == mAlbumConfig.getSelectMode() || mAlbumConfig.isShowCamera()) {
                // 如果是单选，拍照后直接返回
                medias.add(media);
                onResult(medias);
            } else {
                // 多选，返回列表并选中当前拍照的
                mImages.add(0, media);
                if (mAdapter != null) {
                    List<Media> selectedImages = mAdapter.getSelectedImages();
                    // 没有到最大选择量,才做默认选中刚拍好的
                    if (selectedImages.size() < mAlbumConfig.getSelectableCount()) {
                        // 类型相同或还没有选中才加进选中集合中
                        selectedImages.add(media);
                        mAdapter.setSelectedImages(selectedImages);
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }

            if (mAdapter != null) {
                // 解决部分手机拍照完Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
                // 不及时刷新问题手动添加
                manualSaveFolder(media);
                tvEmpty.setVisibility(mImages.size() > 0 ? View.INVISIBLE : View.VISIBLE);
            }

            int lastImageId = getLastImageId();
            if (lastImageId != -1) {
                removeImage(lastImageId);
            }
        }
    }

    protected void onResult(List<Media> images) {
        if (mAlbumConfig.isShowCamera() && mAlbumConfig.getSelectMode() == AlbumConfig.MULTIPLE && mAdapter.getSelectedImages() != null) {
            images.addAll(mAdapter.getSelectedImages());
        }
        Intent intent = new Intent();
        intent.putExtra("medias", (Serializable) mImages);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 判断拍照 图片是否旋转
     *
     * @param degree
     * @param file
     */
    protected void rotateImage(int degree, File file) {
        if (degree > 0) {
            // 针对相片有旋转问题的处理方式
            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();//获取缩略图显示到屏幕上
                opts.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                Bitmap bmp = PictureUtils.rotateBitmap(degree, bitmap);
                PictureUtils.saveBitmapFile(bmp, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String createImageType(String path) {
        try {
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                String fileName = file.getName();
                int last = fileName.lastIndexOf(".") + 1;
                String temp = fileName.substring(last, fileName.length());
                return "image/" + temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "image/jpeg";
        }
        return "image/jpeg";
    }

    /**
     * 获取DCIM文件下最新一条拍照记录
     *
     * @return
     */
    protected int getLastImageId() {
        try {
            //selection: 指定查询条件
            String absolutePath = PictureUtils.getDCIMCameraPath();
            String ORDER_BY = MediaStore.Files.FileColumns._ID + " DESC";
            String selection = MediaStore.Images.Media.DATA + " like ?";
            //定义selectionArgs：
            String[] selectionArgs = {absolutePath + "%"};
            Cursor imageCursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                    selection, selectionArgs, ORDER_BY);
            if (imageCursor.moveToFirst()) {
                int id = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
                long date = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));
                int duration = dateDiffer(date);
                imageCursor.close();
                // DCIM文件下最近时间30s以内的图片，可以判定是最新生成的重复照片
                return duration <= 30 ? id : -1;
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 判断两个时间戳相差多少秒
     *
     * @param d
     * @return
     */
    public int dateDiffer(long d) {
        try {
            long l1 = Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(0, 10));
            long interval = l1 - d;
            return (int) Math.abs(interval);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * 删除部分手机 拍照在DCIM也生成一张的问题
     *
     * @param id
     */
    protected void removeImage(int id) {
        try {
            ContentResolver cr = getContentResolver();
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Images.Media._ID + "=?";
            cr.delete(uri, selection, new String[]{Long.toString(id)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
