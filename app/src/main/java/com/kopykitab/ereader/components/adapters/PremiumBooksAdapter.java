package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.models.PremiumItem;
import com.kopykitab.ereader.settings.Utils;

import java.util.List;

public class PremiumBooksAdapter extends RecyclerView.Adapter<PremiumBooksAdapter.MyViewHolder> {
    Context mContext;
    private List<PremiumItem> premiumItemList;

    public PremiumBooksAdapter(Context mContext, List<PremiumItem> premiumItemList) {
        this.premiumItemList = premiumItemList;
        this.mContext = mContext;
    }

    public void setPremiumItemList(List<PremiumItem> premiumItemList) {
        this.premiumItemList = premiumItemList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.premium_book_list, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        PremiumItem premiumItem = premiumItemList.get(position);
        holder.premiumImage.setImageDrawable(null);
        Utils.getImageLoaderOnline(mContext).displayImage(premiumItem.getImageURL().replaceAll(" ", "%20"), holder.premiumImage);
        holder.premiumPrice1.setText(premiumItem.getPrice_1());
        holder.premiumPrice2.setText(premiumItem.getPrice_2());
    }

    @Override
    public int getItemCount() {
        return premiumItemList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView premiumImage;
        TextView premiumPrice1;
        TextView premiumPrice2;

        public MyViewHolder(View view) {
            super(view);

            premiumImage = (ImageView) view.findViewById(R.id.premium_book_image);
            premiumPrice1 = (TextView) view.findViewById(R.id.premium_price_1);
            ((com.kopykitab.ereader.components.TextView) premiumPrice1).setAddStrike(true);
            premiumPrice2 = (TextView) view.findViewById(R.id.premium_price_2);
        }

    }

}