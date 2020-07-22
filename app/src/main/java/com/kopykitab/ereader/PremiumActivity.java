package com.kopykitab.ereader;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.kopykitab.ereader.components.PremiumBannerFragment;
import com.kopykitab.ereader.components.adapters.PremiumBooksAdapter;
import com.kopykitab.ereader.components.adapters.PremiumCategoryAdapter;
import com.kopykitab.ereader.models.PremiumItem;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PremiumActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private String customerId, productId, productPrice1, productPrice2;
    private List<String> premiumCategoryList = new ArrayList<>();
    private Button goPremiumButton;
    private Spinner categorySpinner;
    private PremiumBannerPagerAdapter bannerPagerAdapter;
    private PremiumCategoryAdapter premiumCategoryAdapter;
    private ViewPager topBannerViewPager, bottomBannerViewPager;
    private ArrayList<PremiumItem> premiumItemList;
    private RecyclerView premiumBookRecyclerView;
    private PremiumBooksAdapter premiumBooksAdapter;
    private JSONObject premiumObject;
    private LinearLayout premiumProgress;
    private List<String> bannerListTop, bannerListBottom;
    private StringBuilder goPremiumButtonText;
    private boolean isAsyncTaskIsRunning = false;
    private String screenName = "GoPremium";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.premium_activity);

        customerId = AppSettings.getInstance(this).get("CUSTOMER_ID");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        categorySpinner = (Spinner) findViewById(R.id.premium_category_spinner);
        categorySpinner.setOnItemSelectedListener(this);

        goPremiumButton = (Button) findViewById(R.id.btnGoPremium);
        premiumProgress = (LinearLayout) findViewById(R.id.premium_progress);
        premiumBookRecyclerView = (RecyclerView) findViewById(R.id.premium_book_view);
        premiumItemList = new ArrayList<>();

        new GetPremiumData().execute();

        premiumCategoryAdapter = new PremiumCategoryAdapter(getApplicationContext(), premiumCategoryList);
        categorySpinner.setAdapter(premiumCategoryAdapter);

        premiumBooksAdapter = new PremiumBooksAdapter(this, premiumItemList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        premiumBookRecyclerView.setLayoutManager(mLayoutManager);
        premiumBookRecyclerView.setItemAnimator(new DefaultItemAnimator());
        premiumBookRecyclerView.setAdapter(premiumBooksAdapter);

        topBannerViewPager = (ViewPager) findViewById(R.id.top_banner_view_pager);
        bottomBannerViewPager = (ViewPager) findViewById(R.id.bottom_banner_view_pager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Utils.triggerScreen(PremiumActivity.this, screenName);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, final int position, long id) {
        if (premiumObject != null) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        String key = premiumCategoryList.get(position);
                        if (premiumObject.get(key) instanceof JSONObject) {
                            premiumItemList = new ArrayList<>();
                            JSONObject premiumProductObject = (JSONObject) premiumObject.get(key);
                            productId = premiumProductObject.getString("product_id");
                            productPrice1 = premiumProductObject.getString("price_1");
                            productPrice2 = premiumProductObject.getString("price_2");
                            JSONArray premiumArray = premiumProductObject.getJSONArray("products");
                            for (int i = 0; i < premiumArray.length(); i++) {
                                JSONObject premium = premiumArray.getJSONObject(i);
                                PremiumItem premiumItem = new PremiumItem(premium.getString("image"), premium.getString("price_1"), premium.getString("price_2"));
                                premiumItemList.add(premiumItem);
                            }

                            JSONArray bannerArray = ((JSONObject) premiumObject.get(key)).getJSONArray("banners");
                            if (bannerArray != null) {
                                bannerListTop = new ArrayList<>();
                                int i;
                                for (i = 0; i < bannerArray.length(); i++) {
                                    bannerListTop.add(bannerArray.getString(i));

                                    if (i == 1)
                                        break;
                                }
                                bannerListBottom = new ArrayList<>();
                                for (i = i + 1; i < bannerArray.length(); i++) {
                                    bannerListBottom.add(bannerArray.getString(i));
                                }
                            }

                            return true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return false;
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    super.onPostExecute(aBoolean);

                    if (aBoolean) {
                        if (productPrice2 != null && !productPrice2.isEmpty()) {
                            goPremiumButtonText = new StringBuilder("gopremium for " + productPrice2);
                            goPremiumButton.setText(goPremiumButtonText);
                        } else {
                            goPremiumButtonText = new StringBuilder("gopremium for " + productPrice1);
                            goPremiumButton.setText(goPremiumButtonText);
                        }
                        premiumBooksAdapter.setPremiumItemList(premiumItemList);
                        premiumBookRecyclerView.setAdapter(premiumBooksAdapter);
                        premiumBooksAdapter.notifyDataSetChanged();

                        if (bannerListTop != null && bannerListTop.size() > 0) {
                            bannerPagerAdapter = new PremiumBannerPagerAdapter(getSupportFragmentManager(), bannerListTop);
                            topBannerViewPager.setAdapter(bannerPagerAdapter);
                        } else {
                            topBannerViewPager.setVisibility(View.GONE);
                        }

                        if (bannerListBottom != null && bannerListBottom.size() > 0) {
                            bannerPagerAdapter = new PremiumBannerPagerAdapter(getSupportFragmentManager(), bannerListBottom);
                            bottomBannerViewPager.setAdapter(bannerPagerAdapter);
                        } else {
                            bottomBannerViewPager.setVisibility(View.GONE);
                        }
                    }
                }
            }.execute();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    public void OnGoPremiumButtonClicked(View v) {
        if (Utils.isNetworkConnected(this)) {
            if (productId != null && !isAsyncTaskIsRunning) {
                isAsyncTaskIsRunning = true;
                goPremiumButton.setText("Adding to the Cart..");
                new AddProductToCart(productId).execute();

                Utils.triggerGAEventOnline(this, "gopremium", productId, customerId);
            }
        } else {
            Utils.networkNotAvailableAlertBox(this);
        }
    }

    public void OnBackButtonClick(View v) {
        onBackPressed();
    }

    public class PremiumBannerPagerAdapter extends FragmentStatePagerAdapter {
        private List<String> bannerList;
        private FragmentManager fragmentManager;
        private PremiumBannerFragment[] fragments;

        public PremiumBannerPagerAdapter(FragmentManager fm, List<String> bannerList) {
            super(fm);
            this.bannerList = bannerList;
            fragmentManager = fm;
            fragments = new PremiumBannerFragment[bannerList.size()];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            assert (0 <= position && position < fragments.length);
            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.remove(fragments[position]);
            trans.commitAllowingStateLoss();
            fragments[position] = null;
        }

        @Override
        public Fragment instantiateItem(ViewGroup container, int position) {
            Fragment fragment = getItem(position);
            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.add(container.getId(), fragment, "fragment:" + position);
            trans.commitAllowingStateLoss();
            return fragment;
        }

        @Override
        public boolean isViewFromObject(View view, Object fragment) {
            return ((Fragment) fragment).getView() == view;
        }

        @Override
        public int getCount() {
            return bannerList.size();
        }

        @Override
        public Fragment getItem(int position) {
            assert (0 <= position && position < fragments.length);
            if (fragments[position] == null) {
                fragments[position] = new PremiumBannerFragment(bannerList.get(position));
            }

            return fragments[position];
        }
    }

    private class GetPremiumData extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            String response = null;
            try {
                response = Utils.sendPost(Constants.PREMIUM_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
                Log.i("Premium Response", response);
                if (response != null) {
                    premiumObject = new JSONObject(response);
                    Iterator<String> keys = premiumObject.keys();
                    while (keys.hasNext()) {
                        premiumCategoryList.add(keys.next());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);

            if (premiumCategoryList != null) {
                premiumProgress.setVisibility(View.GONE);
                premiumCategoryAdapter.notifyDataSetChanged();
            }
        }
    }

    public class AddProductToCart extends AsyncTask<String, String, String> {
        private String productId;

        public AddProductToCart(String productId) {
            this.productId = productId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            premiumProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            String response = null;
            try {
                response = Utils.sendPost(PremiumActivity.this, Constants.PRODUCT_ADD_TO_CART, "product_id=" + URLEncoder.encode(productId, "UTF-8") + "&customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
                Log.i("Adding Product to Cart", response);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            isAsyncTaskIsRunning = false;

            try {
                if (result != null) {
                    premiumProgress.setVisibility(View.GONE);
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.getBoolean("success")) {
                        Utils.showWebViewActivity(PremiumActivity.this, jsonObject.getString("redirect"));
                        goPremiumButton.setText(goPremiumButtonText);
                    } else {
                        Constants.showToast(jsonObject.getString("message"), PremiumActivity.this);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
