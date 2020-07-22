package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.models.StoreCartItem;
import com.kopykitab.ereader.settings.Utils;

import java.util.List;

public class StoreCartAdapter extends RecyclerView.Adapter<StoreCartAdapter.MyViewHolder> {
    private List<StoreCartItem> cartList;
    private Context mContext;
    private StoreItemClickListener mItemClickListener;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout cartItem;
        public ImageView image;
        public TextView optionDetail, name, outOfStock, price_1, price_2;

        public MyViewHolder(View view) {
            super(view);
            cartItem = (LinearLayout) view.findViewById(R.id.cart_item);
            optionDetail = (TextView) view.findViewById(R.id.product_option_detail);
            image = (ImageView) view.findViewById(R.id.product_image);
            name = (TextView) view.findViewById(R.id.product_name);
            outOfStock = (TextView) view.findViewById(R.id.out_of_stock);
            price_1 = (TextView) view.findViewById(R.id.product_price_1);
            ((com.kopykitab.ereader.components.TextView) price_1).setAddStrike(true);
            price_2 = (TextView) view.findViewById(R.id.product_price_2);
        }
    }

    public StoreCartAdapter(Context mContext, List<StoreCartItem> cartList, StoreItemClickListener itemClickListener) {
        this.mContext = mContext;
        this.cartList = cartList;
        this.mItemClickListener = itemClickListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.store_cart_list, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final StoreCartItem storeCartItem = cartList.get(position);
        holder.cartItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.itemClicked(storeCartItem);
            }
        });
        holder.image.setImageDrawable(null);
        Utils.getImageLoader(mContext).displayImage(storeCartItem.getImageUrl().replaceAll(" ", "%20"), holder.image);

        String bookName = storeCartItem.getName();
        if (bookName.length() > 30) {
            bookName = bookName.substring(0, 27) + "...";
        }
        holder.name.setText(bookName);

        String bookOptionDetail = storeCartItem.getOptionDetail();
        if (bookOptionDetail != null) {
            holder.optionDetail.setText(bookOptionDetail);
        } else {
            holder.optionDetail.setText("");
        }
        String bookPrice1 = storeCartItem.getPrice_1().replace("INR", "\u20B9");
        String bookPrice2 = storeCartItem.getPrice_2().replace("INR", "\u20B9");

        if (bookPrice2 != null && !bookPrice2.isEmpty() && !bookPrice2.equals("")) {
            holder.price_1.setText(bookPrice1);
            holder.price_2.setText(bookPrice2);
        } else {
            holder.price_1.setVisibility(View.GONE);
            holder.price_2.setText(bookPrice1);
        }
        if (storeCartItem.getStockStatusId().equals("7")) {
            holder.optionDetail.setAlpha(1.0f);
            holder.image.setAlpha(1.0f);
            holder.name.setAlpha(1.0f);
            holder.price_1.setVisibility(View.VISIBLE);
            holder.price_2.setVisibility(View.VISIBLE);
            holder.outOfStock.setVisibility(View.GONE);
        } else {
            holder.optionDetail.setAlpha(0.5f);
            holder.image.setAlpha(0.5f);
            holder.name.setAlpha(0.5f);
            holder.price_1.setVisibility(View.GONE);
            holder.price_2.setVisibility(View.GONE);
            holder.outOfStock.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }
}