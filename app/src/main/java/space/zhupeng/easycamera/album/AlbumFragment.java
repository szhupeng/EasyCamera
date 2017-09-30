package space.zhupeng.easycamera.album;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by zhupeng on 2017/9/30.
 */

public class AlbumFragment extends Fragment {

    public static AlbumFragment newInstance(AlbumConfig config) {
        Bundle args = new Bundle();

        AlbumFragment fragment = new AlbumFragment();
        args.putParcelable("config", config);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
