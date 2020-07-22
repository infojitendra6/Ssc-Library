package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.models.StoreBannerItem;
import com.kopykitab.ereader.settings.Utils;

import java.util.List;


public class BannerPagerAdapter extends PagerAdapter {
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private List<StoreBannerItem> bannerItemList;
    private StoreItemClickListener mItemClickListener;

    public BannerPagerAdapter(Context context, List<StoreBannerItem> bannerItemList, StoreItemClickListener mItemClickListener) {
        this.mContext = context;
        this.bannerItemList = bannerItemList;
        this.mItemClickListener = mItemClickListener;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return bannerItemList.size();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((LinearLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.store_banner_pager_item, container, false);

        final StoreBannerItem bannerItem = bannerItemList.get(position);
        ImageView bannerImage = (ImageView) itemView.findViewById(R.id.imageView);
        bannerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.itemClicked(bannerItem);
            }
        });
        bannerImage.setImageDrawable(null);
        Utils.getImageLoader(mContext).displayImage(bannerItem.getImage().replaceAll(" ", "%20"), bannerImage);

        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }
}
