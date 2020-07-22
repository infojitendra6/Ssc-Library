package com.kopykitab.ereader;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.kopykitab.ereader.components.CircularProgressView;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Utils;

import java.util.HashMap;
import java.util.Map;

public class VideoViewActivity extends AppCompatActivity implements Player.EventListener {

    private SimpleExoPlayerView mExoPlayerView;
    private MediaSource mVideoSource;
    private boolean mExoPlayerFullscreen = false, isVideoError = false;
    private LinearLayout mVideoDetails;
    private ImageView mFullScreenIcon;
    private CircularProgressView videoLoadingProgress;
    private TextView videoError;

    private int mResumeWindow;
    private long mResumePosition;

    private HashMap<String, String> productData;
    private String videoUrl, productId, customerId, viewType = "Online";
    private long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_view);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.back_button);
        actionBar.setDisplayHomeAsUpEnabled(true);
        SpannableString actionBarLabel = new SpannableString("Video View");
        actionBarLabel.setSpan(new TypefaceSpan("" + Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf")), 0, actionBarLabel.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        actionBar.setTitle(actionBarLabel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        productData = (HashMap<String, String>) getIntent().getSerializableExtra("product_detail");
        videoUrl = productData.get("product_link");
        productId = productData.get("product_id");
        customerId = AppSettings.getInstance(VideoViewActivity.this).get("CUSTOMER_ID");
        startTime = System.currentTimeMillis();

        mExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.exoplayer);
        videoLoadingProgress = (CircularProgressView) findViewById(R.id.video_loading_progress);
        videoError = (TextView) findViewById(R.id.video_error);
        mVideoDetails = (LinearLayout) findViewById(R.id.video_details);
        TextView videoTitle = (TextView) findViewById(R.id.video_title);
        videoTitle.setText(productData.get("name"));

        TextView videoDescripton = (TextView) findViewById(R.id.video_description);
        videoDescripton.setText(productData.get("description"));

        String userAgent = System.getProperty("http.agent", "");
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            userAgent += " [" + packageInfo.packageName + "/video_view/" + packageInfo.versionName + "]";
        } catch (Exception e) {
            e.printStackTrace();
            userAgent += " [" + getPackageName() + "/video_view]";
        }

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Referer", videoUrl.replace("video/video_play.php", ""));
        requestHeaders.put("customer_id", customerId);

        HttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
        dataSourceFactory.getDefaultRequestProperties().set(requestHeaders);
        Uri uri = Uri.parse(videoUrl);
        mVideoSource = new DashMediaSource(uri, dataSourceFactory, new DefaultDashChunkSource.Factory(dataSourceFactory, 5), null, null);

        Utils.triggerGAEvent(VideoViewActivity.this, "Video_View_Open_" + viewType, productId, customerId);

        PlaybackControlView controlView = (PlaybackControlView) mExoPlayerView.findViewById(R.id.exo_controller);
        mFullScreenIcon = (ImageView) controlView.findViewById(R.id.exo_fullscreen_icon);
        mFullScreenIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mExoPlayerFullscreen) {
                    openFullscreenDialog();
                    Utils.triggerGAEvent(VideoViewActivity.this, "Video_View_FullScreen", productId, customerId);
                } else {
                    closeFullscreenDialog();
                    Utils.triggerGAEvent(VideoViewActivity.this, "Video_View_Portrait", productId, customerId);
                }
            }
        });
    }

    private void openFullscreenDialog() {
        mVideoDetails.setVisibility(View.GONE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(VideoViewActivity.this, R.drawable.video_fullscreen_minimize));
        mExoPlayerFullscreen = true;
    }

    private void closeFullscreenDialog() {
        mVideoDetails.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().show();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(VideoViewActivity.this, R.drawable.video_fullscreen_maximize));
        mExoPlayerFullscreen = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mExoPlayerFullscreen) {
            closeFullscreenDialog();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(VideoViewActivity.this), new DefaultTrackSelector(new DefaultBandwidthMeter()), new DefaultLoadControl());
        player.addListener(this);
        mExoPlayerView.setPlayer(player);

        boolean haveResumePosition = mResumeWindow != C.INDEX_UNSET;

        if (haveResumePosition) {
            mExoPlayerView.getPlayer().seekTo(mResumeWindow, mResumePosition);
        }

        mExoPlayerView.getPlayer().prepare(mVideoSource);
        mExoPlayerView.getPlayer().setPlayWhenReady(true);

        String screenName = "Video_Portrait";
        if (mExoPlayerFullscreen) {
            openFullscreenDialog();
            screenName = "Video_FullScreen";
        }

        Utils.triggerScreen(VideoViewActivity.this, screenName);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mResumeWindow = mExoPlayerView.getPlayer().getCurrentWindowIndex();
        mResumePosition = Math.max(0, mExoPlayerView.getPlayer().getContentPosition());

        if (mExoPlayerView != null && mExoPlayerView.getPlayer() != null) {
            mExoPlayerView.getPlayer().release();
        }
    }

    @Override
    public void finish() {
        // TODO Auto-generated method stub
        mExoPlayerView.getPlayer().release();
        if (startTime > 0) {
            long currentTime = System.currentTimeMillis();
            int differenceTimeInMinutes = (int) Math.ceil((double) (currentTime - startTime) / 60000);    //1000*60
            Log.i("Video_Close_" + viewType, productId + " (View Time : " + differenceTimeInMinutes + " Minutes)");
            Utils.triggerGAEvent(VideoViewActivity.this, "Video_Close_" + viewType, productId, customerId, differenceTimeInMinutes);
        }
        startTime = 0;

        super.finish();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (isVideoError) {
            videoLoadingProgress.setVisibility(View.GONE);
            videoError.setVisibility(View.VISIBLE);
        } else {
            switch (playbackState) {
                case Player.STATE_ENDED:
                case Player.STATE_READY:
                    videoLoadingProgress.setVisibility(View.GONE);
                    break;
                default:
                    videoLoadingProgress.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        isVideoError = true;
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }
}