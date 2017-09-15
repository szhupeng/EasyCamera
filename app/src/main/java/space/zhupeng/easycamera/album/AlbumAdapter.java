package space.zhupeng.easycamera.album;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import space.zhupeng.easycamera.R;

/**
 * Created by zhupeng on 2017/9/14.
 */

public class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnAlbumActionListener {
        void onTakePhoto();

        void onChange(List<Media> selectImages);

        void onPictureClick(Media media, int position);
    }

    private final static int DURATION = 450;

    private LayoutInflater mInflater;
    private Context context;

    private int mSelectableCount = 1;
    private boolean isShowCamera = true;
    private boolean isPreviewEnable;
    private int mSelectMode = AlbumConfig.MULTIPLE;
    private boolean isZoomEnable;
    private boolean isShowCheckOrder;
    private Animation animation;

    private OnAlbumActionListener mOnActionListener;
    private List<Media> mAllImages = new ArrayList<Media>();
    private List<Media> mSelectedImages = new ArrayList<Media>();

    public AlbumAdapter(final Context context, final AlbumConfig config) {
        super();

        this.context = context;
        this.mInflater = LayoutInflater.from(context);

        this.mSelectableCount = config.getSelectableCount();
        this.isShowCamera = config.isShowCamera();
        this.isPreviewEnable = config.isPreviewEnable();
        this.mSelectMode = config.getSelectMode();
        this.isShowCheckOrder = config.isShowCheckOrder();
        this.isZoomEnable = config.isZoomEnable();

        animation = AnimationUtils.loadAnimation(context, R.anim.modal_in);
    }

    public void setImages(List<Media> images) {
        this.mAllImages = images;
        notifyDataSetChanged();
    }

    public void setSelectedImages(List<Media> images) {
        List<Media> selectImages = new ArrayList<>();
        for (Media media : images) {
            selectImages.add(media);
        }
        this.mSelectedImages = selectImages;
        updateCheckOrder();
        if (mOnActionListener != null) {
            mOnActionListener.onChange(mSelectedImages);
        }
    }

    public List<Media> getSelectedImages() {
        if (null == mSelectedImages) {
            mSelectedImages = new ArrayList<>();
        }
        return mSelectedImages;
    }

    public List<Media> getImages() {
        if (null == mAllImages) {
            mAllImages = new ArrayList<>();
        }
        return mAllImages;
    }

    /**
     * 更新选择的顺序
     */
    private void updateCheckOrder() {
        if (isShowCheckOrder) {
            final int size = mSelectedImages.size();
            for (int index = 0; index < size; index++) {
                Media media = mSelectedImages.get(index);
                media.setCheckOrder(index + 1);
                notifyItemChanged(media.position);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isShowCamera && position == 0) {
            return 1;
        } else {
            return 2;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (1 == viewType) {
            return new CameraHolder(mInflater.inflate(R.layout.item_album_camera, parent, false));
        }
        return new ImageHolder(mInflater.inflate(R.layout.item_album, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (1 == getItemViewType(position)) {
            CameraHolder viewHolder = (CameraHolder) holder;
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnActionListener != null) {
                        mOnActionListener.onTakePhoto();
                    }
                }
            });
        } else {
            final ImageHolder viewHolder = (ImageHolder) holder;
            final Media image = mAllImages.get(isShowCamera ? position - 1 : position);
            image.position = viewHolder.getAdapterPosition();
            final String path = image.getMediaPath();
            final String mimeType = image.getMimeType();
            viewHolder.flCheck.setVisibility(mSelectMode == AlbumConfig.SINGLE ? View.GONE : View.VISIBLE);
            if (isShowCheckOrder) {
                notifyCheckChanged(viewHolder, image);
            }
            selectImage(viewHolder, isSelected(image), false);
            viewHolder.tvGif.setVisibility(isGif(image.getMimeType()) ? View.VISIBLE : View.GONE);
            int width = image.getWidth();
            int height = image.getHeight();
            int h = width * 5;
            viewHolder.tvLongChart.setVisibility(height > h ? View.VISIBLE : View.GONE);

            RequestOptions options = new RequestOptions();
            options.sizeMultiplier(0.5f);
            options.diskCacheStrategy(DiskCacheStrategy.ALL);
            options.centerCrop();
            options.placeholder(R.drawable.placeholder);
            Glide.with(context)
                    .asBitmap()
                    .load(path)
                    .apply(options)
                    .into(viewHolder.ivPicture);

            if (isPreviewEnable) {
                viewHolder.flCheck.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeCheckboxState(viewHolder, image);
                    }
                });
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 如原图路径不存在或者路径存在但文件不存在
                    if (!new File(path).exists()) {
                        Toast.makeText(context, context.getString(R.string.picture_error), Toast.LENGTH_LONG)
                                .show();
                        return;
                    }

                    if (isPreviewEnable || AlbumConfig.MULTIPLE == mSelectMode) {
                        int index = isShowCamera ? position - 1 : position;
                        if (mOnActionListener != null) {
                            mOnActionListener.onPictureClick(image, index);
                        }
                    } else {
                        changeCheckboxState(viewHolder, image);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        int size = 0;
        if (mAllImages != null) {
            size = mAllImages.size();
        }
        return isShowCamera ? size + 1 : size;
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(final ImageHolder holder, final Media image) {
        holder.tvCheck.setText(null);
        for (Media media : mSelectedImages) {
            if (media.getMediaPath().equals(image.getMediaPath())) {
                image.setCheckOrder(media.getCheckOrder());
                media.setPosition(image.getPosition());
                holder.tvCheck.setText(Integer.toString(image.getCheckOrder()));
            }
        }
    }

    public void selectImage(ImageHolder holder, boolean isChecked, boolean isAnim) {
        holder.tvCheck.setSelected(isChecked);
        if (isChecked) {
            if (isAnim) {
                if (animation != null) {
                    holder.tvCheck.startAnimation(animation);
                }
            }
            holder.ivPicture.setColorFilter(ContextCompat.getColor(context, R.color.selected_overlay), PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.ivPicture.setColorFilter(ContextCompat.getColor(context, R.color.unselected_overlay), PorterDuff.Mode.SRC_ATOP);
        }
    }

    public boolean isSelected(final Media image) {
        for (Media media : mSelectedImages) {
            if (media.getMediaPath().equals(image.getMediaPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是gif
     *
     * @param mimeType
     * @return
     */
    private boolean isGif(String mimeType) {
        return "image/gif".equals(mimeType) || "image/GIF".equals(mimeType);
    }

    /**
     * 改变图片选中状态
     *
     * @param holder
     * @param image
     */

    private void changeCheckboxState(ImageHolder holder, final Media image) {
        boolean isChecked = holder.tvCheck.isSelected();

        if (mSelectedImages.size() >= mSelectableCount && !isChecked) {
            Toast.makeText(context, context.getString(R.string.reach_max_prompt, mSelectableCount), Toast.LENGTH_LONG).show();
            return;
        }

        if (isChecked) {
            for (Media media : mSelectedImages) {
                if (media.getMediaPath().equals(image.getMediaPath())) {
                    mSelectedImages.remove(media);
                    updateCheckOrder();
                    zoomOut(holder.ivPicture);
                    break;
                }
            }
        } else {
            mSelectedImages.add(image);
            image.setCheckOrder(mSelectedImages.size());
            zoomIn(holder.ivPicture);
        }
        //通知点击项发生了改变
        notifyItemChanged(holder.getAdapterPosition());
        selectImage(holder, !isChecked, true);
        if (mOnActionListener != null) {
            mOnActionListener.onChange(mSelectedImages);
        }
    }

    public void setOnAlbumActionListener(OnAlbumActionListener listener) {
        this.mOnActionListener = listener;
    }

    private void zoomIn(ImageView iv_img) {
        if (isZoomEnable) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(iv_img, "scaleX", 1f, 1.12f),
                    ObjectAnimator.ofFloat(iv_img, "scaleY", 1f, 1.12f)
            );
            set.setDuration(DURATION);
            set.start();
        }
    }

    private void zoomOut(ImageView iv_img) {
        if (isZoomEnable) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(iv_img, "scaleX", 1.12f, 1f),
                    ObjectAnimator.ofFloat(iv_img, "scaleY", 1.12f, 1f)
            );
            set.setDuration(DURATION);
            set.start();
        }
    }

    public class CameraHolder extends RecyclerView.ViewHolder {
        public CameraHolder(View itemView) {
            super(itemView);
        }
    }

    public class ImageHolder extends RecyclerView.ViewHolder {
        ImageView ivPicture;
        FrameLayout flCheck;
        TextView tvCheck;
        TextView tvGif;
        TextView tvLongChart;

        public ImageHolder(View itemView) {
            super(itemView);

            ivPicture = (ImageView) itemView.findViewById(R.id.iv_picture);
            tvCheck = (TextView) itemView.findViewById(R.id.tv_check);
            flCheck = (FrameLayout) itemView.findViewById(R.id.fl_check);
            tvGif = (TextView) itemView.findViewById(R.id.tv_gif);
            tvLongChart = (TextView) itemView.findViewById(R.id.tv_long_chart);
        }
    }
}
