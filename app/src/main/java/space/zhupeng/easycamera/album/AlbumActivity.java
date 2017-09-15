package space.zhupeng.easycamera.album;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import space.zhupeng.easycamera.R;
import space.zhupeng.easycamera.SystemBarTintManager;

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

        } else {
        }
    }

    protected int dp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
