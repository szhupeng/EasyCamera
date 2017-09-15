package space.zhupeng.easycamera.album;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import space.zhupeng.easycamera.R;

public class FolderPopWindow extends PopupWindow implements View.OnClickListener {
    private Context context;
    private RecyclerView rvFolder;
    private AlbumDirectoryAdapter mAdapter;
    private Animation mAnimationIn, mAnimationOut;
    private boolean isDismiss = false;
    private LinearLayout llRoot;
    private TextView tvPictureTitle;
    private Drawable mDrawableUp, mDrawableDown;

    public FolderPopWindow(Context context) {
        this.context = context;
        final View view = LayoutInflater.from(context).inflate(R.layout.layout_folder_pop, null);
        this.setContentView(view);
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.setWidth(metrics.widthPixels);
        this.setHeight(metrics.heightPixels);
        this.setAnimationStyle(R.style.WindowStyle);
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.update();
        this.setBackgroundDrawable(new ColorDrawable(Color.argb(123, 0, 0, 0)));
        mDrawableUp = ContextCompat.getDrawable(context, R.drawable.ic_arrow_up);
        mDrawableDown = ContextCompat.getDrawable(context, R.drawable.ic_arrow_down);
        mAnimationIn = AnimationUtils.loadAnimation(context, R.anim.photo_album_show);
        mAnimationOut = AnimationUtils.loadAnimation(context, R.anim.photo_album_dismiss);

        initView(metrics);
    }

    public void initView(final DisplayMetrics metrics) {
        final View view = getContentView();
        llRoot = (LinearLayout) view.findViewById(R.id.ll_root);
        mAdapter = new AlbumDirectoryAdapter(context);
        rvFolder = (RecyclerView) view.findViewById(R.id.rv_folders);
        rvFolder.getLayoutParams().height = (int) (metrics.heightPixels * 0.6f);
        rvFolder.setLayoutManager(new LinearLayoutManager(context));
        rvFolder.setAdapter(mAdapter);
        llRoot.setOnClickListener(this);
    }

    public void setFolders(List<MediaFolder> folders) {
        mAdapter.setFolders(folders);
    }

    public void setPictureTitleView(TextView titleView) {
        this.tvPictureTitle = titleView;
    }

    @Override
    public void showAsDropDown(View anchor) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                Rect rect = new Rect();
                anchor.getGlobalVisibleRect(rect);
                int h = anchor.getResources().getDisplayMetrics().heightPixels - rect.bottom;
                setHeight(h);
            }
            super.showAsDropDown(anchor);
            isDismiss = false;
            rvFolder.startAnimation(mAnimationIn);
            modifyTextViewDrawable(tvPictureTitle, mDrawableUp, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnItemClickListener(AlbumDirectoryAdapter.OnItemClickListener listener) {
        mAdapter.setOnItemClickListener(listener);
    }

    @Override
    public void dismiss() {
        if (isDismiss) {
            return;
        }
        modifyTextViewDrawable(tvPictureTitle, mDrawableDown, 2);
        isDismiss = true;
        rvFolder.startAnimation(mAnimationOut);
        dismiss();
        mAnimationOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isDismiss = false;
                dismissInternal();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void dismissInternal() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            //在android4.1.1和4.1.2版本关闭PopWindow
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    FolderPopWindow.super.dismiss();
                }
            });
        } else {
            FolderPopWindow.super.dismiss();
        }
    }

    /**
     * 设置选中状态
     */
    public void notifyCheckedChanged(List<Media> medias) {
        try {
            // 获取选中图片
            List<MediaFolder> folders = mAdapter.getFolders();
            for (MediaFolder folder : folders) {
                folder.setCheckedCount(0);
            }
            if (medias.size() > 0) {
                for (MediaFolder folder : folders) {
                    int count = 0;// 记录当前相册下有多少张图片被选中
                    List<Media> images = folder.getImages();
                    for (Media media : images) {
                        String path = media.getMediaPath();
                        for (Media m : medias) {
                            if (path.equals(m.getMediaPath())) {
                                count++;
                                folder.setCheckedCount(count);
                            }
                        }
                    }
                }
            }
            mAdapter.setFolders(folders);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyTextViewDrawable(TextView v, Drawable drawable, int index) {
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        //index 0:左 1：上 2：右 3：下
        if (index == 0) {
            v.setCompoundDrawables(drawable, null, null, null);
        } else if (index == 1) {
            v.setCompoundDrawables(null, drawable, null, null);
        } else if (index == 2) {
            v.setCompoundDrawables(null, null, drawable, null);
        } else {
            v.setCompoundDrawables(null, null, null, drawable);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ll_root) {
            dismiss();
        }
    }
}
