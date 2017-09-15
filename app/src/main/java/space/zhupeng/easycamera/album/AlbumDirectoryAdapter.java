package space.zhupeng.easycamera.album;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.util.ArrayList;
import java.util.List;

import space.zhupeng.easycamera.R;

/**
 * Created by zhupeng on 2017/9/15.
 */

public class AlbumDirectoryAdapter extends RecyclerView.Adapter<AlbumDirectoryAdapter.DirectoryHolder> {

    private Context context;
    private List<MediaFolder> mFolders = new ArrayList<>();

    public AlbumDirectoryAdapter(Context context) {
        super();
        this.context = context;
    }

    public void setFolders(List<MediaFolder> folders) {
        this.mFolders = folders;
        notifyDataSetChanged();
    }

    public List<MediaFolder> getFolders() {
        if (null == mFolders) {
            mFolders = new ArrayList<>();
        }

        return mFolders;
    }

    @Override
    public DirectoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DirectoryHolder(LayoutInflater.from(context).inflate(R.layout.item_folders, parent, false));
    }

    @Override
    public void onBindViewHolder(final DirectoryHolder holder, int position) {
        final MediaFolder folder = mFolders.get(position);
        final String folderName = folder.getFolderName();
        final int imageCount = folder.getImageCount();
        final String imagePath = folder.getFirstImagePath();
        final boolean isChecked = folder.isChecked();
        final int checkedNum = folder.getCheckedCount();
        holder.tvSign.setVisibility(checkedNum > 0 ? View.VISIBLE : View.INVISIBLE);
        holder.itemView.setSelected(isChecked);
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .centerCrop()
                .sizeMultiplier(0.5f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(160, 160);
        Glide.with(holder.itemView.getContext())
                .asBitmap()
                .load(imagePath)
                .apply(options)
                .into(new BitmapImageViewTarget(holder.ivFirstImage) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                        rbd.setCornerRadius(8);
                        holder.ivFirstImage.setImageDrawable(rbd);
                    }
                });
        holder.tvImageCount.setText(TextUtils.concat("(", Integer.toString(imageCount), ")"));
        holder.tvFolderName.setText(folderName);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null) {
                    for (MediaFolder mediaFolder : mFolders) {
                        mediaFolder.setChecked(false);
                    }
                    folder.setChecked(true);
                    notifyDataSetChanged();
                    onItemClickListener.onItemClick(folder.getFolderName(), folder.getImages());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (null == mFolders) return 0;
        return mFolders.size();
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(String folderName, List<Media> images);
    }

    class DirectoryHolder extends RecyclerView.ViewHolder {
        ImageView ivFirstImage;
        TextView tvFolderName;
        TextView tvImageCount;
        TextView tvSign;

        public DirectoryHolder(View itemView) {
            super(itemView);
            ivFirstImage = (ImageView) itemView.findViewById(R.id.iv_first_image);
            tvFolderName = (TextView) itemView.findViewById(R.id.tv_folder_name);
            tvImageCount = (TextView) itemView.findViewById(R.id.tv_image_count);
            tvSign = (TextView) itemView.findViewById(R.id.tv_sign);
        }
    }
}
