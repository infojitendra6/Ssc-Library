package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.models.PremiumFeatureItem;
import com.kopykitab.ereader.settings.Utils;

import java.util.List;

public class PremiumFeatureAdapter extends RecyclerView.Adapter<PremiumFeatureAdapter.MyViewHolder> {
    Context mContext;
    private List<PremiumFeatureItem> premiumFeatureItemList;

    public PremiumFeatureAdapter(Context mContext, List<PremiumFeatureItem> premiumFeatureItemList) {
        this.premiumFeatureItemList = premiumFeatureItemList;
        this.mContext = mContext;
    }

    @Override
    public PremiumFeatureAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.premium_feature_list, null);

        return new PremiumFeatureAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PremiumFeatureAdapter.MyViewHolder holder, int position) {

        PremiumFeatureItem premiumFeatureItem = premiumFeatureItemList.get(position);
        holder.featureImage.setImageResource(premiumFeatureItem.getImage());
        holder.featureTitle.setText(premiumFeatureItem.getTitle());
        holder.featureDescription.setText(premiumFeatureItem.getDescription());

        Utils.getImageLoader(mContext).displayImage("drawable://" + premiumFeatureItem.getImage(), holder.featureImage);
    }

    @Override
    public int getItemCount() {
        return premiumFeatureItemList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView featureImage;
        TextView featureTitle;
        TextView featureDescription;

        public MyViewHolder(View view) {
            super(view);

            featureImage = (ImageView) view.findViewById(R.id.premium_feature_image);
            featureTitle = (TextView) view.findViewById(R.id.premium_feature_title);
            featureDescription = (TextView) view.findViewById(R.id.premium_feature_description);
        }
    }
}