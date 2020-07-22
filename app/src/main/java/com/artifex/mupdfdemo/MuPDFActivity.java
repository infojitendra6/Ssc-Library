package com.artifex.mupdfdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;

import com.artifex.mupdfdemo.PopoverView.PopoverViewDelegate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.R;
import com.kopykitab.ereader.components.ClearableEditText;
import com.kopykitab.ereader.components.PagerSlidingTabStrip;
import com.kopykitab.ereader.components.handler.PermissionHandler;
import com.kopykitab.ereader.components.handler.PermissionHandler.PermissionListener;
import com.kopykitab.ereader.components.adapters.NewFeatureAdapter;
import com.kopykitab.ereader.settings.AppSettings;
import com.artifex.mupdfdemo.NotesOrBookmarksAdapter.DeleteAnnotationListener;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

class ThreadPerTaskExecutor implements Executor {
    public void execute(Runnable r) {
        new Thread(r).start();
    }
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MuPDFActivity extends AppCompatActivity implements FilePicker.FilePickerSupport, PopoverViewDelegate, DeleteAnnotationListener {

    /* The core rendering instance */
    enum TopBarMode {
        Main, Search, Annot, Delete, More, Accept
    }

    enum AcceptMode {
        Highlight, Underline, StrikeOut, Ink, CopyText
    }

    private String screenName = "Pdf_Open";

    private final int OUTLINE_REQUEST = 0;
    private final int PRINT_REQUEST = 1;
    private final int FILEPICK_REQUEST = 2;

    private MuPDFCore core;
    private String mFileName;
    private MuPDFReaderView mDocView;
    private View mButtonsView;
    private boolean mButtonsVisible;
    private EditText mPasswordView;
    private TextView mFilenameView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private TextView mInfoView;
    private ImageButton mSearchButton;
    private ImageButton mReflowButton;
    private ImageButton mOutlineButton;
    private ImageButton mMoreButton;
    private TextView mAnnotTypeText;
    private ImageButton mAnnotButton;
    private ViewAnimator mTopBarSwitcher;
    private ImageButton mLinkButton;
    private TopBarMode mTopBarMode = TopBarMode.Main;
    private AcceptMode mAcceptMode;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    private EditText mSearchText;
    private SearchTask mSearchTask;
    private AlertDialog.Builder mAlertBuilder;
    private boolean mLinkHighlight = false;
    private final Handler mHandler = new Handler();
    private boolean mAlertsActive = false;
    private boolean mReflow = false;
    private AsyncTask<Void, Void, MuPDFAlert> mAlertTask;
    private AlertDialog mAlertDialog;
    private FilePicker mFilePicker;
    private RelativeLayout mLowerButtons, mTopBarAnnotationButtons;
    private ImageButton mDayNightModeButton;
    private static ImageButton mBookmarkPageButton;

    private HashMap<String, String> book;
    private String customerId, productId, gaAnalyticsData;
    private RelativeLayout rootView;
    private PopoverView popoverView;
    private long pdfStartReadTime = 0, pageStartReadTime = 0;
    private int lastViewedPageNumber = -1;

    private static HashMap<Integer, HashMap<String, String>> bookmarkedData;
    private static HashMap<String, HashMap<Integer, HashMap<String, String>>> fileDataBookmark;
    private RelativeLayout mFlipperRelativeLayout, mBackDimFlipperRelativeLayout;
    private View notesAndBookmarksMenu;
    private boolean notesAndBookmarksMenuOut = false, enablePrintAnnotationMode;
    private PagerSlidingTabStrip notesAndBookmarksTabs;
    private NotesAndBookmarkPagerAdapter notesAndBookmarksAdapter;
    private List<String> notesAndBookmarksTabList = new ArrayList<String>();
    private ViewPager notesAndBookmarksViewPager;
    private boolean notesAndBookmarksHasShown = false;
    private static Integer currentPage;
    private DeleteAnnotationListener deleteAnnotationListener;
    private PopupWindow moreAnnotationsDropdown = null;
    private PermissionHandler mPermissionHandler;
    private Bundle tempSavedInstanceState;
    private String annotationText;
    private int annotatedTextLength = 0;


    public void createAlertWaiter() {
        mAlertsActive = true;
        // All mupdf library calls are performed on asynchronous tasks to avoid stalling
        // the UI. Some calls can lead to javascript-invoked requests to display an
        // alert dialog and collect a reply from the user. The task has to be blocked
        // until the user's reply is received. This method creates an asynchronous task,
        // the purpose of which is to wait of these requests and produce the dialog
        // in response, while leaving the core blocked. When the dialog receives the
        // user's response, it is sent to the core via replyToAlert, unblocking it.
        // Another alert-waiting task is then created to pick up the next alert.
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mAlertTask = new AsyncTask<Void, Void, MuPDFAlert>() {

            @Override
            protected MuPDFAlert doInBackground(Void... arg0) {
                if (!mAlertsActive)
                    return null;

                return core.waitForAlert();
            }

            @Override
            protected void onPostExecute(final MuPDFAlert result) {
                // core.waitForAlert may return null when shutting down
                if (result == null)
                    return;
                final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
                for (int i = 0; i < 3; i++)
                    pressed[i] = MuPDFAlert.ButtonPressed.None;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            int index = 0;
                            switch (which) {
                                case AlertDialog.BUTTON1:
                                    index = 0;
                                    break;
                                case AlertDialog.BUTTON2:
                                    index = 1;
                                    break;
                                case AlertDialog.BUTTON3:
                                    index = 2;
                                    break;
                            }
                            result.buttonPressed = pressed[index];
                            // Send the user's response to the core, so that it can
                            // continue processing.
                            core.replyToAlert(result);
                            // Create another alert-waiter to pick up the next alert.
                            createAlertWaiter();
                        }
                    }
                };
                mAlertDialog = mAlertBuilder.create();
                mAlertDialog.setTitle(result.title);
                mAlertDialog.setMessage(result.message);
                switch (result.iconType) {
                    case Error:
                        break;
                    case Warning:
                        break;
                    case Question:
                        break;
                    case Status:
                        break;
                }
                switch (result.buttonGroupType) {
                    case OkCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.cancel), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
                    case Ok:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.okay), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Ok;
                        break;
                    case YesNoCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON3, getString(R.string.cancel), listener);
                        pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
                    case YesNo:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.yes), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Yes;
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.no), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.No;
                        break;
                }
                mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            result.buttonPressed = MuPDFAlert.ButtonPressed.None;
                            core.replyToAlert(result);
                            createAlertWaiter();
                        }
                    }
                });

                mAlertDialog.show();
            }
        };

        mAlertTask.executeOnExecutor(new ThreadPerTaskExecutor());
    }

    public void destroyAlertWaiter() {
        mAlertsActive = false;
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
    }

    private MuPDFCore openFile(String path) {
        int lastSlashPos = path.lastIndexOf('/');
        mFileName = new String(lastSlashPos == -1
                ? path
                : path.substring(lastSlashPos + 1));
        System.out.println("Trying to open " + path);
        try {
            core = new MuPDFCore(this, path);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return core;
    }

    private MuPDFCore openBuffer(byte buffer[], String magic) {
        System.out.println("Trying to open byte buffer");
        try {
            core = new MuPDFCore(this, buffer, magic);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return core;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        customerId = AppSettings.getInstance(MuPDFActivity.this).get("CUSTOMER_ID");


        mAlertBuilder = new AlertDialog.Builder(this);

        book = (HashMap<String, String>) getIntent().getSerializableExtra("product_detail");

        bookmarkedData = new HashMap<Integer, HashMap<String, String>>();
        fileDataBookmark = new HashMap<String, HashMap<Integer, HashMap<String, String>>>();
        deleteAnnotationListener = this;

        try {
            if (book != null && book.containsKey("product_id")) {
                productId = book.get("product_id");
                gaAnalyticsData = productId;

                if (book.get("option_name").equals("Chapters")) {
                    String[] optionValue = book.get("option_value").split(" ");
                    if (optionValue != null) {
                        gaAnalyticsData += "_" + optionValue[1];
                    }
                }
            } else {
                productId = "-1";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tempSavedInstanceState = savedInstanceState;
        customerId = AppSettings.getInstance(MuPDFActivity.this).get("CUSTOMER_ID");
        mPermissionHandler = new PermissionHandler();
        mPermissionHandler.requestPermission(this, Constants.STORAGE_PERMISSION, 101, new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                prepareMuPDF(tempSavedInstanceState);

                if (Utils.isNetworkConnected(MuPDFActivity.this)) {
                    Utils.triggerGAEventOnline(MuPDFActivity.this, "Permission_Allow_" + screenName, "Logged_In", customerId);
                }
            }

            @Override
            public void onPermissionDenied() {
                Utils.showPermissionInfoDialog(MuPDFActivity.this);
                if (Utils.isNetworkConnected(MuPDFActivity.this)) {
                    Utils.triggerGAEventOnline(MuPDFActivity.this, "Permission_Denied_" + screenName, "Logged_In", customerId);
                }
            }

            @Override
            public void onPermissionPermanentlyDenied() {
                Utils.showPermissionInfoDialog(MuPDFActivity.this);
                if (Utils.isNetworkConnected(MuPDFActivity.this)) {
                    Utils.triggerGAEventOnline(MuPDFActivity.this, "Permission_Permanently_Denied_" + screenName, "Logged_In", customerId);
                }
            }
        });
    }

    private void prepareMuPDF(Bundle savedInstanceState) {
        /* load bookmarked page number from JSON file */
        File jsonFile = new File(Utils.getDirectory(getApplicationContext()) + Constants.BOOKMARKED_PAGE_JSON_FILENAME);
        try {
            if (jsonFile.exists()) {
                fileDataBookmark = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<Integer, HashMap<String, String>>>>() {
                }.getType());
                if (fileDataBookmark != null && fileDataBookmark.size() > 0) {
                    String mainKey = AppSettings.getInstance(getApplicationContext()).get("CUSTOMER_ID") + "_" + productId;
                    if (fileDataBookmark.get(mainKey) != null) {
                        bookmarkedData.putAll(fileDataBookmark.get(mainKey));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (core == null) {
            core = (MuPDFCore) getLastCustomNonConfigurationInstance();

            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
                mFileName = savedInstanceState.getString("FileName");
            }
        }
        if (core == null) {
            Intent intent = getIntent();
            byte buffer[] = null;
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                pdfStartReadTime = System.currentTimeMillis();
                System.out.println("URI to open is: " + uri);
                if (uri.toString().startsWith("content://")) {
                    String reason = null;
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        int len = is.available();
                        buffer = new byte[len];
                        is.read(buffer, 0, len);
                        is.close();
                    } catch (java.lang.OutOfMemoryError e) {
                        System.out.println("Out of memory during buffer reading");
                        reason = e.toString();
                    } catch (Exception e) {
                        System.out.println("Exception reading from stream: " + e);

                        // Handle view requests from the Transformer Prime's file manager
                        // Hopefully other file managers will use this same scheme, if not
                        // using explicit paths.
                        // I'm hoping that this case below is no longer needed...but it's
                        // hard to test as the file manager seems to have changed in 4.x.
                        try {
                            Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                            if (cursor.moveToFirst()) {
                                String str = cursor.getString(0);
                                if (str == null) {
                                    reason = "Couldn't parse data in intent";
                                } else {
                                    uri = Uri.parse(str);
                                }
                            }
                        } catch (Exception e2) {
                            System.out.println("Exception in Transformer Prime file manager code: " + e2);
                            reason = e2.toString();
                        }
                    }
                    if (reason != null) {
                        buffer = null;
                        Resources res = getResources();
                        AlertDialog alert = mAlertBuilder.create();
                        setTitle(String.format(res.getString(R.string.cannot_open_document_Reason), reason));
                        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                        alert.show();
                        return;
                    }
                }
                if (buffer != null) {
                    core = openBuffer(buffer, intent.getType());
                } else {
                    core = openFile(Uri.decode(uri.getEncodedPath()));
                }
                SearchTaskResult.set(null);
            }
            if (core != null && core.needsPassword()) {
                requestPassword(savedInstanceState);
                return;
            }
            if (core != null && core.countPages() == 0) {
                core = null;
            }
        }
        if (core == null) {
            //delete existing PDF for re-downloading
            String pdfUrl = "";
            if (book != null && book.containsKey("product_link")) {
                pdfUrl = book.get("product_link");

                try {
                    File pdfFile = new File(Utils.getFileDownloadPath(MuPDFActivity.this, pdfUrl));
                    if (pdfFile.exists()) {
                        pdfFile.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle(R.string.error_in_opening_document);
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            alert.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            alert.show();
            return;
        }
        core.getKey(productId); //called only to assign productId;
        createUI(savedInstanceState);
    }

    public void requestPassword(final Bundle savedInstanceState) {
        String pdfKey = core.getKey(productId);

        if (core.authenticatePassword(pdfKey)) {
            createUI(savedInstanceState);
        } else {
            finish();
        }

		/*mPasswordView = new EditText(this);
        mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(R.string.enter_password);
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (core.authenticatePassword(mPasswordView.getText().toString())) {
					createUI(savedInstanceState);
				} else {
					requestPassword(savedInstanceState);
				}
			}
		});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alert.show();*/
    }

    public void createUI(Bundle savedInstanceState) {
        if (core == null)
            return;

        //keep screen light on while book opened
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        customerId = AppSettings.getInstance(MuPDFActivity.this).get("CUSTOMER_ID");
        pageStartReadTime = 0;

        // Now create the UI.
        // First create the document view
        mDocView = new MuPDFReaderView(this) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null)
                    return;
                mPageNumberView.setText(String.format("%d / %d", i + 1,
                        core.countPages()));
                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);

                // call method to update bookmarkPageIcon
                updateBookmarkPageIcon();

                // Trigger GA Analytics PageWise
                if (!productId.equals("-1")) {
                    long currentTime = System.currentTimeMillis();
                    int differenceTimeInSeconds = (int) Math.ceil((double) (currentTime - pageStartReadTime) / 1000);
                    pageStartReadTime = currentTime;

                    if (lastViewedPageNumber != -1) {
                        Utils.triggerGAEventOnline(MuPDFActivity.this, "PDF_Read_App_Pagewise", gaAnalyticsData + "_" + (lastViewedPageNumber + 1), customerId, differenceTimeInSeconds);
                    }
                }

                lastViewedPageNumber = i;

                super.onMoveToChild(i);
            }

            @Override
            protected void onTapMainDocArea() {
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    if (mTopBarMode == TopBarMode.Main)
                        hideButtons();
                }
            }

            @Override
            protected void onDocMotion() {
                hideButtons();
            }

            @Override
            protected void onHit(Hit item) {
                switch (mTopBarMode) {
                    case Main:
                    case Annot:
                        if (item == Hit.Annotation) {
                            showButtons();
                            mTopBarMode = TopBarMode.Delete;
                            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                        }
                        break;
                    case Delete:
                        mTopBarMode = TopBarMode.Annot;
                        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                        // fall through
                    default:
                        // Not in annotation editing mode, but the pageview will
                        // still select and highlight hit annotations, so
                        // deselect just in case.
                        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                        if (pageView != null)
                            pageView.deselectAnnotation();
                        break;
                }
            }
        };
        mDocView.setAdapter(new MuPDFPageAdapter(this, this, core, productId));

        mSearchTask = new SearchTask(this, core) {
            @Override
            protected void onTextFound(SearchTaskResult result) {
                SearchTaskResult.set(result);
                // Ask the ReaderView to move to the resulting page
                mDocView.setDisplayedViewIndex(result.pageNumber);
                // Make the ReaderView act on the change to SearchTaskResult
                // via overridden onChildSetup method.
                mDocView.resetupChildren();
            }
        };

        // Make the buttons overlay, and store all its
        // controls in variables
        makeButtonsView();

        // Set up the page slider
        int smax = Math.max(core.countPages() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;

        // Set the file-name text
        mFilenameView.setText(mFileName);

        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.setDisplayedViewIndex((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
            }
        });

        // Activate the reflow button
        mReflowButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleReflow();
            }
        });

        mAnnotButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mTopBarMode = TopBarMode.Annot;
                mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
            }
        });

		/*if (core.fileFormat().startsWith("PDF") && core.isUnencryptedPDF() && !core.wasOpenedFromBuffer())
        {
			mAnnotButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mTopBarMode = TopBarMode.Annot;
					mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
				}
			});
		}
		else
		{
			mAnnotButton.setVisibility(View.GONE);
		}*/

        // Search invoking buttons are disabled while there is no text specified
        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        // React to interaction with the text widget
        mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }
        });

        //React to Done button on keyboard
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    search(1);
                return false;
            }
        });

        mSearchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                    search(1);
                return false;
            }
        });

        // Activate search invoking buttons
        mSearchBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(-1);
            }
        });
        mSearchFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(1);
            }
        });

        setLinkHighlight(!mLinkHighlight);
        /*mLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

			}
		});*/

        if (core.hasOutline()) {
            mOutlineButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    OutlineItem outline[] = core.getOutline();
                    if (outline != null) {
                        OutlineActivityData.get().items = outline;
                        OutlineActivityData.setBook(book);
                        OutlineActivityData.setNightMode(core.getNightMode());
                        Intent intent = new Intent(MuPDFActivity.this, com.artifex.mupdfdemo.OutlineActivity.class);
                        startActivityForResult(intent, OUTLINE_REQUEST);
                        Utils.triggerGAEvent(MuPDFActivity.this, "Pdf_Outline", productId, AppSettings.getInstance(MuPDFActivity.this).get("CUSTOMER_ID"));
                    }
                }
            });
        } else {
            mOutlineButton.setVisibility(View.GONE);
        }

        mDayNightModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                int barColor = getResources().getColor(R.color.day_mode_background_color);
                if (core.getNightMode()) {
                    core.setNightMode(false);
                    mDayNightModeButton.setImageResource(R.drawable.ic_night_mode);
                    getWindow().getDecorView().setBackgroundColor(0xFFFFFFFF);
                    barColor = getResources().getColor(R.color.day_mode_background_color);
                    mPageSlider.setProgressDrawable(getResources().getDrawable(R.drawable.seek_progress));
                    mPageNumberView.setTextColor(getResources().getColor(R.color.night_mode_background_color));
                    Utils.triggerGAEvent(MuPDFActivity.this, "Pdf_Day_Mode", productId, AppSettings.getInstance(MuPDFActivity.this).get("CUSTOMER_ID"));
                } else {
                    core.setNightMode(true);
                    mDayNightModeButton.setImageResource(R.drawable.ic_day_mode);
                    getWindow().getDecorView().setBackgroundColor(0xFF000000);
                    barColor = getResources().getColor(R.color.night_mode_background_color);
                    mPageSlider.setProgressDrawable(getResources().getDrawable(R.drawable.seek_progress_2));
                    mPageNumberView.setTextColor(Color.WHITE);
                    Utils.triggerGAEvent(MuPDFActivity.this, "Pdf_Night_Mode", productId, AppSettings.getInstance(MuPDFActivity.this).get("CUSTOMER_ID"));
                }
                mDocView.setAdapter(new MuPDFPageAdapter(MuPDFActivity.this, MuPDFActivity.this, core, productId));
                mDocView.refresh(mReflow);
                for (int i = 0; i < mTopBarSwitcher.getChildCount(); i++) {
                    mTopBarSwitcher.getChildAt(i).setBackgroundColor(barColor);
                }
                mLowerButtons.setBackgroundColor(barColor);
            }
        });

        mBookmarkPageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                int pageNumber = mDocView.getDisplayedViewIndex();
                CharSequence text = "";
                String customerId = AppSettings.getInstance(getApplicationContext()).get("CUSTOMER_ID");
                String label = productId + "_" + pageNumber;

                if (v.getId() == R.id.bookmarkPageButton) {
                    if (!bookmarkedData.containsKey(pageNumber)) {
                        HashMap<String, String> bookmarkData = new HashMap<String, String>();

                        //get today's date
                        String todaysDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime());
                        String notes = "";

                        bookmarkData.put("notes", notes);
                        bookmarkData.put("date_added", todaysDate);
                        bookmarkedData.put(pageNumber, bookmarkData);

                        mBookmarkPageButton.setImageResource(R.drawable.bookmark_page_after);
                        text = "Added as Bookmark";
                        Utils.triggerGAEvent(MuPDFActivity.this, "Pdf_Annotation_Bookmark", customerId, label);
                    } else {
                        bookmarkedData.remove(pageNumber);

                        mBookmarkPageButton.setImageResource(R.drawable.bookmark_page);
                        text = "Removed from Bookmark";
                        Utils.triggerGAEvent(MuPDFActivity.this, "Pdf_Annotation_Delete_Bookmark", customerId, label);
                    }

                    //show toast
                    Toast toast = Toast.makeText(MuPDFActivity.this, text, Toast.LENGTH_SHORT);
                    toast.show();

                    // update bookmarked page
                    String mainKey = AppSettings.getInstance(getApplicationContext()).get("CUSTOMER_ID") + "_" + productId;
                    fileDataBookmark.put(mainKey, bookmarkedData);

                    // save bookmarked page number to JSON file
                    saveBookmarkedPageNumbersToJSONFile();
                }
            }
        });

        // Reenstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));

        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
            showButtons();

        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
            searchModeOn();

        if (savedInstanceState != null && savedInstanceState.getBoolean("ReflowMode", false))
            reflowModeSet(true);

        // Stick the document view and the buttons overlay into a parent view
        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(mDocView);
        layout.addView(mButtonsView);
        setContentView(layout);

        rootView = layout;
    }

    private void updateBookmarkPageIcon() {
        // TODO Auto-generated method stub
        int pageNumber = mDocView.getDisplayedViewIndex();
        currentPage = pageNumber;
        if (bookmarkedData.containsKey(pageNumber)) {
            mBookmarkPageButton.setImageResource(R.drawable.bookmark_page_after);
        } else {
            mBookmarkPageButton.setImageResource(R.drawable.bookmark_page);
        }
    }

    public static void updateBookmarkPageIconWhenDelete(HashMap<Integer, HashMap<String, String>> bookmarkedDataNew, Integer pageNumber) {
        bookmarkedData = new HashMap<Integer, HashMap<String, String>>(bookmarkedDataNew);
        if ((currentPage.intValue()) == (pageNumber.intValue())) {
            mBookmarkPageButton.setImageResource(R.drawable.bookmark_page);
        }
    }

    // write Bookmarked page to JSON file
    private void saveBookmarkedPageNumbersToJSONFile() {
        try {
            FileWriter newJsonFile = new FileWriter(new File(Utils.getDirectory(getApplicationContext()) + Constants.BOOKMARKED_PAGE_JSON_FILENAME));
            newJsonFile.write(new Gson().toJson(fileDataBookmark));
            newJsonFile.flush();
            newJsonFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    mDocView.setDisplayedViewIndex(resultCode);
                break;
            case PRINT_REQUEST:
                if (resultCode == RESULT_CANCELED)
                    showInfo(getString(R.string.print_failed));
                break;
            case FILEPICK_REQUEST:
                if (mFilePicker != null && resultCode == RESULT_OK)
                    mFilePicker.onPick(data.getData());
        }

        if (notesAndBookmarksHasShown) {
            mBackDimFlipperRelativeLayout.setVisibility(View.GONE);
            notesAndBookmarksMenu.setVisibility(View.GONE);
            notesAndBookmarksMenuOut = false;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // TODO Auto-generated method stub
        MuPDFCore mycore = core;
        core = null;
        return mycore;
    }

    @Override
    public void deleteAnnotation(Integer pageNumber, String coordinate) {
        core.deleteAnnotation(pageNumber, coordinate);
        MuPDFView pageView = (MuPDFView) mDocView.getView(pageNumber);
        if (pageView != null) {
            pageView.cancelDraw();
            pageView.loadAnnotations();
            pageView.update();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mPermissionHandler.onPermissionsResult(requestCode, permissions, grantResults);
    }

    private void reflowModeSet(boolean reflow) {
        mReflow = reflow;
        mDocView.setAdapter(mReflow ? new MuPDFReflowAdapter(this, core, productId) : new MuPDFPageAdapter(this, this, core, productId));
        mReflowButton.setColorFilter(mReflow ? Color.argb(0xFF, 172, 114, 37) : Color.argb(0xFF, 255, 255, 255));
        setButtonEnabled(mAnnotButton, !reflow);
        setButtonEnabled(mSearchButton, !reflow);
        if (reflow)
            setLinkHighlight(false);
        setButtonEnabled(mLinkButton, !reflow);
        setButtonEnabled(mMoreButton, !reflow);
        mDocView.refresh(mReflow);
    }

    private void toggleReflow() {
        reflowModeSet(!mReflow);
        showInfo(mReflow ? getString(R.string.entering_reflow_mode) : getString(R.string.leaving_reflow_mode));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileName != null && mDocView != null) {
            outState.putString("FileName", mFileName);

            // Store current page in the prefs against the file name,
            // so that we can pick it up each time the file is loaded
            // Other info is needed only for screen-orientation change,
            // so it can go in the bundle
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.commit();
        }

        if (!mButtonsVisible)
            outState.putBoolean("ButtonsHidden", true);

        if (mTopBarMode == TopBarMode.Search)
            outState.putBoolean("SearchMode", true);

        if (mReflow)
            outState.putBoolean("ReflowMode", true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSearchTask != null)
            mSearchTask.stop();

        if (mFileName != null && mDocView != null) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.commit();
        }
    }

    public void onDestroy() {
        if (mDocView != null) {
            mDocView.applyToChildren(new ReaderView.ViewMapper() {
                void applyToView(View view) {
                    ((MuPDFView) view).releaseBitmaps();
                }
            });
        }
        if (core != null)
            core.onDestroy();
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        core = null;
        super.onDestroy();
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255) : Color.argb(255, 128, 128, 128));
    }

    private void setLinkHighlight(boolean highlight) {
        mLinkHighlight = highlight;
        // LINK_COLOR tint
        mLinkButton.setColorFilter(highlight ? Color.argb(0xFF, 172, 114, 37) : Color.argb(0xFF, 255, 255, 255));
        // Inform pages of the change.
        mDocView.setLinksEnabled(highlight);
    }

    private void showButtons() {
        if (core == null)
            return;
        if (!mButtonsVisible) {

            // hide bookmark page button relative layout
            mTopBarAnnotationButtons.setVisibility(View.GONE);

            mButtonsVisible = true;

            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
            mPageSlider.setProgress(index * mPageSliderRes);
            if (mTopBarMode == TopBarMode.Search) {
                mSearchText.requestFocus();
                showKeyboard();
            }

            Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, mLowerButtons.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mLowerButtons.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mLowerButtons.startAnimation(anim);

			/*anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
            anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageSlider.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mPageNumberView.setVisibility(View.VISIBLE);
				}
			});
			mPageSlider.startAnimation(anim);*/
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {

            // show bookmark page button relative layout
            mTopBarAnnotationButtons.setVisibility(View.VISIBLE);

            mButtonsVisible = false;
            hideKeyboard();

            Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.INVISIBLE);
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, 0, mLowerButtons.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mLowerButtons.setVisibility(View.INVISIBLE);
                }
            });
            mLowerButtons.startAnimation(anim);

			/*anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
            anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageNumberView.setVisibility(View.INVISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mPageSlider.setVisibility(View.INVISIBLE);
				}
			});
			mPageSlider.startAnimation(anim);*/
        }
    }

    private void searchModeOn() {
        if (mTopBarMode != TopBarMode.Search) {
            mTopBarMode = TopBarMode.Search;
            //Focus on EditTextWidget
            mSearchText.requestFocus();
            showKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        }
    }

    private void searchModeOff() {
        if (mTopBarMode == TopBarMode.Search) {
            mTopBarMode = TopBarMode.Main;
            hideKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
            SearchTaskResult.set(null);
            // Make the ReaderView act on the change to mSearchTaskResult
            // via overridden onChildSetup method.
            mDocView.resetupChildren();
        }
    }

    private void updatePageNumView(int index) {
        if (core == null)
            return;
        mPageNumberView.setText(String.format("%d / %d", index + 1, core.countPages()));
    }

    private void printDoc() {
        if (!core.fileFormat().startsWith("PDF")) {
            showInfo(getString(R.string.format_currently_not_supported));
            return;
        }

        Intent myIntent = getIntent();
        Uri docUri = myIntent != null ? myIntent.getData() : null;

        if (docUri == null) {
            showInfo(getString(R.string.print_failed));
        }

        if (docUri.getScheme() == null)
            docUri = Uri.parse("file://" + docUri.toString());

        Intent printIntent = new Intent(this, PrintDialogActivity.class);
        printIntent.setDataAndType(docUri, "aplication/pdf");
        printIntent.putExtra("title", mFileName);
        startActivityForResult(printIntent, PRINT_REQUEST);
    }

    private void showInfo(String message) {
        mInfoView.setText(message);

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            SafeAnimatorInflater safe = new SafeAnimatorInflater((Activity) this, R.animator.info, (View) mInfoView);
        } else {
            mInfoView.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mInfoView.setVisibility(View.INVISIBLE);
                }
            }, 500);
        }
    }

    private void makeButtonsView() {
        mButtonsView = getLayoutInflater().inflate(R.layout.buttons, null);
        mFilenameView = (TextView) mButtonsView.findViewById(R.id.docNameText);
        mPageSlider = (SeekBar) mButtonsView.findViewById(R.id.pageSlider);
        mPageNumberView = (TextView) mButtonsView.findViewById(R.id.pageNumber);
        mInfoView = (TextView) mButtonsView.findViewById(R.id.info);
        mSearchButton = (ImageButton) mButtonsView.findViewById(R.id.searchButton);
        mReflowButton = (ImageButton) mButtonsView.findViewById(R.id.reflowButton);
        mOutlineButton = (ImageButton) mButtonsView.findViewById(R.id.outlineButton);
        mAnnotButton = (ImageButton) mButtonsView.findViewById(R.id.editAnnotButton);
        mAnnotTypeText = (TextView) mButtonsView.findViewById(R.id.annotType);
        mTopBarSwitcher = (ViewAnimator) mButtonsView.findViewById(R.id.switcher);
        mSearchBack = (ImageButton) mButtonsView.findViewById(R.id.searchBack);
        mSearchFwd = (ImageButton) mButtonsView.findViewById(R.id.searchForward);
        mSearchText = (EditText) mButtonsView.findViewById(R.id.searchText);
        mLinkButton = (ImageButton) mButtonsView.findViewById(R.id.linkButton);
        mMoreButton = (ImageButton) mButtonsView.findViewById(R.id.moreButton);
        mDayNightModeButton = (ImageButton) mButtonsView.findViewById(R.id.dayNightModeButton);
        mBookmarkPageButton = (ImageButton) mButtonsView.findViewById(R.id.bookmarkPageButton);
        mTopBarAnnotationButtons = (RelativeLayout) mButtonsView.findViewById(R.id.topBarAnnotationButton);
        mLowerButtons = (RelativeLayout) mButtonsView.findViewById(R.id.lowerButtons);
        mTopBarSwitcher.setVisibility(View.INVISIBLE);
        mLowerButtons.setVisibility(View.INVISIBLE);

        mTopBarSwitcher.setInAnimation(AnimationUtils.loadAnimation(MuPDFActivity.this, R.anim.slide_in_up));
        mTopBarSwitcher.setOutAnimation(AnimationUtils.loadAnimation(MuPDFActivity.this, R.anim.slide_out_up));

        // show new features dialog
        String isNewFeatureShow = AppSettings.getInstance(MuPDFActivity.this).get("MUPDF_NEW_FEATURE");
        if (isNewFeatureShow.isEmpty() || isNewFeatureShow.equals("0")) {
            showNewFeatureDialog();
        }
    }

    public void OnMainBackButtonClick(View v) {
        onBackPressed();
    }

    public void OnMoreButtonClick(View v) {
        mTopBarMode = TopBarMode.More;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelMoreButtonClick(View v) {
        mTopBarMode = TopBarMode.Main;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnPrintButtonClick(View v) {
        printDoc();
    }

    public void OnCopyTextButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.CopyText;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(getString(R.string.copy_text));
        showInfo(getString(R.string.select_text));
    }

    public void OnPrintAnnotationButtonClick(View view) {
        enablePrintAnnotationMode = true;
        openListOfAnnotationView();
    }

    public void OnMoreAnnotButtonClick(View view) {
        LayoutInflater mInflater;
        View moreAnnotationsView;
        LinearLayout bookmark, notesAndBookmarks;
        final TextView bookmarksText;
        final int pageNumber;
        final String TAG = MuPDFActivity.class.getName();

        try {
            mInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            moreAnnotationsView = mInflater.inflate(R.layout.more_annotation_dialog, null);

            bookmark = (LinearLayout) moreAnnotationsView.findViewById(R.id.item_bookmarks);
            notesAndBookmarks = (LinearLayout) moreAnnotationsView.findViewById(R.id.item_notes_bookmarks);

            bookmarksText = (TextView) bookmark.findViewById(R.id.item_bookmarks_text);

            pageNumber = mDocView.getDisplayedViewIndex();
            currentPage = pageNumber;

            if (bookmarkedData.containsKey(pageNumber)) {
                bookmarksText.setText(getString(R.string.remove_bookmark));
            }

            bookmark.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, bookmarksText.getText().toString());
                    moreAnnotationsDropdown.dismiss();
                    CharSequence text = "";
                    String customerId = AppSettings.getInstance(getApplicationContext()).get("CUSTOMER_ID");
                    String label = productId + "_" + pageNumber;

                    if (!bookmarkedData.containsKey(pageNumber)) {
                        HashMap<String, String> bookmarkData = new HashMap<String, String>();

                        //get today's date
                        String todaysDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime());
                        String notes = "";

                        bookmarkData.put("notes", notes);
                        bookmarkData.put("date_added", todaysDate);
                        bookmarkedData.put(pageNumber, bookmarkData);

                        mBookmarkPageButton.setImageResource(R.drawable.bookmark_page_after);
                        text = "Added as Bookmark";
                        Utils.triggerGAEvent(MuPDFActivity.this, "Pdf_Annotation_Bookmark", customerId, label);
                    } else {
                        bookmarkedData.remove(pageNumber);

                        mBookmarkPageButton.setImageResource(R.drawable.bookmark_page);
                        text = "Removed from Bookmark";
                        Utils.triggerGAEvent(MuPDFActivity.this, "Pdf_Annotation_Delete_Bookmark", customerId, label);
                    }

                    //show toast
                    Toast toast = Toast.makeText(MuPDFActivity.this, text, Toast.LENGTH_SHORT);
                    toast.show();

                    // update bookmarked page
                    String mainKey = customerId + "_" + productId;
                    fileDataBookmark.put(mainKey, bookmarkedData);

                    // save bookmarked page number to JSON file
                    saveBookmarkedPageNumbersToJSONFile();
                }
            });

            notesAndBookmarks.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "List of Notes & Bookmarks");
                    moreAnnotationsDropdown.dismiss();

                    enablePrintAnnotationMode = false;
                    openListOfAnnotationView();
                }
            });


            moreAnnotationsView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            moreAnnotationsDropdown = new PopupWindow(moreAnnotationsView, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, true);
            moreAnnotationsDropdown.setBackgroundDrawable(new BitmapDrawable());
            moreAnnotationsDropdown.showAsDropDown(view, 5, (int) getResources().getDimension(R.dimen.popupmenu_bottom_margin));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openListOfAnnotationView() {
        mFlipperRelativeLayout = (RelativeLayout) MuPDFActivity.this.findViewById(R.id.notesBookmarksFlipperMenu);
        notesAndBookmarksMenu = mFlipperRelativeLayout.findViewById(R.id.notesBookmarksMenu);
        mFlipperRelativeLayout.bringToFront();

        if (!notesAndBookmarksMenuOut) {

            //relative layout for dimming the background like drawer layout
            mBackDimFlipperRelativeLayout = (RelativeLayout) MuPDFActivity.this.findViewById(R.id.bac_dim_notesBookmarksFlipperMenu);
            mBackDimFlipperRelativeLayout.setVisibility(View.VISIBLE);

            mBackDimFlipperRelativeLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    // hide notes and bookmark Menu when click to outer region of menu
                    if (notesAndBookmarksMenuOut) {
                        notesAndBookmarksMenu.setVisibility(View.GONE);
                        notesAndBookmarksMenuOut = false;

                        //Invisible Dim Background
                        mBackDimFlipperRelativeLayout.setVisibility(View.GONE);
                    }
                }
            });

            notesAndBookmarksMenu.setAnimation(AnimationUtils.loadAnimation(MuPDFActivity.this, R.anim.push_left_in));

            Animation anim = new TranslateAnimation(0, 0, 0, 0);
            anim.setDuration(500);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    notesAndBookmarksMenu.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            notesAndBookmarksMenu.startAnimation(anim);

            notesAndBookmarksMenuOut = !notesAndBookmarksMenuOut;
        }

        if (!notesAndBookmarksHasShown) {
            listOfNotesAndBookmarksMenu();
        } else {
            notesAndBookmarksViewPager.setAdapter(null);
            notesAndBookmarksTabs.updateTabStyles();
            notesAndBookmarksViewPager.setAdapter(notesAndBookmarksAdapter);
        }
    }

    private void listOfNotesAndBookmarksMenu() {
        notesAndBookmarksHasShown = true;

        //get Chapter information
        LinkedHashMap<Integer, String> mChapterInfo = new LinkedHashMap<Integer, String>();
        if (core.hasOutline()) {
            OutlineItem outline[] = core.getOutline();
            if (outline != null) {
                for (OutlineItem item : outline) {
                    mChapterInfo.put(item.page, item.title);
                }
            }
        }

        notesAndBookmarksTabs = (PagerSlidingTabStrip) findViewById(R.id.notes_and_bookmarks_tabs);
        notesAndBookmarksViewPager = (ViewPager) findViewById(R.id.notes_and_bookmarks_viewpager);

        String[] tabNames = getResources().getStringArray(R.array.notes_and_bookmarks_tabs_items);
        for (int i = 0; i < tabNames.length; i++) {
            notesAndBookmarksTabList.add(tabNames[i]);
        }

        notesAndBookmarksTabs.setTabNames(notesAndBookmarksTabList);
        int offScreenPageLimit = notesAndBookmarksTabList.size();
        notesAndBookmarksViewPager.setOffscreenPageLimit(offScreenPageLimit);
        notesAndBookmarksAdapter = new NotesAndBookmarkPagerAdapter(getSupportFragmentManager(), this, notesAndBookmarksTabList, mChapterInfo);
        notesAndBookmarksViewPager.setAdapter(notesAndBookmarksAdapter);

        int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        notesAndBookmarksViewPager.setPageMargin(pageMargin);
        notesAndBookmarksViewPager.setCurrentItem(0, true);
        notesAndBookmarksTabs.setViewPager(notesAndBookmarksViewPager);
    }

    public void OnEditAnnotButtonClick(View v) {
        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelAnnotButtonClick(View v) {
        mTopBarMode = TopBarMode.Main;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnHighlightButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Highlight;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.highlight);
        showInfo(getString(R.string.select_text));
    }

    public void OnUnderlineButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Underline;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.underline);
        showInfo(getString(R.string.select_text));
    }

    public void OnStrikeOutButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.StrikeOut;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.strike_out);
        showInfo(getString(R.string.select_text));
    }

    public void OnInkButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Ink;
        mDocView.setMode(MuPDFReaderView.Mode.Drawing);
        mAnnotTypeText.setText(R.string.ink);
        showInfo(getString(R.string.draw_annotation));
    }

    public void OnCancelAcceptButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null) {
            pageView.deselectText();
            pageView.cancelDraw();
        }
        mDocView.setMode(MuPDFReaderView.Mode.Viewing);
        switch (mAcceptMode) {
            case CopyText:
                mTopBarMode = TopBarMode.More;
                break;
            default:
                mTopBarMode = TopBarMode.Annot;
                break;
        }
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public HashMap<String, String> OnAcceptButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        StringBuilder copyTextBuilder = null;
        boolean success = false;
        Context context = getApplicationContext();
        CharSequence text = "Hello You have exceeded maximum annotation level it should be less than 1024 character";
        int duration = Toast.LENGTH_SHORT;

        switch (mAcceptMode) {
            case CopyText:
                if (pageView != null)
                    success = pageView.copySelection();
                mTopBarMode = TopBarMode.More;
                showInfo(success ? getString(R.string.copied_to_clipboard) : getString(R.string.no_text_selected));
                break;

            case Highlight:
                if (pageView != null) {
                    copyTextBuilder = pageView.copySelectionText();
                    if (copyTextBuilder != null) {
                        annotatedTextLength = copyTextBuilder.length();
                        if (annotatedTextLength > 1024) {
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                            pageView.deselectText();
                        } else {
                            success = pageView.markupSelection(Annotation.Type.HIGHLIGHT);
                        }
                    }

                }
                mTopBarMode = TopBarMode.Annot;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case Underline:
                if (pageView != null) {
                    copyTextBuilder = pageView.copySelectionText();
                    if (copyTextBuilder != null) {
                        annotatedTextLength = copyTextBuilder.length();
                        if (annotatedTextLength > 1024) {
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                            pageView.deselectText();
                        } else {
                            success = pageView.markupSelection(Annotation.Type.UNDERLINE);
                        }
                    }
                }
                mTopBarMode = TopBarMode.Annot;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case StrikeOut:
                if (pageView != null) {
                    copyTextBuilder = pageView.copySelectionText();
                    if (copyTextBuilder != null) {
                        annotatedTextLength = copyTextBuilder.length();
                        if (annotatedTextLength > 1024) {
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                            pageView.deselectText();
                        } else {
                            success = pageView.markupSelection(Annotation.Type.STRIKEOUT);
                        }
                    }
                }
                mTopBarMode = TopBarMode.Annot;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case Ink:
                if (pageView != null)
                    success = pageView.saveDraw();
                mTopBarMode = TopBarMode.Annot;
                if (!success)
                    showInfo(getString(R.string.nothing_to_save));
                break;
        }

        //back to main
        mTopBarMode = TopBarMode.Main;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mDocView.setMode(MuPDFReaderView.Mode.Viewing);

        if (copyTextBuilder != null && annotatedTextLength <= 1024) {
            String copyText = copyTextBuilder.toString();
            if (!copyText.isEmpty()) {
                //get Last added coordinates
                List<Float> coordinates = core.getLastAddedAnnotation();

                //save annotation to JSON file
                String coordinatesStringWithBrackets = coordinates.toString();
                String coordinatesString = coordinatesStringWithBrackets.substring(1, coordinatesStringWithBrackets.length() - 1);

                HashMap<String, String> notesData = new HashMap<>();
                notesData.put("notes", "");
                notesData.put("annotation_text", copyText);
                notesData.put("date_added", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime()));

                //save notes, annotation to JSON file
                UtilsPDF.saveNotes(MuPDFActivity.this, notesData, customerId, productId, currentPage, coordinatesString);

                return notesData;
            }
        }

        return null;
    }

    public void OnSaveAndAcceptButtonClick(View v) {
        final AlertDialog alertDialog = UtilsPDF.createAlertBox(MuPDFActivity.this);
        alertDialog.show();

        ImageButton cancelNotes = ((ImageButton) alertDialog.findViewById(R.id.cancelNotesButton));
        Button saveNotesButton = ((Button) alertDialog.findViewById(R.id.saveNotesButton));
        final EditText editText = ((EditText) alertDialog.findViewById(R.id.dialogEditText));

        cancelNotes.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                //perform to cancel note and back like cancel accept button
                OnCancelAcceptButtonClick(v);

                alertDialog.dismiss();
            }
        });

        saveNotesButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String notesMsg = editText.getText().toString().trim();
                CharSequence text = "";
                if (!notesMsg.isEmpty()) {
                    //perform to save annotations like accept button
                    HashMap<String, String> notesData = OnAcceptButtonClick(v);

                    //get Last added coordinates
                    List<Float> coordinates = core.getLastAddedAnnotation();

                    if (coordinates != null && notesData != null && notesData.size() > 0) {
                        String customerId = AppSettings.getInstance(getApplicationContext()).get("CUSTOMER_ID");
                        notesData.put("notes", notesMsg);

                        //save notes to JSON file
                        String coordinatesStringWithBrackets = coordinates.toString();
                        String coordinatesString = coordinatesStringWithBrackets.substring(1, coordinatesStringWithBrackets.length() - 1);

                        UtilsPDF.saveNotes(MuPDFActivity.this, notesData, customerId, productId, mDocView.getDisplayedViewIndex(), coordinatesString);

                        text = "Notes are Added Here...";
                    } else {
                        text = "Error Occurred while saving Notes...";
                    }

                    //disable alert dialog
                    alertDialog.dismiss();
                } else {
                    text = "Please write a Note...";
                }

                //show toast
                Toast toast = Toast.makeText(MuPDFActivity.this, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public void OnCancelSearchButtonClick(View v) {
        searchModeOff();
    }

    public void OnDeleteButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.deleteSelectedAnnotation();
        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelDeleteButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.deselectAnnotation();
        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnAddNotesButtonClick(View v) {
        // initialize annotation text
        annotationText = "";

        final AlertDialog alertDialog = UtilsPDF.createAlertBox(MuPDFActivity.this);
        alertDialog.show();

        ImageButton cancelNotes = ((ImageButton) alertDialog.findViewById(R.id.cancelNotesButton));
        Button saveNotesButton = ((Button) alertDialog.findViewById(R.id.saveNotesButton));
        final EditText editText = ((EditText) alertDialog.findViewById(R.id.dialogEditText));

        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        int annotation_index = -1;
        if (pageView != null)
            annotation_index = pageView.getSelectedAnnotationIndex();

        final String customerId = AppSettings.getInstance(getApplicationContext()).get("CUSTOMER_ID");
        final Integer pageNumber = mDocView.getDisplayedViewIndex();
        String mainKey = customerId + "_" + productId + "___" + pageNumber;

        // get coordinates for selected annotation
        String coordinatesWithBrackets = core.getAllCoordinatesForNotesAnnotation(mainKey, annotation_index).toString();
        final String coordinates = coordinatesWithBrackets.substring(1, coordinatesWithBrackets.length() - 1);

        // Get Notes from JSON File & Set to EditText
        HashMap<String, HashMap<String, String>> allNotesAndCoordinates = UtilsPDF.getAllNotesAndCoordinatesByKey(MuPDFActivity.this, customerId, productId, pageNumber);

        if (allNotesAndCoordinates != null && allNotesAndCoordinates.size() > 0) {
            HashMap<String, String> notesData = allNotesAndCoordinates.get(coordinates);

            if (notesData != null && notesData.size() > 0) {
                editText.setText(notesData.get("notes"));
                editText.setSelection(notesData.get("notes").length());
                annotationText = notesData.get("annotation_text");
            }
        }

        cancelNotes.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                alertDialog.dismiss();
            }
        });

        saveNotesButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String notesMsg = editText.getText().toString().trim();
                CharSequence text = "";
                if (!notesMsg.isEmpty()) {
                    HashMap<String, String> notesData = new HashMap<String, String>();
                    notesData.put("notes", notesMsg);
                    notesData.put("annotation_text", annotationText);
                    notesData.put("date_added", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime()));

                    //save notes to JSON file
                    UtilsPDF.saveNotes(MuPDFActivity.this, notesData, customerId, productId, pageNumber, coordinates);

                    text = "Notes are Added Here...";

                    //disable alert dialog
                    alertDialog.dismiss();
                } else {
                    text = "Note cant't be Empty...";
                }

                //show toast
                Toast toast = Toast.makeText(MuPDFActivity.this, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(mSearchText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    private void search(int direction) {
        hideKeyboard();
        int displayPage = mDocView.getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        ImageButton noteAndAcceptButton = (ImageButton) mButtonsView.findViewById(R.id.saveAndAcceptButton);
        if (mDocView.getMode() == MuPDFReaderView.Mode.Selecting) {
            List<Float> coord = null;
            MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
            switch (mAcceptMode) {
                case Highlight:
                    if (pageView != null)
                        coord = pageView.copySelectionCoordinates(Annotation.Type.HIGHLIGHT);
                    break;

                case Underline:
                    if (pageView != null)
                        coord = pageView.copySelectionCoordinates(Annotation.Type.UNDERLINE);
                    break;

                case StrikeOut:
                    if (pageView != null)
                        coord = pageView.copySelectionCoordinates(Annotation.Type.STRIKEOUT);
                    break;
                default:
                    break;
            }

            if (coord != null && coord.size() > 0) {
                noteAndAcceptButton.setVisibility(View.VISIBLE);
            } else {
                noteAndAcceptButton.setVisibility(View.GONE);
            }
        } else {
            noteAndAcceptButton.setVisibility(View.GONE);
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onSearchRequested() {
        if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOn();
        }
        return super.onSearchRequested();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOff();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        if (core != null) {
            core.startAlerts();
            createAlertWaiter();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (core != null) {
            destroyAlertWaiter();
            core.stopAlerts();
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (core != null && core.hasChanges()) {
            core.save();
            /*DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
					if (which == AlertDialog.BUTTON_POSITIVE)	{
						core.save();
					}
				}
			};
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle("KopyKitab eReader");
			alert.setMessage(getString(R.string.document_has_changes_save_them_));
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
			alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
			alert.show();*/
        } else {
            super.onBackPressed();
        }
        finish();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Utils.triggerScreen(MuPDFActivity.this, screenName);

        // Prepare MuPDF when user gave permissions from settings and came back to activity
        if (Utils.isPermissionDialogOpen()) {
            if (Utils.hasPermissions(MuPDFActivity.this, Constants.STORAGE_PERMISSION)) {
                if (isFinishing()) {
                    Utils.closePermissionDialog();
                }
                prepareMuPDF(tempSavedInstanceState);
            }
        }
    }

    @Override
    public void finish() {
        // TODO Auto-generated method stub
        if (pdfStartReadTime > 0) {
            long currentTime = System.currentTimeMillis();
            int differenceTimeInMinutes = (int) Math.ceil((double) (currentTime - pdfStartReadTime) / 60000);    //1000*60
            Log.i("Pdf_Close", productId + " (Read Time : " + differenceTimeInMinutes + " Minutes)");
            Utils.triggerGAEvent(this, "Pdf_Close", productId, AppSettings.getInstance(this).get("CUSTOMER_ID"), differenceTimeInMinutes);
        }
        pdfStartReadTime = 0;

        super.finish();
    }

    @Override
    public void performPickFor(FilePicker picker) {
        mFilePicker = picker;
        Intent intent = new Intent(this, ChoosePDFActivity.class);
        intent.setAction(ChoosePDFActivity.PICK_KEY_FILE);
        startActivityForResult(intent, FILEPICK_REQUEST);
    }

    @Override
    public void popoverViewWillShow(com.artifex.mupdfdemo.PopoverView view) {
        // TODO Auto-generated method stub

    }

    @Override
    public void popoverViewDidShow(com.artifex.mupdfdemo.PopoverView view) {
        // TODO Auto-generated method stub

    }

    @Override
    public void popoverViewWillDismiss(com.artifex.mupdfdemo.PopoverView view) {
        // TODO Auto-generated method stub

    }

    @Override
    public void popoverViewDidDismiss(com.artifex.mupdfdemo.PopoverView view) {
        // TODO Auto-generated method stub

    }

    public void OnSearchButtonClick(View v) {
        //searchModeOn();

        if (popoverView == null) {
            popoverView = new PopoverView(this, R.layout.search_popover);
            popoverView.setContentSizeForViewInPopover(new Point(640, 800));
            popoverView.setDelegate(this);
            popoverView.showPopoverFromRectInViewGroup(rootView, PopoverView.getFrameForView(v), PopoverView.PopoverArrowDirectionAny, true);
        } else {
            popoverView.showPopover();
        }

        final ClearableEditText searchText = (ClearableEditText) popoverView.findViewById(R.id.search_text);
        searchText.requestFocus();
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT);

        final ViewFlipper searchPopoverFlip = (ViewFlipper) popoverView.findViewById(R.id.search_popover_flip);
        final ListView listView = (ListView) popoverView.findViewById(R.id.search_results);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
                    searchPopoverFlip.setDisplayedChild(0);
                    SearchResultsAdapter searchResultsAdapter = new SearchResultsAdapter(MuPDFActivity.this, popoverView, new SparseArray<RectF[]>(), core);
                    listView.setAdapter(searchResultsAdapter);
                    listView.setOnScrollListener(searchResultsAdapter);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                            // TODO Auto-generated method stub
                            SearchResultsAdapter clickedItem = (SearchResultsAdapter) parent.getAdapter();
                            mSearchTask.go(searchText.getText().toString(), 0, mDocView.getDisplayedViewIndex(), Long.valueOf(clickedItem.getItemId(position)).intValue());
                            popoverView.dissmissPopover(true);
                        }
                    });
                    return true;
                }
                return false;
            }
        });

        searchText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
                if (s == null || s.length() <= 0) {
                    SearchResultsAdapter searchResultsAdapter = (SearchResultsAdapter) listView.getAdapter();
                    if (searchResultsAdapter != null) {
                        searchResultsAdapter.clear();
                    }

                    searchPopoverFlip.setDisplayedChild(0);
                    LinearLayout searchProgressLayout = (LinearLayout) searchPopoverFlip.findViewById(R.id.search_progress_layout);
                    if (searchProgressLayout.getVisibility() == View.VISIBLE) {
                        searchProgressLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
    }

    public class NotesAndBookmarkPagerAdapter extends FragmentStatePagerAdapter {

        private Context mContext;
        private List<String> tabItems;
        private LinkedHashMap<Integer, String> mChapterInfo;
        private FragmentManager fragmentManager;
        private Fragment[] fragments;

        public NotesAndBookmarkPagerAdapter(FragmentManager fm, Context mContext, List<String> notesAndBookmarksTabList, LinkedHashMap<Integer, String> mChapterInfo) {
            super(fm);
            this.mContext = mContext;
            this.tabItems = notesAndBookmarksTabList;
            this.mChapterInfo = mChapterInfo;

            fragmentManager = fm;
            fragments = new Fragment[tabItems.size()];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            assert (0 <= position && position < fragments.length);
            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.remove(fragments[position]);
            trans.commit();
            fragments[position] = null;
        }

        @Override
        public Fragment instantiateItem(ViewGroup container, int position) {
            Fragment fragment = getItem(position);
            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.add(container.getId(), fragment, "fragment:" + position);
            trans.commit();
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
        public Fragment getItem(int position) {
            assert (0 <= position && position < fragments.length);
            if (fragments[position] == null) {
                fragments[position] = new DetailedNotesOrBookmarksFragment(mContext, position, mChapterInfo, book, deleteAnnotationListener, enablePrintAnnotationMode);
            }

            return fragments[position];
        }
    }


    public final static class UtilsPDF {
        public static AlertDialog createAlertBox(Context context) {
            ContextThemeWrapper ctw = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
            AlertDialog.Builder alertBox = new AlertDialog.Builder(ctw);
            alertBox.setInverseBackgroundForced(true);
            AlertDialog alertDialog = alertBox.create();
            alertDialog.setView(LayoutInflater.from(context).inflate(R.layout.add_notes_annots_dialog, null), 0, 0, 0, 0);

            return alertDialog;
        }

        public static HashMap<String, HashMap<String, String>> getAllNotesAndCoordinatesByKey(Context context, String customerId, String productId, Integer pageNumber) {
            String mainKey = customerId + "_" + productId + "___" + pageNumber;
            HashMap<String, HashMap<String, HashMap<String, String>>> fileDataNotes = new HashMap<String, HashMap<String, HashMap<String, String>>>();
            HashMap<String, HashMap<String, String>> allNotesAndCoordinates = new HashMap<String, HashMap<String, String>>();
            try {
                File jsonFile = new File(Utils.getDirectory(context) + Constants.ANNOT_ADD_NOTES_JSON_FILENAME);
                if (jsonFile.exists()) {
                    fileDataNotes = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<String, HashMap<String, String>>>>() {
                    }.getType());
                    if (fileDataNotes != null && fileDataNotes.size() > 0) {
                        allNotesAndCoordinates = fileDataNotes.get(mainKey);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return allNotesAndCoordinates;
        }

        public static void saveNotes(Context context, HashMap<String, String> notesData, String customerId, String productId, Integer pageNumber, String coordinate) {
            String mainKey = customerId + "_" + productId + "___" + pageNumber;
            HashMap<String, HashMap<String, HashMap<String, String>>> fileDataNotes = new HashMap<String, HashMap<String, HashMap<String, String>>>();
            HashMap<String, HashMap<String, String>> allNotesAndCoordinates = new HashMap<String, HashMap<String, String>>();

            try {
                File jsonFile = new File(Utils.getDirectory(context) + Constants.ANNOT_ADD_NOTES_JSON_FILENAME);
                if (jsonFile.exists()) {
                    fileDataNotes = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<String, HashMap<String, String>>>>() {
                    }.getType());
                    if (fileDataNotes != null && fileDataNotes.size() > 0) {
                        if (fileDataNotes.get(mainKey) != null && fileDataNotes.get(mainKey).size() > 0) {
                            allNotesAndCoordinates.putAll(fileDataNotes.get(mainKey));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Either append note or Add note
            allNotesAndCoordinates.put(coordinate, notesData);
            fileDataNotes.put(mainKey, allNotesAndCoordinates);

            if (coordinate.startsWith("8.0")) {
                Utils.triggerGAEvent(context, "Pdf_Annotation_Highlight_Notes", customerId, productId + "_" + pageNumber);
            } else if (coordinate.startsWith("9.0")) {
                Utils.triggerGAEvent(context, "Pdf_Annotation_Underline_Notes", customerId, productId + "_" + pageNumber);
            } else if (coordinate.startsWith("11.0")) {
                Utils.triggerGAEvent(context, "Pdf_Annotation_Strikeout_Notes", customerId, productId + "_" + pageNumber);
            }

            try {
                // write data to JSON file
                FileWriter newJsonFile = new FileWriter(new File(Utils.getDirectory(context) + Constants.ANNOT_ADD_NOTES_JSON_FILENAME));
                newJsonFile.write(new Gson().toJson(fileDataNotes));
                newJsonFile.flush();
                newJsonFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /* load notes from JSON file and update the JSON File */
        public static void deleteNotesDataFromJSONFile(Context mContext, String customerId, String productId, Integer pageNumber, String coordinate) {
            String mainKey = customerId + "_" + productId + "___" + pageNumber;
            HashMap<String, HashMap<String, HashMap<String, String>>> fileDataNotes = new HashMap<String, HashMap<String, HashMap<String, String>>>();
            HashMap<String, HashMap<String, String>> allNotesAndCoordinates = new HashMap<String, HashMap<String, String>>();

            try {
                File jsonFile = new File(Utils.getDirectory(mContext) + Constants.ANNOT_ADD_NOTES_JSON_FILENAME);
                if (jsonFile.exists()) {
                    fileDataNotes = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<String, HashMap<String, String>>>>() {
                    }.getType());
                    if (fileDataNotes != null && fileDataNotes.size() > 0) {
                        allNotesAndCoordinates = fileDataNotes.get(mainKey);
                        if (allNotesAndCoordinates != null && allNotesAndCoordinates.size() > 0) {
                            allNotesAndCoordinates.remove(coordinate);

                            fileDataNotes.put(mainKey, allNotesAndCoordinates);

                            if (coordinate.startsWith("8.0")) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Highlight_Notes", customerId, productId + "_" + pageNumber);
                            } else if (coordinate.startsWith("9.0")) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Underline_Notes", customerId, productId + "_" + pageNumber);
                            } else if (coordinate.startsWith("11.0")) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Strikeout_Notes", customerId, productId + "_" + pageNumber);
                            }

                            // write data to JSON file
                            FileWriter newJsonFile = new FileWriter(new File(Utils.getDirectory(mContext) + Constants.ANNOT_ADD_NOTES_JSON_FILENAME));
                            newJsonFile.write(new Gson().toJson(fileDataNotes));
                            newJsonFile.flush();
                            newJsonFile.close();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void showNewFeatureDialog() {
        final AlertDialog newFeatureDialog = Utils.createAlertBox(MuPDFActivity.this, R.layout.new_feature_view);
        newFeatureDialog.setCancelable(false);
        newFeatureDialog.show();

        TextView title = (TextView) newFeatureDialog.findViewById(R.id.new_feature_title);
        ListView newFeatureView = (ListView) newFeatureDialog.findViewById(R.id.new_feature_list_view);
        Button settingButton = (Button) newFeatureDialog.findViewById(R.id.new_feature_ok_button);

        title.setText("How to make your reading more easy");
        title.setMaxLines(3);

        newFeatureView.setAdapter(new NewFeatureAdapter(MuPDFActivity.this, getResources().getStringArray(R.array.new_feature_header_text_array), getResources().getStringArray(R.array.new_feature_text_array), Constants.NEW_FEATURE_IMAGES));

        settingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                newFeatureDialog.dismiss();
            }
        });

        AppSettings.getInstance(MuPDFActivity.this).set("MUPDF_NEW_FEATURE", "1");
    }
}