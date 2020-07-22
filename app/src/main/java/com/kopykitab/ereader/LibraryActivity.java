package com.kopykitab.ereader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.components.AppRater;
import com.kopykitab.ereader.components.Button;
import com.kopykitab.ereader.components.CircularImageView;
import com.kopykitab.ereader.components.LibraryAutofitRecyclerView;
import com.kopykitab.ereader.components.LibraryFragment;
import com.kopykitab.ereader.components.LibrarySlidingTabStrip;
import com.kopykitab.ereader.components.adapters.BooksAdapter;
import com.kopykitab.ereader.components.adapters.BooksAdapter.BooksAdapterListener;
import com.kopykitab.ereader.components.handler.PermissionHandler;
import com.kopykitab.ereader.components.handler.PermissionHandler.PermissionListener;
import com.kopykitab.ereader.components.adapters.MenuListAdapter;
import com.kopykitab.ereader.models.BookItem;
import com.kopykitab.ereader.models.DatabaseHelper;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.DatabaseConstants;
import com.kopykitab.ereader.settings.DownloadFileService;
import com.kopykitab.ereader.settings.Utils;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LibraryActivity extends MainFoundationActivity implements BooksAdapterListener, PermissionListener {
    private Toolbar mToolbar, mSearchToolbar;
    private Menu mSearchMenu;
    private MenuItem mItemSearch, mMenuItem;
    private boolean shouldSearchIconShow = false;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private LinearLayout dashboardMenuContainer;
    private ExpandableListView leftDrawerList;
    private MenuListAdapter navigationDrawerAdapter;

    private List<String> categoryList;
    private int totalCategoryList;
    private ViewFlipper libraryFlipper;
    private LibraryPagerAdapter libraryAdapter;
    private ViewPager libraryPager;
    protected LibrarySlidingTabStrip mLibraryTab;
    private ImageView libraryViewChange;
    private String productTypes;
    private boolean isSync = false;
    private static boolean listView = false;
    private boolean isSearch = false;
    private ImageView noProductImageView;
    private TextView noProductText, noProductSubText;
    private Button noProductButton;

    private BooksAdapterListener bookItemListener;
    private LibraryAutofitRecyclerView libraryBookView;
    private List<BookItem> searchBookList = new ArrayList<>();
    private BooksAdapter mSearchBooksAdapter, mDownloadedBooksAdapter;

    private LinearLayout bottomNavigationWidget;
    private Button ebookButton, testPreparationButton, videoButton, storesButton, recommendationButton;
    private ProgressBar mProgressBar, downloadProgressBar;
    private Dialog pDownloadDialog;
    private ImageButton downloadCancel;
    private TextView downloadBarCompleted;

    private PermissionHandler mPermissionHandler;
    private String screenName = "Library", tempScreenName = "Library";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_activity);

        productTypes = getIntent().getStringExtra("product_type");
        if (productTypes == null || productTypes.isEmpty() || productTypes.equals("")) {
            productTypes = "ebook";
        }
        isSync = getIntent().getBooleanExtra("is_sync", false);
        tempScreenName = screenName + "_" + productTypes.replaceAll("(\\s|,)+", "_");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        libraryFlipper = (ViewFlipper) findViewById(R.id.library_flipper);
        noProductImageView = (ImageView) libraryFlipper.findViewById(R.id.no_product_image_view);
        noProductText = (TextView) libraryFlipper.findViewById(R.id.no_product_text);
        noProductSubText = (TextView) libraryFlipper.findViewById(R.id.no_product_sub_text);
        noProductButton = (Button) libraryFlipper.findViewById(R.id.no_product_button);

        bottomNavigationWidget = (LinearLayout) findViewById(R.id.bottom_navigation_widget);
        bottomNavigationWidget.setVisibility(View.INVISIBLE);
        ebookButton = (Button) bottomNavigationWidget.findViewById(R.id.ebook_bottom_navigation_button);
        testPreparationButton = (Button) bottomNavigationWidget.findViewById(R.id.test_preparation_bottom_navigation_button);
        videoButton = (Button) bottomNavigationWidget.findViewById(R.id.video_bottom_navigation_button);
        storesButton = (Button) bottomNavigationWidget.findViewById(R.id.store_bottom_navigation_button);
        recommendationButton = (Button) bottomNavigationWidget.findViewById(R.id.recommendation_bottom_navigation_button);

        libraryViewChange = (CircularImageView) findViewById(R.id.library_view_change);

        mPermissionHandler = new PermissionHandler();
        mPermissionHandler.requestPermission(this, Constants.STORAGE_PERMISSION, 101, this);

        bookItemListener = this;
        drawerLayout = (DrawerLayout) findViewById(R.id.library_drawer_layout);
        mToolbar = (Toolbar) findViewById(R.id.library_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        dashboardMenuContainer = (LinearLayout) findViewById(R.id.dashboard_menu_container);
        dashboardMenuContainer.setOnClickListener(null);
        leftDrawerList = (ExpandableListView) findViewById(R.id.menu_list_view);

        String libraryView = Utils.getLibraryView();
        if (libraryView.equals("GridView")) {
            libraryViewChange.setTag(R.drawable.list_view_icon);
            libraryViewChange.setImageResource(R.drawable.list_view_icon);
        } else if (libraryView.equals("ListView")) {
            libraryViewChange.setTag(R.drawable.grid_view_icon);
            libraryViewChange.setImageResource(R.drawable.grid_view_icon);
        }

        libraryViewChange.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                productTypes = getIntent().getStringExtra("product_type");
                if (productTypes == null || productTypes.isEmpty() || productTypes.equals("")) {
                    productTypes = "ebook";
                }
                if (productTypes.startsWith("downloaded_")) {
                    if (libraryViewChange.getTag().equals(R.drawable.list_view_icon)) {
                        libraryViewChange.setTag(R.drawable.grid_view_icon);
                        libraryViewChange.setImageResource(R.drawable.grid_view_icon);

                        listView = true;
                        Utils.setLibraryView("ListView");
                        Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "List_View", customerId);

                        if (isSearch) {
                            libraryBookView.switchToListView();
                            mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, mSearchBooksAdapter.getBookItems(), searchBookList, R.layout.library_list, "ListView", bookItemListener);
                            libraryBookView.setAdapter(mSearchBooksAdapter);
                        } else {
                            libraryBookView.switchToListView();
                            mDownloadedBooksAdapter = new BooksAdapter(LibraryActivity.this, mDownloadedBooksAdapter.getBookItems(), R.layout.library_list, "ListView", bookItemListener);
                            libraryBookView.setAdapter(mDownloadedBooksAdapter);
                        }
                    } else if (libraryViewChange.getTag().equals(R.drawable.grid_view_icon)) {
                        libraryViewChange.setTag(R.drawable.list_view_icon);
                        libraryViewChange.setImageResource(R.drawable.list_view_icon);

                        listView = false;
                        Utils.setLibraryView("GridView");
                        Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "Grid_View", customerId);

                        if (isSearch) {
                            libraryBookView.switchToGridView();
                            mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, mSearchBooksAdapter.getBookItems(), searchBookList, R.layout.library_grid, "GridView", bookItemListener);
                            libraryBookView.setAdapter(mSearchBooksAdapter);
                        } else {
                            libraryBookView.switchToGridView();
                            mDownloadedBooksAdapter = new BooksAdapter(LibraryActivity.this, mDownloadedBooksAdapter.getBookItems(), R.layout.library_grid, "GridView", bookItemListener);
                            libraryBookView.setAdapter(mDownloadedBooksAdapter);
                        }
                    }
                } else {
                    try {
                        int currentPosition = libraryPager.getCurrentItem();
                        LibraryFragment currentFragment = libraryAdapter.fragments[currentPosition];
                        if (libraryViewChange.getTag().equals(R.drawable.list_view_icon)) {
                            libraryViewChange.setTag(R.drawable.grid_view_icon);
                            libraryViewChange.setImageResource(R.drawable.grid_view_icon);

                            //update list views of current displayed fragment
                            currentFragment.setViewToListView();
                            listView = true;
                            Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "List_View", customerId);

                            if (isSearch) {
                                libraryBookView.switchToListView();
                                mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, mSearchBooksAdapter.getBookItems(), searchBookList, R.layout.library_list, "ListView", bookItemListener);
                                libraryBookView.setAdapter(mSearchBooksAdapter);
                            } else {
                                libraryBookView.switchToListView();
                                mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, searchBookList, R.layout.library_list, "ListView", bookItemListener);
                                libraryBookView.setAdapter(mSearchBooksAdapter);
                            }

                            //update list views of cached fragment
                            new UpdateChachedViewPager(currentPosition).execute("ListView");
                        } else if (libraryViewChange.getTag().equals(R.drawable.grid_view_icon)) {
                            libraryViewChange.setTag(R.drawable.list_view_icon);
                            libraryViewChange.setImageResource(R.drawable.list_view_icon);

                            //update grid views of current displayed fragment
                            currentFragment.setViewToGridView();
                            listView = false;
                            Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "Grid_View", customerId);

                            if (isSearch) {
                                libraryBookView.switchToGridView();
                                mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, mSearchBooksAdapter.getBookItems(), searchBookList, R.layout.library_grid, "GridView", bookItemListener);
                                libraryBookView.setAdapter(mSearchBooksAdapter);
                            } else {
                                libraryBookView.switchToGridView();
                                mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, searchBookList, R.layout.library_grid, "GridView", bookItemListener);
                                libraryBookView.setAdapter(mSearchBooksAdapter);
                            }

                            //update grid views of cached fragment
                            new UpdateChachedViewPager(currentPosition).execute("GridView");
                        }
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, mToolbar, R.string.app_name, R.string.app_name) {
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);
        leftDrawerList = (ExpandableListView) findViewById(R.id.menu_list_view);
        ((LinearLayout) dashboardMenuContainer.findViewById(R.id.menu_list_header_back_button)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });

        libraryBookView = (LibraryAutofitRecyclerView) findViewById(R.id.libraryBookView);
        libraryBookView.setHasFixedSize(true);

        ebookButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if (!Constants.getActivateBottomNavigationWidgetButton().equals("E-books")) {
                    disabledActivateBottomNavigationWidgetButton();

                    ebookButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_ebook, 0, 0);
                    ebookButton.setTypeface(null, Typeface.BOLD);
                    ebookButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));

                    Utils.showLibrary(LibraryActivity.this, "ebook");
                    Utils.triggerGAEvent(LibraryActivity.this, "BottomNavigation", "E-books", customerId);
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

                    Utils.showLibrary(LibraryActivity.this, "test_preparation, mock_test");
                    Utils.triggerGAEvent(LibraryActivity.this, "BottomNavigation", "Test Preparation", customerId);
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

                    Utils.showLibrary(LibraryActivity.this, "video");
                    Utils.triggerGAEvent(LibraryActivity.this, "BottomNavigation", "Video", customerId);
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

                    Utils.showStoreActivity(LibraryActivity.this);
                    Utils.triggerGAEvent(LibraryActivity.this, "BottomNavigation", "Store", customerId);
                }
            }
        });

        recommendationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if (!Constants.getActivateBottomNavigationWidgetButton().equals("Recommendation")) {
                    if (Utils.isNetworkConnected(LibraryActivity.this)) {
                        disabledActivateBottomNavigationWidgetButton();

                        recommendationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_recommendation, 0, 0);
                        recommendationButton.setTypeface(null, Typeface.BOLD);
                        recommendationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));

                        Utils.showRecommendationsActivity(LibraryActivity.this);
                    } else {
                        Utils.networkNotAvailableAlertBox(LibraryActivity.this);
                    }
                    Utils.triggerGAEvent(LibraryActivity.this, "BottomNavigation", "Recommendation", customerId);
                }
            }
        });

        libraryBookView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        setSearchToolbar();
    }

    @Override
    public void onBookClick(BookItem item, View bookView) {
        downloadAndOpenPDFFile(LibraryActivity.this, item, bookView);
    }

    @Override
    public void onBookLongClick(BookItem item, View bookView) {
        createBookOptionsDialog(LibraryActivity.this, item, bookView);
    }

    @Override
    public void onPermissionGranted() {
        new PrepareLibrary(LibraryActivity.this, isSync).execute();
        if (Utils.isNetworkConnected(LibraryActivity.this)) {
            Utils.triggerGAEventOnline(LibraryActivity.this, "Permission_Allow_" + tempScreenName, "Logged_In", customerId);
        }
    }

    @Override
    public void onPermissionDenied() {
        Utils.showPermissionInfoDialog(LibraryActivity.this);
        if (Utils.isNetworkConnected(LibraryActivity.this)) {
            Utils.triggerGAEventOnline(LibraryActivity.this, "Permission_Denied_" + tempScreenName, "Logged_In", customerId);
        }
    }

    @Override
    public void onPermissionPermanentlyDenied() {
        Utils.showPermissionInfoDialog(LibraryActivity.this);
        if (Utils.isNetworkConnected(LibraryActivity.this)) {
            Utils.triggerGAEventOnline(LibraryActivity.this, "Permission_Permanently_Denied_" + tempScreenName, "Logged_In", customerId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.library_menu, menu);
        mMenuItem = menu.findItem(R.id.search_icon);

        if (shouldSearchIconShow) {
            mMenuItem.setVisible(true);
        } else {
            mMenuItem.setVisible(false);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mPermissionHandler.onPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        productTypes = getIntent().getStringExtra("product_type");
        if (productTypes == null || productTypes.isEmpty() || productTypes.equals("")) {
            productTypes = "ebook";
        }

        //Enable Bottom Navigation Widget Button of MyLibrary
        disabledAllActivatedBottomNavigationWidgetButton();
        if (productTypes.equals("test_preparation, mock_test")) {
            Constants.setActivateBottomNavigationWidgetButton("Test Preparation");
            enableCurrentBottomNavigationWidgetButton();
        } else if (productTypes.equals("ebook")) {
            Constants.setActivateBottomNavigationWidgetButton("E-books");
            enableCurrentBottomNavigationWidgetButton();
        } else if (productTypes.equals("video")) {
            Constants.setActivateBottomNavigationWidgetButton("Video");
            enableCurrentBottomNavigationWidgetButton();
        } else if (productTypes.startsWith("downloaded_")) {
            Constants.setActivateBottomNavigationWidgetButton("NONE");
        }

        String tempScreenName = screenName + "_" + productTypes.replaceAll("(\\s|,)+", "_");

        Utils.triggerScreen(LibraryActivity.this, tempScreenName);

        // Prepare Library when user gave permissions from settings and came back to activity
        if (Utils.isPermissionDialogOpen()) {
            if (Utils.hasPermissions(LibraryActivity.this, Constants.STORAGE_PERMISSION)) {
                if (isFinishing()) {
                    Utils.closePermissionDialog();
                }
                onPermissionGranted();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        boolean shouldBackPress = false;
        if (Utils.isNetworkConnected(this)) {
            String appRated = AppSettings.getInstance(this).get("RateApp");
            if (appRated != null && !appRated.isEmpty() && appRated != "") {
                String[] appRatedArray = appRated.split("\\|");
                int remindDays = -1;
                if (appRatedArray[0].equals(AppRater.RATE_NOW)) {
                    remindDays = AppRater.RATE_NOW_REMIND_DAYS;
                } else if (appRatedArray[0].equals(AppRater.REMIND_LATER)) {
                    remindDays = AppRater.REMIND_LATER_REMIND_DAYS;
                } else if (appRatedArray[0].equals(AppRater.NO_THANKS)) {
                    remindDays = AppRater.NO_THANKS_REMIND_DAYS;
                }
                if (remindDays > 0) {
                    long remindTime = Long.parseLong(appRatedArray[1]) + ((long) remindDays * 86400000);    //1000*60*60*24
                    if (remindTime >= System.currentTimeMillis()) {
                        shouldBackPress = true;
                    }
                }
            }
        } else {
            shouldBackPress = true;
        }

        if (shouldBackPress) {
            super.onBackPressed();
            finish();
        } else {
            AppRater.showRateDialog(this);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem alertMenuItem = menu.findItem(R.id.premium_item);
        FrameLayout libraryPremiumView = (FrameLayout) alertMenuItem.getActionView();

        libraryPremiumView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(alertMenuItem);
            }
        });

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.search_icon:
                productTypes = getIntent().getStringExtra("product_type");
                if (productTypes == null || productTypes.isEmpty() || productTypes.equals("")) {
                    productTypes = "ebook";
                }
                if (searchBookList.size() <= 0) {
                    new GetAllBookList().execute();
                }
                isSearch = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    circleReveal(R.id.library_search_toolbar, 1, true, true);
                } else {
                    mSearchToolbar.setVisibility(View.VISIBLE);
                }
                mItemSearch.expandActionView();
                if (mLibraryTab != null) {
                    mLibraryTab.setVisibility(View.GONE);
                    libraryPager.setVisibility(View.GONE);
                }
                libraryBookView.setVisibility(View.VISIBLE);

                return true;
            case R.id.premium_item:
                if (Utils.isNetworkConnected(this)) {
                    Utils.showPremiumActivity(this);
                } else {
                    Utils.networkNotAvailableAlertBox(this);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setSearchToolbar() {
        mSearchToolbar = (Toolbar) findViewById(R.id.library_search_toolbar);
        if (mSearchToolbar != null) {
            mSearchToolbar.inflateMenu(R.menu.library_menu_search);
            mSearchMenu = mSearchToolbar.getMenu();

            mSearchToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        circleReveal(R.id.library_search_toolbar, 1, true, false);
                    } else {
                        mSearchToolbar.setVisibility(View.GONE);
                    }
                    isSearch = false;
                    if (mLibraryTab == null) {
                        libraryBookView.setVisibility(View.VISIBLE);
                    } else {
                        libraryBookView.setVisibility(View.GONE);
                        if (categoryList.size() >= 2) {
                            mLibraryTab.setVisibility(View.VISIBLE);
                        }
                        libraryPager.setVisibility(View.VISIBLE);
                    }
                }
            });

            mItemSearch = mSearchMenu.findItem(R.id.action_filter_search);

            MenuItemCompat.setOnActionExpandListener(mItemSearch, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    // Do something when collapsed
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        circleReveal(R.id.library_search_toolbar, 1, true, false);
                    } else {
                        mSearchToolbar.setVisibility(View.GONE);
                    }
                    isSearch = false;
                    if (mLibraryTab == null) {
                        libraryBookView.setVisibility(View.VISIBLE);
                    } else {
                        libraryBookView.setVisibility(View.GONE);
                        if (categoryList.size() >= 2) {
                            mLibraryTab.setVisibility(View.VISIBLE);
                        }
                        libraryPager.setVisibility(View.VISIBLE);
                    }

                    return true;
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Do something when expanded
                    return true;
                }
            });

            initSearchView();
        }
    }

    public void initSearchView() {
        final SearchView searchView = (SearchView) mSearchMenu.findItem(R.id.action_filter_search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // Enable/Disable Submit button in the keyboard
        searchView.setSubmitButtonEnabled(false);

        // Change search close button image
        ImageView closeButton = (ImageView) searchView.findViewById(R.id.search_close_btn);
        closeButton.setImageResource(R.drawable.ic_action_navigation_close);

        // set hint and the text colors
        EditText txtSearch = ((EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text));
        txtSearch.setHint(getResources().getString(R.string.search_books_hint));
        txtSearch.setHintTextColor(Color.DKGRAY);
        txtSearch.setTextColor(getResources().getColor(R.color.black));


        // set the cursor
        AutoCompleteTextView searchTextView = (AutoCompleteTextView) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(searchTextView, R.drawable.library_search_cursor); //This sets the cursor resource ID to 0 or @null which will make it visible on white background
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchBooksAdapter.getFilter().filter(query);
                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (mSearchBooksAdapter != null && query != null) {
                    mSearchBooksAdapter.getFilter().filter(query);
                }
                return true;
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void circleReveal(int viewID, int posFromRight, boolean containsOverflow, final boolean isShow) {
        final View myView = findViewById(viewID);

        int width = myView.getWidth();

        if (posFromRight > 0) {
            width -= (posFromRight * getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_material)) - (getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_material) / 2);
        }
        if (containsOverflow) {
            width -= getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_overflow_material);
        }

        int cx = width;
        int cy = myView.getHeight() / 2;

        Animator anim;
        if (isShow) {
            anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, (float) width);
        } else {
            anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, (float) width, 0);
        }

        anim.setDuration((long) 220);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isShow) {
                    super.onAnimationEnd(animation);
                    myView.setVisibility(View.INVISIBLE);
                }
            }
        });

        // make the view visible and start the animation
        if (isShow) {
            myView.setVisibility(View.VISIBLE);
        }

        // start the animation
        anim.start();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;
        if (listView) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "List_View_Portrait", customerId);
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "List_View_Landscape", customerId);
            }
        } else {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "Grid_View_Portrait", customerId);
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Utils.triggerGAEvent(LibraryActivity.this, "My_Library_View", "Grid_View_Landscape", customerId);
            }
        }
    }

    public static boolean isListView() {
        return listView;
    }

    public void OnNoProductButtonClick(View v) {
        productTypes = getIntent().getStringExtra("product_type");
        if (productTypes == null || productTypes.isEmpty() || productTypes.equals("")) {
            productTypes = "ebook";
        }
        if (productTypes.startsWith("downloaded_")) {
            Utils.showLibrary(this, "ebook");
        } else if (productTypes.equals("test_preparation, mock_test")) {
            String url = Constants.INDEX_URL + "?route=product/category/byParams&product_type[]=test_preparation&product_type[]=mock_test";
            Utils.showWebViewActivity(LibraryActivity.this, url);
        } else if (productTypes.equals("video")) {
            String url = Constants.INDEX_URL + "?route=product/category/byParams&product_type[]=video";
            Utils.showWebViewActivity(LibraryActivity.this, url);
        }
    }

    public void setLibraryViewWhenNoDataFound(Drawable image, String text, String subText, String buttonText) {
        noProductImageView.setImageDrawable(image);
        noProductText.setText(text);
        noProductSubText.setText(subText);
        noProductButton.setText(buttonText);
    }

    public void OnRefreshButtonClick(View v) {
        if (Utils.isNetworkConnected(this)) {
            new PrepareLibrary(LibraryActivity.this, true).execute();
        }
    }

    public void OnSettingsButtonClick(View v) {
        Activity activity = (Activity) this;
        activity.startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
    }

    public void OnExploreButtonClick(View view) {
        if (Utils.isNetworkConnected(LibraryActivity.this)) {
            Utils.showStoreActivity(LibraryActivity.this);
            Utils.triggerGAEventOnline(LibraryActivity.this, "Library_To_Store", customerId, "");
        } else {
            Utils.networkNotAvailableAlertBox(LibraryActivity.this);
        }
    }

    public void OnSearchToStoreButtonClick(View v) {
        if (Utils.isNetworkConnected(this)) {
            String searchKeyword = Utils.getSearchInStoreKeyword();
            if (searchKeyword != null && !searchKeyword.isEmpty() && !searchKeyword.equals("")) {
                Utils.showWebViewActivity(LibraryActivity.this, Constants.SEARCH_IN_STORE_URL + "&filter_name=" + Utils.getSearchInStoreKeyword());
                Utils.triggerGAEventOnline(LibraryActivity.this, "Library_Search_To_Store", customerId, "");
            }
        } else {
            Utils.networkNotAvailableAlertBox(LibraryActivity.this);
        }
    }

    private void downloadAndOpenPDFFile(Context mContext, BookItem clickedBook, View bookView) {
        String productType = clickedBook.getProductType();
        String productId = clickedBook.getProductId();
        if (productType.equals("ebook")) {
            final String pdfUrl = clickedBook.getProductLink();
            File pdfFile = new File(Utils.getFileDownloadPath(mContext, pdfUrl));
            if (!pdfFile.exists()) {
                if (Utils.isNetworkConnected(mContext)) {
                    pDownloadDialog = new Dialog(mContext);
                    pDownloadDialog.setCancelable(false);
                    pDownloadDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                    View downloadProgress = inflater.inflate(R.layout.download_progress, null);
                    pDownloadDialog.setContentView(downloadProgress);
                    pDownloadDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    final ImageView downloadBookImage = (ImageView) downloadProgress.findViewById(R.id.download_book_image);
                    final TextView downloadBookName = (TextView) downloadProgress.findViewById(R.id.download_book_name);
                    downloadBookName.setText(clickedBook.getName());
                    downloadProgressBar = (ProgressBar) downloadProgress.findViewById(R.id.download_bar);
                    downloadCancel = (ImageButton) downloadProgress.findViewById(R.id.download_cancel);
                    downloadBarCompleted = (TextView) downloadProgress.findViewById(R.id.download_bar_completed);

                    pDownloadDialog.show();

                    Utils.getImageLoader(mContext).loadImage(clickedBook.getImageURL(), new ImageLoadingListener() {

                        @Override
                        public void onLoadingStarted(String paramString, View paramView) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onLoadingFailed(String paramString, View paramView, FailReason paramFailReason) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onLoadingComplete(String paramString, View paramView, Bitmap paramBitmap) {
                            // TODO Auto-generated method stub
                            downloadBookImage.setImageBitmap(paramBitmap);
                            downloadBookName.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingCancelled(String paramString, View paramView) {
                        }
                    });

                    downloadCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            pDownloadDialog.dismiss();
                            DownloadFileService.isCancelled = true;
                        }
                    });

                    DownloadFileService.isCancelled = false;
                    Intent intent = new Intent(this, DownloadFileService.class);
                    intent.putExtra("productId", clickedBook.getProductId());
                    intent.putExtra("customerId", customerId);
                    intent.putExtra("url", pdfUrl);
                    intent.putExtra("receiver", new DownloadFileReceiver(new Handler(), clickedBook, bookView));
                    startService(intent);
                } else {
                    Utils.networkNotAvailableAlertBox(mContext);
                }
            } else {
                Utils.openPDFFile(mContext, clickedBook);
            }
        } else if (productType.equals("test_preparation") || productType.equals("mock_test")) {
            if (Utils.isNetworkConnected(mContext)) {
                Intent webViewIntent = new Intent(mContext, WebViewActivity.class);
                webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                webViewIntent.putExtra("product_id", productId);
                webViewIntent.putExtra("product_type", productType);
                webViewIntent.putExtra("web_url", clickedBook.getProductLink());
                startActivity(webViewIntent);
                ((Activity) mContext).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            } else {
                Utils.networkNotAvailableAlertBox(mContext);
            }
        } else if (productType.equals("video")) {
            if (Utils.isNetworkConnected(mContext)) {
                Intent videoViewIntent = new Intent(mContext, VideoViewActivity.class);
//                Intent videoViewIntent = new Intent(mContext, FeedbackActivity.class);
                videoViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                videoViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                HashMap<String, String> bookDetails = new HashMap<String, String>();
                bookDetails.put("name", clickedBook.getName());
                bookDetails.put("image_url", clickedBook.getImageURL());
                bookDetails.put("description", clickedBook.getDescription());
                bookDetails.put("product_id", clickedBook.getProductId());
                bookDetails.put("product_type", clickedBook.getProductType());
                bookDetails.put("product_link", clickedBook.getProductLink());
                videoViewIntent.putExtra("product_detail", bookDetails);
                startActivity(videoViewIntent);
                getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            } else {
                Utils.networkNotAvailableAlertBox(mContext);
            }
        }
    }

    private void createBookOptionsDialog(final Context mContext, final BookItem clickedBook, final View bookView) {
        productTypes = getIntent().getStringExtra("product_type");
        if (productTypes == null || productTypes.isEmpty() || productTypes.equals("")) {
            productTypes = "ebook";
        }

        AlertDialog.Builder bookOptionsBuilder = new AlertDialog.Builder(mContext);

        String bookName = clickedBook.getName();
        bookOptionsBuilder.setTitle(bookName);
        if (bookName.length() > 50) {
            bookOptionsBuilder.setTitle(bookName.substring(0, 47) + "...");
        }
        final String productType = clickedBook.getProductType();
        if (productType.equals("test_preparation") || productType.equals("mock_test")) {
            String[] bookOptions = {"Start Test Now", "Cancel"};
            bookOptionsBuilder.setItems(bookOptions, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    switch (which) {
                        case 0:
                            downloadAndOpenPDFFile(mContext, clickedBook, bookView);
                            break;
                        case 1:
                            dialog.cancel();
                            break;
                    }
                }
            });
        } else if (productType.equals("video")) {
            String[] bookOptions = {"Play", "Cancel"};
            bookOptionsBuilder.setItems(bookOptions, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    switch (which) {
                        case 0:
                            downloadAndOpenPDFFile(mContext, clickedBook, bookView);
                            break;
                        case 1:
                            dialog.cancel();
                            break;
                    }
                }
            });
        } else if (productType.equals("ebook")) {
            String pdfUrl = clickedBook.getProductLink();
            final String productId = clickedBook.getProductId();
            final File pdfFile = new File(Utils.getFileDownloadPath(mContext, pdfUrl));
            if (!pdfFile.exists()) {
                String[] bookOptions = {"Download on device", "Cancel"};
                bookOptionsBuilder.setItems(bookOptions, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        switch (which) {
                            case 0:
                                downloadAndOpenPDFFile(mContext, clickedBook, bookView);
                                break;
                            case 1:
                                dialog.cancel();
                                break;
                        }
                    }
                });
            } else {
                String[] bookOptions = {"Read", "Remove from device", "Cancel"};
                bookOptionsBuilder.setItems(bookOptions, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        switch (which) {
                            case 0:
                                downloadAndOpenPDFFile(mContext, clickedBook, bookView);
                                break;
                            case 1:
                                pdfFile.delete();
                                Utils.triggerGAEvent(mContext, "Pdf_Delete", productId, customerId);

                                // Delete downloaded book details from database
                                String query = "DELETE FROM " + DatabaseConstants.DatabaseEntry.DOWNLOADED_BOOKS_TABLE_NAME + " WHERE product_id = " + productId;
                                DatabaseHelper.getInstance(LibraryActivity.this).deleteData(query);

                                ImageView bookDepiction = (ImageView) bookView.findViewById(R.id.book_depiction);
                                if (bookDepiction != null) {
                                    bookDepiction.setImageResource(R.drawable.cloud_icon);
                                }
                                if (productTypes.startsWith("downloaded_")) {
                                    mDownloadedBooksAdapter.removeItem(clickedBook);
                                    int downloadedBooks = mDownloadedBooksAdapter.getItemCount();
                                    if (downloadedBooks == 0) {
                                        shouldSearchIconShow = false;
                                        libraryViewChange.setVisibility(View.INVISIBLE);
                                        libraryFlipper.setDisplayedChild(1);
                                        Drawable image = getResources().getDrawable(R.drawable.no_download_ebooks);
                                        String text = "No Downloads Yet";
                                        String subText = "Download your eBooks by Clicking on it\nTo Read without Internet Connection.";
                                        String buttonText = "My Books";
                                        setLibraryViewWhenNoDataFound(image, text, subText, buttonText);

                                        invalidateOptionsMenu();
                                    }
                                }
                                break;
                            case 2:
                                dialog.cancel();
                                break;
                        }
                    }
                });
            }
        }

        AlertDialog bookOptionsDialog = bookOptionsBuilder.create();
        bookOptionsDialog.setCanceledOnTouchOutside(true);
        bookOptionsDialog.show();
    }

    public class PrepareLibrary extends SyncData {
        private Context mContext;

        public PrepareLibrary(Context mContext, boolean syncFromAPI) {
            super(mContext, syncFromAPI);

            bottomNavigationWidget.setVisibility(View.INVISIBLE);
            mProgressBar = (ProgressBar) findViewById(R.id.library_progressBar);
            mProgressBar.incrementProgressBy(1);
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.bringToFront();

            // If using native accentColor (SDK < 21)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                int color = Color.parseColor("#383838");
                mProgressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                mProgressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }

            this.mContext = mContext;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            libraryFlipper.setDisplayedChild(0);
            bottomNavigationWidget.setVisibility(View.VISIBLE);
            libraryViewChange.setVisibility(View.VISIBLE);
            shouldSearchIconShow = true;
            if (productTypes.startsWith("downloaded_")) {
                libraryBookView.setVisibility(View.VISIBLE);
                new PrepareDownloadedBookLibrary().execute();
            } else {
                categoryList = Utils.getTabs(mContext, productTypes);
                if (categoryList != null && categoryList.size() > 0) {
                    categoryList.add("Store");
                    categoryList.add("goPremium");
                    totalCategoryList = categoryList.size();
                    libraryAdapter = new LibraryPagerAdapter(getSupportFragmentManager(), mContext, categoryList);
                    libraryPager = (ViewPager) findViewById(R.id.view_pager);
                    int offScreenPageLimit = categoryList.size();
                    if (offScreenPageLimit > 4) {
                        offScreenPageLimit = 3;
                    }
                    libraryPager.setOffscreenPageLimit(offScreenPageLimit);
                    libraryPager.setAdapter(libraryAdapter);

                    mLibraryTab = (LibrarySlidingTabStrip) findViewById(R.id.library_tab);
                    if (categoryList.size() >= 2) {
                        mLibraryTab.setVisibility(View.VISIBLE);
                    }
                    mLibraryTab.setTabNames(categoryList);
                    mLibraryTab.setViewPager(libraryPager);

                    mLibraryTab.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                        @Override
                        public void onPageSelected(int arg0) {
                            int currentPosition = libraryPager.getCurrentItem();
                            if (currentPosition == (totalCategoryList - 1) || currentPosition == (totalCategoryList - 2)) {
                                if (libraryViewChange.getVisibility() == View.VISIBLE) {
                                    libraryViewChange.setVisibility(View.INVISIBLE);
                                }
                            } else {
                                if (libraryViewChange.getVisibility() == View.INVISIBLE) {
                                    libraryViewChange.setVisibility(View.VISIBLE);
                                }
                            }

                            if (bottomNavigationWidget.getVisibility() == View.GONE) {
                                bottomNavigationWidget.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onPageScrolled(int arg0, float arg1, int arg2) {
                            // TODO Auto-generated method stub
                        }

                        @Override
                        public void onPageScrollStateChanged(int arg0) {
                            // TODO Auto-generated method stub
                        }
                    });
                } else {
                    shouldSearchIconShow = false;
                    if (productTypes.equals("test_preparation, mock_test")) {
                        libraryViewChange.setVisibility(View.INVISIBLE);
                        libraryFlipper.setDisplayedChild(1);
                        Drawable image = getResources().getDrawable(R.drawable.no_test_preparations);
                        String text = "No Mock Test / Test Preparation\nin your Account Yet.";
                        String subText = "Browse your Subject related Mock Test or\nTest Preparation";
                        String buttonText = "Shop Now";
                        setLibraryViewWhenNoDataFound(image, text, subText, buttonText);
                    } else if (productTypes.equals("video")) {
                        libraryViewChange.setVisibility(View.INVISIBLE);
                        libraryFlipper.setDisplayedChild(1);
                        Drawable image = getResources().getDrawable(R.drawable.no_video);
                        String text = "No Videos in your Account Yet.";
                        String subText = "Click here to browse Videos";
                        String buttonText = "Shop Now";
                        setLibraryViewWhenNoDataFound(image, text, subText, buttonText);
                    } else {
                        libraryViewChange.setVisibility(View.INVISIBLE);
                        libraryFlipper.setDisplayedChild(2);
                    }
                }
            }

            navigationDrawerAdapter = new MenuListAdapter(mContext, "Preparing Library, Please wait...", drawerLayout);
            List<HashMap<String, String>> menuItems = new ArrayList<HashMap<String, String>>();
            LinkedList<HashMap<String, String>> menuItemList = Utils.getMenuItems();
            if (menuItemList != null) {
                for (HashMap<String, String> item : menuItemList) {
                    menuItems.add(navigationDrawerAdapter.createMenuItem(item.get("text"), item.get("left_drawer_icon"), item.get("product_type")));
                }
                navigationDrawerAdapter.setMenuGroupsItems("My Library", menuItems);
            }
            leftDrawerList.setAdapter(navigationDrawerAdapter);
            leftDrawerList.setOnChildClickListener(navigationDrawerAdapter);

            String versionPopupExpiryTime = AppSettings.getInstance(LibraryActivity.this).get("version_popup_expiry_time");
            if (versionPopupExpiryTime == null || versionPopupExpiryTime.isEmpty() || versionPopupExpiryTime.equals("") || System.currentTimeMillis() > Long.parseLong(versionPopupExpiryTime)) {
                if (Utils.isNetworkConnected(LibraryActivity.this)) {
                    new GetLatestVersion().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

            invalidateOptionsMenu();
            mProgressBar.setVisibility(View.GONE);

            // Update Library Book If user purchased
            if (Utils.isNetworkConnected(LibraryActivity.this)) {
                new CustomerDetails().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            // Show alert when user internally delete downloaded books or downloaded books are corrupted
            if (productTypes.equals("ebook")) {
                new ShowDeletedBookList().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    public class LibraryPagerAdapter extends FragmentStatePagerAdapter {

        private Context mContext;
        private List<String> tabItems;
        private FragmentManager fragmentManager;
        private LibraryFragment[] fragments;

        public LibraryPagerAdapter(FragmentManager fm, Context mContext, List<String> tabItems) {
            super(fm);
            this.mContext = mContext;
            this.tabItems = tabItems;

            fragmentManager = fm;
            fragments = new LibraryFragment[tabItems.size()];
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
            return tabItems.size();
        }

        @Override
        public LibraryFragment getItem(int position) {
            assert (0 <= position && position < fragments.length);
            if (fragments[position] == null) {
                fragments[position] = new LibraryFragment(mContext, productTypes, totalCategoryList, position, bookItemListener);
            }

            return fragments[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabItems.get(position);
        }
    }

    private class UpdateChachedViewPager extends AsyncTask<String, Void, Void> {
        private int currentPosition;
        private String view;

        public UpdateChachedViewPager(int currentPosition) {
            this.currentPosition = currentPosition;
        }

        @Override
        protected Void doInBackground(String... params) {
            view = params[0];
            Utils.setLibraryView(view);

            return null;
        }

        @Override
        protected void onPostExecute(Void s) {
            super.onPostExecute(s);

            if (view.equals("ListView")) {
                for (int i = 0; i < libraryAdapter.fragments.length - 2; i++) {
                    if (i == currentPosition)
                        continue;
                    LibraryFragment displayFragment = libraryAdapter.fragments[i];
                    if (displayFragment != null) {
                        displayFragment.setViewToListView();
                    }
                }
            } else if (view.equals("GridView")) {
                for (int i = 0; i < libraryAdapter.fragments.length - 2; i++) {
                    if (i == currentPosition)
                        continue;
                    LibraryFragment displayFragment = libraryAdapter.fragments[i];
                    if (displayFragment != null) {
                        displayFragment.setViewToGridView();
                    }
                }
            }
        }
    }

    public class PrepareDownloadedBookLibrary extends AsyncTask<Void, BookItem, List<BookItem>> {
        private List<BookItem> downloadedBookList = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (listView) {
                mDownloadedBooksAdapter = new BooksAdapter(LibraryActivity.this, R.layout.library_list, "ListView", bookItemListener);
            } else {
                mDownloadedBooksAdapter = new BooksAdapter(LibraryActivity.this, R.layout.library_grid, "GridView", bookItemListener);
            }
            libraryBookView.setAdapter(mDownloadedBooksAdapter);
        }

        @Override
        protected List<BookItem> doInBackground(Void... params) {
            Set<HashMap<String, String>> booksList = new HashSet<HashMap<String, String>>();
            try {
                String tempProductTypes = productTypes;
                if (tempProductTypes.startsWith("downloaded_")) {
                    productTypes = productTypes.replace("downloaded_", "");
                }
                String jsonFileName = productTypes.replaceAll("(\\s|,)+", "_") + "_by_categories.json";
                File jsonFile = new File(Utils.getDirectory(LibraryActivity.this) + jsonFileName);
                long serverTime = jsonFile.lastModified(), systemTime = new Date().getTime();

                FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
                byte[] fileRead = new byte[jsonFileInputStream.available()];
                jsonFileInputStream.read(fileRead);
                jsonFileInputStream.close();
                JSONObject productTypeJsonObject = new JSONObject(new String(Base64.decode(fileRead, Base64.DEFAULT)));
                JSONArray productsByCategories = productTypeJsonObject.getJSONArray("products");
                for (int i = 0; i < productsByCategories.length(); i++) {
                    List<HashMap<String, String>> tempBooksList = new ArrayList<HashMap<String, String>>();
                    tempBooksList = new Gson().fromJson(productsByCategories.getJSONObject(i).getJSONArray("products").toString(), new TypeToken<ArrayList<HashMap<String, String>>>() {
                    }.getType());
                    booksList.addAll(tempBooksList);
                }

                for (HashMap<String, String> book : booksList) {
                    boolean shouldIncludeBook = true;
                    if (tempProductTypes.startsWith("downloaded_")) {
                        File pdfFile = new File(Utils.getFileDownloadPath(LibraryActivity.this, book.get("product_link")));
                        if (!pdfFile.exists())
                            shouldIncludeBook = false;
                    }
                    if (shouldIncludeBook) {
                        book.put("image_url", Constants.IMAGE_URL + book.get("image_url"));
                        long diffDays = Math.abs(systemTime - serverTime) / 86400000; // Days calculation by 1000*60*60*24
                        if (book.get("left_days") == null || book.get("left_days").isEmpty() || book.get("left_days").equals("")) {
                            book.put("left_days", "999999");
                        }
                        int leftDays = Integer.valueOf(book.get("left_days")).intValue();
                        if (leftDays == 999999) {
                            BookItem bookItem = new BookItem(book.get("name"), book.get("order_product_id"), book.get("cidd"), book.get("image_url"), book.get("description"), book.get("price"), book.get("left_days"), book.get("product_id"), book.get("product_type"), book.get("option_name"), book.get("option_value"), book.get("pdf_downloaded_date"), book.get("license_period"), book.get("product_link"), book.get("date_added"));
                            downloadedBookList.add(bookItem);

                            publishProgress(bookItem);
                        } else {
                            if ((leftDays - diffDays) >= 0) {
                                BookItem bookItem = new BookItem(book.get("name"), book.get("order_product_id"), book.get("cidd"), book.get("image_url"), book.get("description"), book.get("price"), book.get("left_days"), book.get("product_id"), book.get("product_type"), book.get("option_name"), book.get("option_value"), book.get("pdf_downloaded_date"), book.get("license_period"), book.get("product_link"), book.get("date_added"));
                                downloadedBookList.add(bookItem);

                                publishProgress(bookItem);
                            } else {
                                Utils.removeExpiredFiles(LibraryActivity.this, book.get("product_link"), book.get("image_url"), book.get("product_id"));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return downloadedBookList;
        }

        @Override
        protected void onProgressUpdate(BookItem... values) {
            super.onProgressUpdate(values);
            mDownloadedBooksAdapter.setItem(values[0]);
        }

        @Override
        protected void onPostExecute(List<BookItem> downloadedBookList) {
            super.onPostExecute(downloadedBookList);

            if (downloadedBookList.size() <= 0) {
                shouldSearchIconShow = false;
                libraryViewChange.setVisibility(View.INVISIBLE);
                libraryFlipper.setDisplayedChild(1);
                Drawable image = getResources().getDrawable(R.drawable.no_download_ebooks);
                String text = "No Downloads Yet";
                String subText = "Download your eBooks by Clicking on it\nTo Read without Internet Connection.";
                String buttonText = "My Books";
                setLibraryViewWhenNoDataFound(image, text, subText, buttonText);

                invalidateOptionsMenu();
            }
        }
    }

    public class GetAllBookList extends AsyncTask<Void, BookItem, List<BookItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (listView) {
                mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, R.layout.library_list, "ListView", bookItemListener);
            } else {
                mSearchBooksAdapter = new BooksAdapter(LibraryActivity.this, R.layout.library_grid, "GridView", bookItemListener);
            }
            libraryBookView.setAdapter(mSearchBooksAdapter);
        }

        @Override
        protected List<BookItem> doInBackground(Void... params) {
            Set<HashMap<String, String>> booksList = new HashSet<HashMap<String, String>>();
            try {
                String tempProductTypes = productTypes;
                if (tempProductTypes.startsWith("downloaded_")) {
                    productTypes = productTypes.replace("downloaded_", "");
                }
                String jsonFileName = productTypes.replaceAll("(\\s|,)+", "_") + "_by_categories.json";
                File jsonFile = new File(Utils.getDirectory(LibraryActivity.this) + jsonFileName);
                long serverTime = jsonFile.lastModified(), systemTime = new Date().getTime();

                FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
                byte[] fileRead = new byte[jsonFileInputStream.available()];
                jsonFileInputStream.read(fileRead);
                jsonFileInputStream.close();
                JSONObject productTypeJsonObject = new JSONObject(new String(Base64.decode(fileRead, Base64.DEFAULT)));
                JSONArray productsByCategories = productTypeJsonObject.getJSONArray("products");
                for (int i = 0; i < productsByCategories.length(); i++) {
                    List<HashMap<String, String>> tempBooksList = new ArrayList<HashMap<String, String>>();
                    tempBooksList = new Gson().fromJson(productsByCategories.getJSONObject(i).getJSONArray("products").toString(), new TypeToken<ArrayList<HashMap<String, String>>>() {
                    }.getType());
                    booksList.addAll(tempBooksList);
                }

                for (HashMap<String, String> book : booksList) {
                    boolean shouldIncludeBook = true;
                    if (tempProductTypes.startsWith("downloaded_")) {
                        File pdfFile = new File(Utils.getFileDownloadPath(LibraryActivity.this, book.get("product_link")));
                        if (!pdfFile.exists())
                            shouldIncludeBook = false;
                    }
                    if (shouldIncludeBook) {
                        book.put("image_url", Constants.IMAGE_URL + book.get("image_url"));
                        long diffDays = Math.abs(systemTime - serverTime) / 86400000; // Days calculation by 1000*60*60*24
                        if (book.get("left_days") == null || book.get("left_days").isEmpty() || book.get("left_days").equals("")) {
                            book.put("left_days", "999999");
                        }
                        int leftDays = Integer.valueOf(book.get("left_days")).intValue();
                        if (leftDays == 999999) {
                            BookItem bookItem = new BookItem(book.get("name"), book.get("order_product_id"), book.get("cidd"), book.get("image_url"), book.get("description"), book.get("price"), book.get("left_days"), book.get("product_id"), book.get("product_type"), book.get("option_name"), book.get("option_value"), book.get("pdf_downloaded_date"), book.get("license_period"), book.get("product_link"), book.get("date_added"));
                            searchBookList.add(bookItem);

                            publishProgress(bookItem);
                        } else {
                            if ((leftDays - diffDays) >= 0) {
                                BookItem bookItem = new BookItem(book.get("name"), book.get("order_product_id"), book.get("cidd"), book.get("image_url"), book.get("description"), book.get("price"), book.get("left_days"), book.get("product_id"), book.get("product_type"), book.get("option_name"), book.get("option_value"), book.get("pdf_downloaded_date"), book.get("license_period"), book.get("product_link"), book.get("date_added"));
                                searchBookList.add(bookItem);

                                publishProgress(bookItem);
                            } else {
                                Utils.removeExpiredFiles(LibraryActivity.this, book.get("product_link"), book.get("image_url"), book.get("product_id"));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return searchBookList;
        }

        @Override
        protected void onProgressUpdate(BookItem... values) {
            super.onProgressUpdate(values);
            mSearchBooksAdapter.setItem(values[0]);
        }

        @Override
        protected void onPostExecute(List<BookItem> searchBookList) {
            super.onPostExecute(searchBookList);
        }
    }

    private class DownloadFileReceiver extends ResultReceiver {
        private BookItem book;
        private View bookView;

        public DownloadFileReceiver(Handler handler, BookItem book, View bookView) {
            super(handler);
            this.book = book;
            this.bookView = bookView;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            try {
                if (resultCode == DownloadFileService.UPDATE_PROGRESS) {
                    int progress = resultData.getInt("progress");
                    downloadProgressBar.setProgress(progress);
                    downloadBarCompleted.setText(progress + "%");
                } else if (resultCode == DownloadFileService.FINISH_PROGRESS) {
                    pDownloadDialog.dismiss();
                    Utils.triggerGAEvent(LibraryActivity.this, "Pdf_Download_Completed", book.getProductId(), customerId);
                    ImageView bookDepiction = (ImageView) bookView.findViewById(R.id.book_depiction);
                    if (bookDepiction != null) {
                        bookDepiction.setImageResource(R.drawable.success_icon);
                    }
                    Utils.openPDFFile(LibraryActivity.this, book);

                    // Insert downloaded book details into database table
                    new InsertDownloadedBook(book).execute();

                } else if (resultCode == DownloadFileService.ERROR_PROGRESS) {
                    pDownloadDialog.dismiss();
                    String response = resultData.getString("response");
                    String pdfUrl = resultData.getString("pdfUrl");
                    Constants.showToast("Download error: " + response, LibraryActivity.this);
                    File outputFile = new File(Utils.getFileDownloadPath(LibraryActivity.this, pdfUrl));
                    if (outputFile.exists()) {
                        outputFile.delete();
                    }
                } else if (resultCode == DownloadFileService.CANCEL_BEFORE_DOWNLOAD_START) {
                    pDownloadDialog.dismiss();
                } else if (resultCode == DownloadFileService.TERMINATE_DOWNLOADING) {
                    pDownloadDialog.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class ShowDeletedBookList extends AsyncTask<Void, Void, List<String>> {
        private List<String> deletedProductIds = new ArrayList<String>();

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> deletedBookNames = new ArrayList<String>();
            int i = 0;
            List<HashMap<String, String>> bookDetails = DatabaseHelper.getInstance(LibraryActivity.this).getDetails(DatabaseConstants.DatabaseEntry.DOWNLOADED_BOOKS_TABLE_NAME, null, null, -1);
            for (HashMap<String, String> book : bookDetails) {
                File pdfFile = new File(Utils.getFileDownloadPath(LibraryActivity.this, book.get("product_link")));
                if (!pdfFile.exists()) {
                    deletedBookNames.add(book.get("name"));
                    deletedProductIds.add(book.get("product_id"));
                }
            }

            return deletedBookNames;
        }

        @Override
        protected void onPostExecute(List<String> bookList) {
            super.onPostExecute(bookList);

            if (bookList.size() > 0) {
                final AlertDialog alertDialog = Utils.createAlertBox(LibraryActivity.this, R.layout.library_dialog_box);
                alertDialog.show();

                TextView title = (TextView) alertDialog.findViewById(R.id.dialog_title_text);
                ListView deletedBookListView = (ListView) alertDialog.findViewById(R.id.dialog_list_view);
                TextView infoText = (TextView) alertDialog.findViewById(R.id.dialog_info_text);
                Button button = (Button) alertDialog.findViewById(R.id.dialog_bottom_button);

                title.setText("List of Deleted Books");
                infoText.setText("Click on individual books again to Re-download");
                button.setText("Continue");

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(LibraryActivity.this, R.layout.library_textview, bookList.toArray(new String[0]));
                deletedBookListView.setAdapter(adapter);

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });

                String productIds = deletedProductIds.toString();
                String query = "DELETE FROM " + DatabaseConstants.DatabaseEntry.DOWNLOADED_BOOKS_TABLE_NAME + " WHERE product_id IN(" + productIds.substring(1, productIds.length() - 1) + ")";
                DatabaseHelper.getInstance(LibraryActivity.this).deleteData(query);
            }
        }
    }

    public class InsertDownloadedBook extends AsyncTask<Void, Void, Void> {
        private BookItem book;

        public InsertDownloadedBook(BookItem book) {
            this.book = book;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentValues values = new ContentValues();
            /*values.put(StoreCategoryEntry.COLUMN_ID, "");*/
            values.put(DatabaseConstants.BookEntry.COLUMN_CUSTOMER_ID, customerId);
            values.put(DatabaseConstants.BookEntry.COLUMN_PRODUCT_ID, book.getProductId());
            values.put(DatabaseConstants.BookEntry.COLUMN_ORDER_PRODUCT_ID, book.getOrderProductId());
            values.put(DatabaseConstants.BookEntry.COLUMN_CIDD, book.getCidd());
            values.put(DatabaseConstants.BookEntry.COLUMN_NAME, book.getName());
            values.put(DatabaseConstants.BookEntry.COLUMN_IMAGE_URL, book.getImageURL());
            values.put(DatabaseConstants.BookEntry.COLUMN_DESCRIPTION, book.getDescription());
            values.put(DatabaseConstants.BookEntry.COLUMN_PRICE, book.getPrice());
            values.put(DatabaseConstants.BookEntry.COLUMN_LEFT_DAYS, book.getLeftDays());
            values.put(DatabaseConstants.BookEntry.COLUMN_PRODUCT_TYPE, book.getProductType());
            values.put(DatabaseConstants.BookEntry.COLUMN_PDF_DOWNLOADED_DATE, book.getPdfDownloadedDate());
            values.put(DatabaseConstants.BookEntry.COLUMN_LICENCE_PERIOD, book.getLicensePeriod());
            values.put(DatabaseConstants.BookEntry.COLUMN_PRODUCT_LINK, book.getProductLink());

            //insert into database
            DatabaseHelper.getInstance(LibraryActivity.this).insertSingleData(DatabaseConstants.DatabaseEntry.DOWNLOADED_BOOKS_TABLE_NAME, values);

            return null;
        }
    }

    protected void enableCurrentBottomNavigationWidgetButton() {
        String activatedButton = Constants.getActivateBottomNavigationWidgetButton();
        if (activatedButton.equals("E-books")) {
            ebookButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_ebook, 0, 0);
            ebookButton.setTypeface(null, Typeface.BOLD);
            ebookButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));
        } else if (activatedButton.equals("Test Preparation")) {
            testPreparationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_test_preparation, 0, 0);
            testPreparationButton.setTypeface(null, Typeface.BOLD);
            testPreparationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));
        } else if (activatedButton.equals("Video")) {
            videoButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_video, 0, 0);
            videoButton.setTypeface(null, Typeface.BOLD);
            videoButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));
        } else if (activatedButton.equals("Store")) {
            storesButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_store, 0, 0);
            storesButton.setTypeface(null, Typeface.BOLD);
            storesButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));
        } else if (activatedButton.equals("Recommendation")) {
            recommendationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_recommendation, 0, 0);
            recommendationButton.setTypeface(null, Typeface.BOLD);
            recommendationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_enable_color));
        }
    }

    protected void disabledActivateBottomNavigationWidgetButton() {
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

    protected void disabledAllActivatedBottomNavigationWidgetButton() {
        //for E-books
        ebookButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_ebook_disabled, 0, 0);
        ebookButton.setTypeface(null, Typeface.NORMAL);
        ebookButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));

        //for Test Preparation
        testPreparationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_test_preparation_disabled, 0, 0);
        testPreparationButton.setTypeface(null, Typeface.NORMAL);
        testPreparationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));

        //for Video
        videoButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_video_disabled, 0, 0);
        videoButton.setTypeface(null, Typeface.NORMAL);
        videoButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));

        //for Store
        storesButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_store_disabled, 0, 0);
        storesButton.setTypeface(null, Typeface.NORMAL);
        storesButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));

        //for Recommendation
        recommendationButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_recommendation_disabled, 0, 0);
        recommendationButton.setTypeface(null, Typeface.NORMAL);
        recommendationButton.setTextColor(getResources().getColor(R.color.bottom_navigation_widget_button_disable_color));
    }

    private void hideBottomNavigationWidget() {
        bottomNavigationWidget.setAnimation(AnimationUtils.loadAnimation(LibraryActivity.this, R.anim.slide_down));

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
        bottomNavigationWidget.setAnimation(AnimationUtils.loadAnimation(LibraryActivity.this, R.anim.slide_up));
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

    @Override
    protected void syncDataFromAPI() {
        if (Utils.isNetworkConnected(this)) {
            Utils.triggerGAEvent(this, "REFRESH", customerId, "");
            new PrepareLibrary(LibraryActivity.this, true).execute();
        } else {
            Utils.networkNotAvailableAlertBox(this);
        }
    }
}