package com.kopykitab.ereader.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ViewFlipper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.LibraryActivity;
import com.kopykitab.ereader.R;
import com.kopykitab.ereader.components.adapters.BooksAdapter;
import com.kopykitab.ereader.components.adapters.BooksAdapter.BooksAdapterListener;
import com.kopykitab.ereader.components.adapters.PremiumBooksAdapter;
import com.kopykitab.ereader.components.adapters.PremiumCategoryAdapter;
import com.kopykitab.ereader.components.adapters.PremiumFeatureAdapter;
import com.kopykitab.ereader.models.BookItem;
import com.kopykitab.ereader.models.PremiumFeatureItem;
import com.kopykitab.ereader.models.PremiumItem;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@SuppressLint("ValidFragment")
public class LibraryFragment extends Fragment implements AdapterView.OnItemSelectedListener, InteractiveScrollView.OnBottomReachedListener, InteractiveScrollView.OnTopReachedListener {

    private View rootView;

    private Context mContext;
    private String productTypes, customerId, productId, productPrice1, productPrice2;
    private int totalCategories, categoryPosition;
    private LibraryAutofitRecyclerView bookListView;
    private BooksAdapter booksAdapter;
    private List<BookItem> bookItemList = new ArrayList<BookItem>();
    private BooksAdapterListener bookItemListener;
    private LinearLayout bottomNavigationWidget;
    private Button ebookButton, testPreparationButton, videoButton, storesButton, recommendationButton;

    private TextView storeOffer;

    private ViewFlipper premiumViewFlipper;
    private Button goPremiumButton, settingButton, refreshButton;
    private PremiumCategoryAdapter premiumCategoryAdapter;
    private RecyclerView premiumBookRecyclerView;
    private PremiumBooksAdapter premiumBooksAdapter;
    private List<PremiumItem> premiumItemList;
    private List<String> premiumCategoryList = new ArrayList<>();
    private RecyclerView premiumFeatureRecyclerView;
    private PremiumFeatureAdapter premiumFeatureAdapter;
    private ArrayList<PremiumFeatureItem> premiumFeatureItems;
    private JSONObject premiumObject;
    private LinearLayout premiumProgress;
    private StringBuilder goPremiumButtonText;
    private boolean isAsyncTaskIsRunning = false;

    Integer featureImage[] = {
            R.drawable.ic_print_notes_1, R.drawable.ic_print_notes_2, R.drawable.ic_print_notes_3, R.drawable.ic_print_notes_4,
    };

    String featureTitle[] = {
            "Print notes + highlight", "Annotations", "Unlock premium ebooks", "Premium Updated Content",
    };

    String featureDescription[] = {"Print your notes & Highlights with goPremium.\n" + "You can now print upto 100 notes anytime you want.",
            "Use annotation feature to Highlight, Underline & Strike-through important points and definitions. You can browse your annotations anytime using the explorer. ",
            "With goPremium get upto 15 premium ebooks that can take  your learning curve to its peak.\n" + "goPremium comes with its own perks,",
            "Going Premium as its own perks Unlock regular content updates & receive massive discounts on our product range.\n" + "Your Search for materials ends here.",
    };

    public LibraryFragment() {
        super();
    }

