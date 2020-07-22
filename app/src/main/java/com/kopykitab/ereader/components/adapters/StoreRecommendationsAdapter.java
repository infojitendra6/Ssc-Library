package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.models.StoreRecommendationsItem;
import com.kopykitab.ereader.settings.Utils;

import java.util.List;

public class StoreRecommendationsAdapter extends RecyclerView.Adapter<StoreRecommendationsAdapter.MyViewHolder> {

    private List<StoreRecommendationsItem> recommendationList;
    private Context mContext;
    private StoreItemClickListener mItemClickListener;

    public StoreRecommendationsAdapter(Context mContext, List<StoreRecommendationsItem> recommendationList, StoreItemClickListener itemClickListener) {
        this.mContext = mContext;
        this.recommendationList = recommendationList;
        this.mItemClickListener = itemClickListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.store_recommendation_list, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final StoreRecommendationsItem storeRecommendationsItem = recommendationList.get(position);
        holder.recommendationItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.itemClicked(storeRecommendationsItem);
            }
        });
        holder.image.setImageDrawable(null);
        Utils.getImageLoader(mContext).displayImage(storeRecommendationsItem.getImageUrl().replaceAll(" ", "%20"), holder.image);

        String bookName = Html.fromHtml(storeRecommendationsItem.getName()).toString();
        if (bookName.length() > 30) {
            bookName = bookName.substring(0, 27) + "...";
        }
        holder.name.setText(bookName);

        String bookPrice1 = storeRecommendationsItem.getPrice_1();
        String bookPrice2 = storeRecommendationsItem.getPrice_2();

        if (bookPrice2 != null && !bookPrice2.isEmpty() && !bookPrice2.equals("")) {
            holder.price_1.setText(bookPrice1);
            holder.price_2.setText(bookPrice2);
        } else {
            holder.price_1.setVisibility(View.GONE);
            holder.price_2.setText(bookPrice1);
        }
    }

    @Override
    public int getItemCount() {
        return recommendationList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout recommendationItem;
        public ImageView image;
        public TextView name, price_1, price_2;

        public MyViewHolder(View view) {
            super(view);
            recommendationItem = (LinearLayout) view.findViewById(R.id.recommendation_item);
            image = (ImageView) view.findViewById(R.id.product_image);
            name = (TextView) view.findViewById(R.id.product_name);
            price_1 = (TextView) view.findViewById(R.id.product_price_1);
            ((com.kopykitab.ereader.components.TextView) price_1).setAddStrike(true);
            price_2 = (TextView) view.findViewById(R.id.product_price_2);
        }
    }
}
