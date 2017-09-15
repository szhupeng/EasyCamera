package space.zhupeng.easycamera.album;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

import space.zhupeng.easycamera.R;
import space.zhupeng.easycamera.SystemBarTintManager;
import space.zhupeng.easycamera.widget.PreviewViewPager;


public class PicturePreviewActivity extends AppCompatActivity implements View.OnClickListener, Animation.AnimationListener {

    public static void toHere(Activity activity, Bundle args, int requestCode) {
        Intent intent = new Intent(activity, PicturePreviewActivity.class);
        if (args != null) {
            intent.putExtra("args", args);
        }
        activity.startActivityForResult(intent, requestCode);
    }

    private ImageView ivBack;
    private TextView tvSelectOrder, tvTitle, tvOk;
    private PreviewViewPager vpPreview;
    private LinearLayout llOk;
    private int position;
    private LinearLayout llCheck;
    private List<Media> mImages = new ArrayList<>();
    private List<Media> mSelectedImages = new ArrayList<>();
    private TextView tvCheck;
    private SimpleFragmentAdapter mAdapter;
    private Animation animation;
    private boolean refresh;
    private int index;

    private int mSelectableCount;
    private boolean isShowCheckOrder;

    private boolean previewEggs = true;

    private int screenWidth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;

        SystemBarTintManager.tint(this);

        animation = AnimationUtils.loadAnimation(this, R.anim.modal_in);
        animation.setAnimationListener(this);

        ivBack = (ImageView) findViewById(R.id.iv_back);
        vpPreview = (PreviewViewPager) findViewById(R.id.vp_preview);
        llCheck = (LinearLayout) findViewById(R.id.ll_check);
        llOk = (LinearLayout) findViewById(R.id.ll_ok);
        tvCheck = (TextView) findViewById(R.id.tv_check);
        ivBack.setOnClickListener(this);
        tvOk = (TextView) findViewById(R.id.tv_ok);
        llOk.setOnClickListener(this);
        tvSelectOrder = (TextView) findViewById(R.id.tv_selected_order);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        position = getIntent().getIntExtra("position", 0);

