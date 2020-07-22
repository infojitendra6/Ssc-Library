package com.kopykitab.ereader.components;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.settings.Utils;

@SuppressLint("ValidFragment")
public class PremiumBannerFragment extends Fragment {
    private View rootView;
    private CornerImage bannerImageView;
    private String bannerURL;

    public PremiumBannerFragment() {
        // Required empty public constructor
    }

    public PremiumBannerFragment(String bannerURL) {
        this.bannerURL = bannerURL;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.premium_banner_fragment, container, false);
        bannerImageView = (CornerImage) rootView.findViewById(R.id.premium_banner_image);

        bannerImageView.setImageDrawable(null);
        if (bannerURL != null) {
            Utils.getImageLoaderOnline(getContext()).displayImage(bannerURL.replaceAll(" ", "%20"), bannerImageView);
        }

        return rootView;
    }
}