    public LibraryFragment(Context mContext, String productTypes, int totalCategories, int categoryPosition, BooksAdapterListener bookItemListener) {
        this.mContext = mContext;
        this.productTypes = productTypes;
        this.totalCategories = totalCategories;
        this.categoryPosition = categoryPosition;
        this.bookItemListener = bookItemListener;
        customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if ((totalCategories - 1) == categoryPosition) {
            rootView = inflater.inflate(R.layout.library_premium_activity, container, false);

            premiumViewFlipper = (ViewFlipper) rootView.findViewById(R.id.library_premium_view_flipper);
            InteractiveScrollView scrollView = (InteractiveScrollView) rootView.findViewById(R.id.library_premium_scroll_view);
            scrollView.setOnTopReachedListener(this);
            scrollView.setOnBottomReachedListener(this);

            goPremiumButton = (Button) rootView.findViewById(R.id.btnGoPremium);
            premiumProgress = (LinearLayout) rootView.findViewById(R.id.premium_progress);

            if (Utils.isNetworkConnected(getContext())) {
                premiumViewFlipper.setDisplayedChild(0);
                new GetPremiumData().execute();
            } else {
                premiumViewFlipper.setDisplayedChild(1);
            }

            settingButton = (Button) rootView.findViewById(R.id.library_premium_setting_button);
            refreshButton = (Button) rootView.findViewById(R.id.library_premium_refresh_button);
            Spinner spinner = (Spinner) rootView.findViewById(R.id.premium_category_spinner);
            spinner.setOnItemSelectedListener(this);

            premiumCategoryAdapter = new PremiumCategoryAdapter(getContext(), premiumCategoryList);
            spinner.setAdapter(premiumCategoryAdapter);

            premiumBookRecyclerView = (RecyclerView) rootView.findViewById(R.id.premium_book_view);
            premiumBookRecyclerView.setHasFixedSize(true);
            premiumBookRecyclerView.setNestedScrollingEnabled(false);
            premiumItemList = new ArrayList<>();

            premiumBooksAdapter = new PremiumBooksAdapter(getContext(), premiumItemList);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            premiumBookRecyclerView.setLayoutManager(mLayoutManager);
            premiumBookRecyclerView.setItemAnimator(new DefaultItemAnimator());
            premiumBookRecyclerView.setAdapter(premiumBooksAdapter);

            premiumFeatureRecyclerView = (RecyclerView) rootView.findViewById(R.id.premium_feature_view);
            premiumFeatureRecyclerView.setHasFixedSize(true);
            premiumFeatureRecyclerView.setNestedScrollingEnabled(false);
            premiumFeatureItems = new ArrayList<>();

            for (int i = 0; i < featureImage.length; i++) {
                PremiumFeatureItem premiumFeatureItem = new PremiumFeatureItem(featureImage[i], featureTitle[i], featureDescription[i]);
                premiumFeatureItems.add(premiumFeatureItem);
            }

            premiumFeatureAdapter = new PremiumFeatureAdapter(getContext(), premiumFeatureItems);
            premiumFeatureRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            premiumFeatureRecyclerView.setAdapter(premiumFeatureAdapter);

            goPremiumButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Utils.isNetworkConnected(getContext())) {
                        if (productId != null && !isAsyncTaskIsRunning) {
                            isAsyncTaskIsRunning = true;
                            goPremiumButton.setText("Adding to the Cart..");
                            new AddProductToCart(productId).execute();

                            Utils.triggerGAEventOnline(getContext(), "gopremium", productId, customerId);
                        }
                    } else {
                        Utils.networkNotAvailableAlertBox(getContext());
                    }
                }
            });

            settingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
                }
            });

            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Utils.isNetworkConnected(getContext())) {
                        premiumViewFlipper.setDisplayedChild(0);
                        new GetPremiumData().execute();
                    }
                }
            });
        } else if ((totalCategories - 2) == categoryPosition) {
            rootView = inflater.inflate(R.layout.library_store, container, false);

            storeOffer = (TextView) rootView.findViewById(R.id.library_store_offer);
            try {
                String offers = AppSettings.getInstance(getContext()).get("offers");
                if (offers != null && !offers.isEmpty()) {
                    JSONArray offersArray = new JSONArray(offers);
                    if (offersArray.length() > 0) {
                        storeOffer.setVisibility(View.VISIBLE);
                        storeOffer.setText(Html.fromHtml(offersArray.getString(0)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            rootView = inflater.inflate(R.layout.library_main, container, false);
            bookListView = (LibraryAutofitRecyclerView) rootView.findViewById(R.id.bookListView);
            bookListView.setHasFixedSize(true);

            new PrepareBookLibrary().execute();

            bookListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy > 0 && bottomNavigationWidget.isShown()) {
                        hideBottomNavigationWidget();
                    } else if (dy < 0) {
                        showBottomNavigationWidget();

                    }
                }

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                    super.onScrollStateChanged(recyclerView, newState);
                }
            });
        }

        bottomNavigationWidget = (LinearLayout) getActivity().findViewById(R.id.bottom_navigation_widget);
        ebookButton = (Button) getActivity().findViewById(R.id.ebook_bottom_navigation_button);
        testPreparationButton = (Button) getActivity().findViewById(R.id.test_preparation_bottom_navigation_button);
        videoButton = (Button) getActivity().findViewById(R.id.video_bottom_navigation_button);
        storesButton = (Button) getActivity().findViewById(R.id.store_bottom_navigation_button);
        recommendationButton = (Button) getActivity().findViewById(R.id.recommendation_bottom_navigation_button);

        ebookButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if (!Constants.getActivateBottomNavigationWidgetButton().equals("E-books")) {
                    disabledActivateBottomNavigationWidgetButton();

                    ebookButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_ebook, 0, 0);
                    ebookButton.setTypeface(null, Typeface.BOLD);
                    ebookButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));

                    Utils.showLibrary(mContext, "ebook");
                    Utils.triggerGAEvent(mContext, "BottomNavigation", "E-books", customerId);
                }
            }
        });

        testPreparationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if (!Constants.getActivateBottomNavigationWidgetButton().equals("Test Preparation")) {
                    disabledActivateBottomNavigationWidgetButton();

                    testPreparationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_test_preparation, 0, 0);
                    testPreparationButton.setTypeface(null, Typeface.BOLD);
                    testPreparationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));

                    Utils.showLibrary(mContext, "test_preparation, mock_test");
                    Utils.triggerGAEvent(mContext, "BottomNavigation", "Test Preparation", customerId);
                }
            }
        });

        videoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if (!Constants.getActivateBottomNavigationWidgetButton().equals("Video")) {
                    disabledActivateBottomNavigationWidgetButton();

                    videoButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_video, 0, 0);
                    videoButton.setTypeface(null, Typeface.BOLD);
                    videoButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));

                    Utils.showLibrary(mContext, "video");
                    Utils.triggerGAEvent(mContext, "BottomNavigation", "Video", customerId);
                }
            }
        });

        storesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if (!Constants.getActivateBottomNavigationWidgetButton().equals("Store")) {
                    disabledActivateBottomNavigationWidgetButton();

                    storesButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_store, 0, 0);
                    storesButton.setTypeface(null, Typeface.BOLD);
                    storesButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));

                    Utils.showStoreActivity(mContext);
                    Utils.triggerGAEvent(mContext, "BottomNavigation", "Store", customerId);
                }
            }
        });

        recommendationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if (!Constants.getActivateBottomNavigationWidgetButton().equals("Recommendation")) {
                    if (Utils.isNetworkConnected(mContext)) {
                        disabledActivateBottomNavigationWidgetButton();

                        recommendationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_recommendation, 0, 0);
                        recommendationButton.setTypeface(null, Typeface.BOLD);
                        recommendationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));

                        Utils.showRecommendationsActivity(mContext);
                    } else {
                        Utils.networkNotAvailableAlertBox(mContext);
                    }
                    Utils.triggerGAEvent(mContext, "BottomNavigation", "Recommendation", customerId);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
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
                    }
                }
            }.execute();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onBottomReached(int scrollY) {
        if (bottomNavigationWidget.isShown()) {
            hideBottomNavigationWidget();
        }
    }

    @Override
    public void onTopReached(int scrollY) {
        if (!bottomNavigationWidget.isShown()) {
            showBottomNavigationWidget();
        }
    }

    public void setViewToListView() {
        if (booksAdapter != null) {
            bookListView.switchToListView();
            booksAdapter = new BooksAdapter(mContext, bookItemList, R.layout.library_list, "ListView", bookItemListener);
            bookListView.setAdapter(booksAdapter);
        }
    }

    public void setViewToGridView() {
        if (booksAdapter != null) {
            bookListView.switchToGridView();
            booksAdapter = new BooksAdapter(mContext, bookItemList, R.layout.library_grid, "GridView", bookItemListener);
            bookListView.setAdapter(booksAdapter);
        }
    }

    public BooksAdapter getAdapter() {
        return booksAdapter;
    }

    private class PrepareBookLibrary extends AsyncTask<String, BookItem, List<BookItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (LibraryActivity.isListView()) {
                booksAdapter = new BooksAdapter(getContext(), R.layout.library_list, "ListView", bookItemListener);
            } else {
                booksAdapter = new BooksAdapter(getContext(), R.layout.library_grid, "GridView", bookItemListener);
            }
            bookListView.setAdapter(booksAdapter);
        }

        @Override
        protected List<BookItem> doInBackground(String... strings) {
            List<HashMap<String, String>> booksList = new ArrayList<HashMap<String, String>>();
            try {
                String jsonFileName = productTypes.replaceAll("(\\s|,)+", "_") + "_by_categories.json";
                File jsonFile = new File(Utils.getDirectory(mContext) + jsonFileName);
                long serverTime = jsonFile.lastModified(), systemTime = new Date().getTime();

                FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
                byte[] fileRead = new byte[jsonFileInputStream.available()];
                jsonFileInputStream.read(fileRead);
                jsonFileInputStream.close();
                JSONObject productTypeJsonObject = new JSONObject(new String(Base64.decode(fileRead, Base64.DEFAULT)));
                booksList = new Gson().fromJson(productTypeJsonObject.getJSONArray("products").getJSONObject(categoryPosition).getJSONArray("products").toString(), new TypeToken<ArrayList<HashMap<String, String>>>() {
                }.getType());

                for (HashMap<String, String> book : booksList) {
                    book.put("image_url", Constants.IMAGE_URL + book.get("image_url"));
                    long diffDays = Math.abs(systemTime - serverTime) / 86400000; // Days calculation by 1000*60*60*24
                    if (book.get("left_days") == null || book.get("left_days").isEmpty() || book.get("left_days").equals("")) {
                        book.put("left_days", "999999");
                    }
                    int leftDays = Integer.valueOf(book.get("left_days")).intValue();
                    if (leftDays == 999999) {
                        BookItem bookItem = new BookItem(book.get("name"), book.get("order_product_id"), book.get("cidd"), book.get("image_url"), book.get("description"), book.get("price"), book.get("left_days"), book.get("product_id"), book.get("product_type"), book.get("option_name"), book.get("option_value"), book.get("pdf_downloaded_date"), book.get("license_period"), book.get("product_link"), book.get("date_added"));
                        bookItemList.add(bookItem);

                        publishProgress(bookItem);
                    } else {
                        if ((leftDays - diffDays) >= 0) {
                            BookItem bookItem = new BookItem(book.get("name"), book.get("order_product_id"), book.get("cidd"), book.get("image_url"), book.get("description"), book.get("price"), book.get("left_days"), book.get("product_id"), book.get("product_type"), book.get("option_name"), book.get("option_value"), book.get("pdf_downloaded_date"), book.get("license_period"), book.get("product_link"), book.get("date_added"));
                            bookItemList.add(bookItem);

                            publishProgress(bookItem);
                        } else {
                            Utils.removeExpiredFiles(mContext, book.get("product_link"), book.get("image_url"), book.get("product_id"));
                        }
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return bookItemList;
        }

        @Override
        protected void onProgressUpdate(BookItem... values) {
            super.onProgressUpdate(values);
            booksAdapter.setItem(values[0]);
        }

        @Override
        protected void onPostExecute(List<BookItem> bookItemList) {
            super.onPostExecute(bookItemList);
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
                response = Utils.sendPost(getContext(), Constants.PRODUCT_ADD_TO_CART, "product_id=" + URLEncoder.encode(productId, "UTF-8") + "&customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
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
                        Utils.showWebViewActivity(getContext(), jsonObject.getString("redirect"));
                        goPremiumButton.setText(goPremiumButtonText);
                    } else {
                        Constants.showToast(jsonObject.getString("message"), getContext());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void disabledActivateBottomNavigationWidgetButton() {
        try {
            Activity activity = getActivity();
            if (activity != null && isAdded()) {
                String lastActivatedButton = Constants.getActivateBottomNavigationWidgetButton();
                if (lastActivatedButton.equals("E-books")) {
                    ebookButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_ebook_disabled, 0, 0);
                    ebookButton.setTypeface(null, Typeface.NORMAL);
                    ebookButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));
                } else if (lastActivatedButton.equals("Test Preparation")) {
                    testPreparationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_test_preparation_disabled, 0, 0);
                    testPreparationButton.setTypeface(null, Typeface.NORMAL);
                    testPreparationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));
                } else if (lastActivatedButton.equals("Video")) {
                    videoButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_video_disabled, 0, 0);
                    videoButton.setTypeface(null, Typeface.NORMAL);
                    videoButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));
                } else if (lastActivatedButton.equals("Store")) {
                    storesButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_store_disabled, 0, 0);
                    storesButton.setTypeface(null, Typeface.NORMAL);
                    storesButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));
                } else if (lastActivatedButton.equals("Recommendation")) {
                    recommendationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_recommendation_disabled, 0, 0);
                    recommendationButton.setTypeface(null, Typeface.NORMAL);
                    recommendationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));
                }
            }
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideBottomNavigationWidget() {
        bottomNavigationWidget.setAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_down));

        Animation anim = new TranslateAnimation(0, 0, 0, bottomNavigationWidget.getHeight());
        anim.setDuration(500);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                bottomNavigationWidget.setVisibility(View.GONE);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
            }
        });
        bottomNavigationWidget.startAnimation(anim);
    }

    private void showBottomNavigationWidget() {
        bottomNavigationWidget.setAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_up));
        Animation anim = new TranslateAnimation(0, 0, bottomNavigationWidget.getHeight(), 0);
        anim.setDuration(500);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                bottomNavigationWidget.setVisibility(View.VISIBLE);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
            }
        });
        bottomNavigationWidget.startAnimation(anim);
    }
}