        mSelectedImages = (List<Media>) getIntent().getSerializableExtra("selectedImages");
        initViewPageAdapterData();
        llCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mImages != null && mImages.size() > 0) {
                    Media image = mImages.get(vpPreview.getCurrentItem());
                    // 刷新图片列表中图片状态
                    boolean isChecked;
                    if (!tvCheck.isSelected()) {
                        isChecked = true;
                        tvCheck.setSelected(true);
                        tvCheck.startAnimation(animation);
                    } else {
                        isChecked = false;
                        tvCheck.setSelected(false);
                    }

                    if (mSelectedImages.size() >= mSelectableCount && isChecked) {
                        Toast.makeText(PicturePreviewActivity.this, getString(R.string.reach_max_prompt, mSelectableCount), Toast.LENGTH_SHORT).show();
                        tvCheck.setSelected(false);
                        return;
                    }
                    if (isChecked) {
                        mSelectedImages.add(image);
                        image.setCheckOrder(mSelectedImages.size());
                        if (isShowCheckOrder) {
                            tvCheck.setText(image.getCheckOrder() + "");
                        }
                    } else {
                        for (Media media : mSelectedImages) {
                            if (media.getMediaPath().equals(image.getMediaPath())) {
                                mSelectedImages.remove(media);
                                subSelectPosition();
                                notifyCheckChanged(media);
                                break;
                            }
                        }
                    }
                    onSelectNumChange(true);
                }
            }
        });
        vpPreview.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                isPreviewEggs(previewEggs, position, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int i) {
                position = i;
                tvTitle.setText(TextUtils.concat(Integer.toString(position + 1), "/", Integer.toString(mImages.size())));
                Media media = mImages.get(position);
                index = media.getPosition();
                if (!previewEggs) {
                    if (isShowCheckOrder) {
                        tvCheck.setText(media.getCheckOrder() + "");
                        notifyCheckChanged(media);
                    }
                    onImageChecked(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * 这里没实际意义，好处是预览图片时 滑动到屏幕一半以上可看到下一张图片是否选中了
     *
     * @param previewEggs          是否显示预览友好体验
     * @param positionOffsetPixels 滑动偏移量
     */
    private void isPreviewEggs(boolean previewEggs, int position, int positionOffsetPixels) {
        if (previewEggs) {
            if (mImages.size() > 0 && mImages != null) {
                Media media;
                int num;
                if (positionOffsetPixels < screenWidth / 2) {
                    media = mImages.get(position);
                    tvCheck.setSelected(isSelected(media));
                    if (isShowCheckOrder) {
                        num = media.getCheckOrder();
                        tvCheck.setText(num + "");
                        notifyCheckChanged(media);
                        onImageChecked(position);
                    }
                } else {
                    media = mImages.get(position + 1);
                    tvCheck.setSelected(isSelected(media));
                    if (isShowCheckOrder) {
                        num = media.getCheckOrder();
                        tvCheck.setText(num + "");
                        notifyCheckChanged(media);
                        onImageChecked(position + 1);
                    }
                }
            }
        }
    }

    private void initViewPageAdapterData() {
        tvTitle.setText(position + 1 + "/" + mImages.size());
        mAdapter = new SimpleFragmentAdapter();
        vpPreview.setAdapter(mAdapter);
        vpPreview.setCurrentItem(position);
        onSelectNumChange(false);
        onImageChecked(position);
        if (mImages.size() > 0) {
            Media media = mImages.get(position);
            index = media.getPosition();
            if (isShowCheckOrder) {
                tvSelectOrder.setSelected(true);
                tvCheck.setText(media.getCheckOrder() + "");
                notifyCheckChanged(media);
            }
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(Media image) {
        if (isShowCheckOrder) {
            tvCheck.setText("");
            for (Media media : mSelectedImages) {
                if (media.getMediaPath().equals(image.getMediaPath())) {
                    image.setCheckOrder(media.getCheckOrder());
                    tvCheck.setText(String.valueOf(image.getCheckOrder()));
                }
            }
        }
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = mSelectedImages.size(); index < len; index++) {
            Media media = mSelectedImages.get(index);
            media.setCheckOrder(index + 1);
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (mImages != null && mImages.size() > 0) {
            Media media = mImages.get(position);
            tvCheck.setSelected(isSelected(media));
        } else {
            tvCheck.setSelected(false);
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    public boolean isSelected(Media image) {
        for (Media media : mSelectedImages) {
            if (media.getMediaPath().equals(image.getMediaPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新图片选择数量
     */

    public void onSelectNumChange(boolean isRefresh) {
        this.refresh = isRefresh;
        boolean enable = mSelectedImages.size() != 0;
        if (enable) {
            llOk.setEnabled(true);
//            if (numComplete) {
//                tvOk.setText(getString(R.string.picture_done_front_num, selectImages.size(), maxSelectNum));
//            } else {
//                if (refresh) {
//                    tvSelectOrder.startAnimation(animation);
//                }
//                tvSelectOrder.setVisibility(View.VISIBLE);
//                tvSelectOrder.setText(Integer.toString(mSelectedImages.size()));
//                tvOk.setText(getString(R.string.picture_completed));
//            }
        } else {
            llOk.setEnabled(false);
            tvOk.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
//            if (numComplete) {
//                tv_ok.setText(getString(R.string.picture_done_front_num, 0, maxSelectNum));
//            } else {
//                tv_img_num.setVisibility(View.INVISIBLE);
//                tv_ok.setText(getString(R.string.picture_please_select));
//            }
        }
        updateSelector(refresh);
    }

    /**
     * 更新图片列表选中效果
     *
     * @param isRefresh
     */
    private void updateSelector(boolean isRefresh) {
        if (isRefresh) {
//            EventEntity obj = new EventEntity(PictureConfig.UPDATE_FLAG, selectImages, index);
//            RxBus.getDefault().post(obj);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        updateSelector(refresh);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }


    public class SimpleFragmentAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            (container).removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View contentView = LayoutInflater.from(PicturePreviewActivity.this).inflate(R.layout.item_picture_preview, container, false);
            final PhotoView imageView = (PhotoView) contentView.findViewById(R.id.iv_preview);
            Media media = mImages.get(position);
            if (media != null) {
                String path = media.getMediaPath();
                boolean isGif = isGif(media.getMimeType());
                // 压缩过的gif就不是gif了
                if (isGif) {
                    RequestOptions gifOptions = new RequestOptions()
                            .override(480, 800)
                            .priority(Priority.HIGH)
                            .diskCacheStrategy(DiskCacheStrategy.NONE);
                    Glide.with(PicturePreviewActivity.this)
                            .asGif()
                            .load(path)
                            .apply(gifOptions)
                            .into(imageView);
                } else {
                    RequestOptions options = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(480, 800);
                    Glide.with(PicturePreviewActivity.this)
                            .asBitmap()
                            .load(path)
                            .apply(options)
                            .into(imageView);
                }

                imageView.setOnPhotoTapListener(new OnPhotoTapListener() {
                    @Override
                    public void onPhotoTap(ImageView view, float x, float y) {
                        finish();
                    }
                });
            }
            (container).addView(contentView, 0);
            return contentView;
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.iv_back) {
            finish();
        }
        if (id == R.id.ll_ok) {
            // 如果设置了图片最小选择数量，则判断是否满足条件
            int size = mSelectedImages.size();
        }
    }

    private boolean isGif(String mimeType) {
        return "image/gif".equals(mimeType) || "image/GIF".equals(mimeType);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }
}
