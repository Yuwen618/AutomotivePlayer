package com.bely.automotiveplayerclient;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayer extends ConstraintLayout {

    private static final String TAG = MusicPlayer.class.getSimpleName();
    private Spinner mSourceList;
    private ImageButton mBtn_Prev;
    private ImageButton mBtn_PlayPause;
    private ImageButton mBtn_Next;
    private Button mBtn_Login;
    private Button mBtn_Setting;
    private ImageButton mBtn_Shuffle;
    private ImageButton mBtn_Repeat;
    private Button mBtn_Custom;
    private TextView mTtile;
    private TextView mArtist;
    private LinearLayout mLayout_CustomBtns;
    private ImageView mAlbumart;
    private SeekBar mProgressbar;
    private TextView mCurPosition;
    private TextView mTotalDuration;

    private Handler mBGHandler;
    private Handler mUIHandler;

    private Context mContext;
    private SourceInfo mCurrentSource;

    private long mCurrentDuration;

    List<SourceInfo> mAllSources = new ArrayList<>();

    List<SourceHelper> mAllSourceHelpers = new ArrayList<>();
    private SourceHelper mCurrentSourceHelper;
    public MusicPlayer(@NonNull Context context) {
        super(context);
        init(context);
    }


    public MusicPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MusicPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.musicplayer, this, true);

        mUIHandler = new Handler();
        HandlerThread bgthread = new HandlerThread("bgthread_musicplayer");
        bgthread.start();
        mBGHandler = new Handler(bgthread.getLooper());

        mSourceList = findViewById(R.id.spinner_sourcelist);
        mBtn_Prev = findViewById(R.id.imageButton_prev);
        mBtn_PlayPause = findViewById(R.id.imageButton_playpause);
        mBtn_Next = findViewById(R.id.imageButton_next);
        mBtn_Login = findViewById(R.id.btn_login);
        mBtn_Setting = findViewById(R.id.btn_setting);
        mBtn_Custom = findViewById(R.id.imageButton_custombtn);
        mBtn_Shuffle = findViewById(R.id.imageButton_shuffle);
        mBtn_Repeat = findViewById(R.id.imageButton_repeat);
        mLayout_CustomBtns = findViewById(R.id.layout_custombutton);

        mTtile = findViewById(R.id.textView_title);
        mArtist = findViewById(R.id.textView_artist);

        mAlbumart = findViewById(R.id.albumart);

        mProgressbar = findViewById(R.id.progressbar);
        mCurPosition = findViewById(R.id.text_currentposition);
        mTotalDuration = findViewById(R.id.text_totalduration);

        mSourceList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SourceInfo source = (SourceInfo) selectedItemView.getTag();
                if (source == null) return;
                mCurrentSource = source;
                if (source.mType == SourceInfo.SOURCE_TYPE.TYPE_APP) {
                    playApp(source);
                } else {

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        mBtn_Prev.setOnClickListener(l-> {
            mCurrentSourceHelper.skipToPrev();
        });

        mBtn_PlayPause.setOnClickListener(l-> {
            mCurrentSourceHelper.toggleplay();
        });

        mBtn_Next.setOnClickListener(l-> {
            mCurrentSourceHelper.skipToNext();
        });

        mBtn_Setting.setOnClickListener(v-> {
            if (v.getTag() != null) {
                Intent intent = (Intent) v.getTag();
                mContext.startActivity(intent);
            }
        });

        mBtn_Custom.setOnClickListener(l-> {
            mLayout_CustomBtns.setVisibility(mLayout_CustomBtns.getVisibility()== View.VISIBLE ?
                    View.GONE : View.VISIBLE);
        });

        mBtn_Shuffle.setOnClickListener(l-> {
            mCurrentSourceHelper.toggleShuffle();
        });

        mBtn_Repeat.setOnClickListener(l-> {
            mCurrentSourceHelper.toggleRepeat();
        });

        mAllSourceHelpers.add(SourceHelper.getInstance());
        //add more

        mBGHandler.post(()->loadSource());
    }

    private void runOnUIThread(Runnable r) {
        mUIHandler.post(r);
    }

    private void updateProgressbar(int curposition, long totalduration) {
        if (totalduration == 0) {
            mProgressbar.setMax(0);
            mProgressbar.setMin(0);
            mProgressbar.setProgress(0);

            mCurPosition.setText("");
            mTotalDuration.setText("");
        } else {
            mProgressbar.setMax((int) totalduration);
            mProgressbar.setMin(0);
            mProgressbar.setProgress(curposition);

            mCurPosition.setText(formatSeconds(curposition));
            mTotalDuration.setText(formatSeconds(totalduration));
        }
    }

    public String formatSeconds(long seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int remainingSeconds = (int) (seconds % 60);

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private UIListener mUIListener = new UIListener() {
        @Override
        public void onMetadataChange(String title, String artist) {
            runOnUIThread(() -> {
                mTtile.setText(title);
                mArtist.setText(artist);
            });
        }

        @Override
        public void onAlbumartChange(Bitmap albumart) {
            runOnUIThread(() -> {
                mAlbumart.setImageBitmap(albumart);
            });
        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            if (state == null) return;
            runOnUIThread(() -> {
                handlePlaybackState(state);
            });
        }

        @Override
        public void onShowSettingBtn(Intent intent) {
            runOnUIThread(() -> {
                mBtn_Setting.setVisibility(intent != null ? View.VISIBLE : View.GONE);
                mBtn_Setting.setTag(intent);
            });
        }

        @Override
        public void onShuffleMode(int shuffleMode) {
            runOnUIThread(() -> {
                int icon = R.drawable.shuffleoff;
                if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL ||
                        shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_GROUP) {
                    icon = R.drawable.shuffle;
                }
                mBtn_Shuffle.setImageResource(icon);
            });
        }

        @Override
        public void onRepeatMode(int repeatMode) {
            runOnUIThread(() -> {
                int icon = R.drawable.repeatnone;
                if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL ||
                    repeatMode == PlaybackStateCompat.REPEAT_MODE_GROUP) {
                    icon = R.drawable.repeatall;
                } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
                    icon = R.drawable.repeat1;
                }
                mBtn_Repeat.setImageResource(icon);
            });
        }

        @Override
        public void onDurationChanged(long aLong) {
            mCurrentDuration = aLong/1000;
        }
    };

    private void handlePlaybackState(PlaybackStateCompat state) {
        if (state.getState() == PlaybackStateCompat.STATE_ERROR) {
            updateProgressbar(0, 0);
            mTtile.setText(state.getErrorMessage());
            mArtist.setText("");
            mAlbumart.setImageBitmap(null);
            mAlbumart.setVisibility(View.GONE);
            if (state.getErrorCode() == PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED) {
                //show login button
                mBtn_Login.setVisibility(View.VISIBLE);
                Bundle extra = state.getExtras();
                String label = extra.getString("android.media.extras.ERROR_RESOLUTION_ACTION_LABEL");
                mBtn_Login.setText(label);
                PendingIntent intent = (PendingIntent) extra.getParcelable("android.media.extras.ERROR_RESOLUTION_ACTION_INTENT");
                mBtn_Login.setOnClickListener((v) -> {
                    try {
                        intent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.i(TAG, "unable to load login");
                    }
                });
            }
        } else {
            mBtn_Login.setVisibility(View.GONE);
            mAlbumart.setVisibility(View.VISIBLE);
            updatePlayPause(state.getState() == PlaybackStateCompat.STATE_BUFFERING ||
                    state.getState() == PlaybackStateCompat.STATE_PLAYING);
            Log.i(TAG, "handlePlaybackState, position="+state.getPosition()+",duration="+mCurrentDuration);
            updateProgressbar((int) state.getPosition()/1000, mCurrentDuration);
        }
        updateShuffleRepeat(state);
        updateCustomButton(state);

    }

    private void updateCustomButton(PlaybackStateCompat state) {
        mLayout_CustomBtns.removeAllViews();
        if (state.getCustomActions() != null && state.getCustomActions().size() > 0) {
            mBtn_Custom.setVisibility(View.VISIBLE);

            List<PlaybackStateCompat.CustomAction> actions = state.getCustomActions();
            PackageManager packageManager = mContext.getPackageManager();
            for (PlaybackStateCompat.CustomAction action : actions) {
                LinearLayout btnLayout = (LinearLayout) LayoutInflater.from(mContext)
                        .inflate(R.layout.custombtn, null);
                ImageButton btn = btnLayout.findViewById(R.id.imgbtn);
                Resources resources = null;
                try {
                    resources = packageManager.getResourcesForApplication(mCurrentSource.getPkgName());
                } catch (PackageManager.NameNotFoundException e) {
                    continue;
                }
                Drawable d = resources.getDrawable(action.getIcon());
                btn.setImageDrawable(d);
                btn.setTag(action);
                btn.setOnClickListener(l->{
                    PlaybackStateCompat.CustomAction a = (PlaybackStateCompat.CustomAction) l.getTag();
                    mCurrentSourceHelper.onCustomAction(a);
                });
                mLayout_CustomBtns.addView(btnLayout);
            }
        } else {
            mBtn_Custom.setVisibility(View.GONE);
        }
    }

    private void updateShuffleRepeat(PlaybackStateCompat state) {
        if ((state.getActions() & PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE) != 0) {
            mBtn_Shuffle.setVisibility(View.VISIBLE);
        }
        if ((state.getActions() & PlaybackStateCompat.ACTION_SET_REPEAT_MODE) != 0) {
            mBtn_Repeat.setVisibility(View.VISIBLE);
        }
    }

    private void updatePlayPause(boolean playing) {
        mBtn_PlayPause.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void playApp(SourceInfo source) {
        if (source == null) return;
        mCurrentSourceHelper = SourceHelper.getInstance();
        mCurrentSourceHelper.registerUIListener(mUIListener);
        mCurrentSourceHelper.connect(source);
        mCurrentSourceHelper.play();
    }

    private void loadSource() {
        for (SourceHelper helper : mAllSourceHelpers) {
            if (helper != null) {
                mAllSources.addAll(helper.loadSources());
            }
        }

        mSourceAdapter = new SourceListAdapter(mContext);
        mSourceList.setAdapter(mSourceAdapter);
        mSourceAdapter.setSourceList(mAllSources);
    }

    private SourceListAdapter mSourceAdapter;

    public class SourceListAdapter extends BaseAdapter {
        private Context context;
        private List<SourceInfo> dataList;

        public SourceListAdapter(Context context) {
            this.context = context;
        }

        public void setSourceList(List<SourceInfo> dataList) {
            this.dataList = dataList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return dataList != null ? dataList.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return dataList != null ? dataList.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (dataList == null) return null;
            // 获取当前项的数据
            SourceInfo data = dataList.get(position);

            // 使用LayoutInflater加载自定义布局
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.sourceitem, null);

            // 获取布局中的控件并设置数据
            TextView sourcename = view.findViewById(R.id.text_sourcename);
            ImageView sourceicon = view.findViewById(R.id.img_sourceicon);

            sourcename.setText(data.getSourceName());
            sourceicon.setImageBitmap(data.getSourceIcon());

            view.setTag(data);

            return view;
        }
    }



    public static class SourceInfo {
        public enum SOURCE_TYPE {
            TYPE_APP,
        }
        private String mSourceName;
        private String mPkgName;
        private String mClsName;
        private Bitmap mIcon;
        private SOURCE_TYPE mType;
        public SourceInfo(String name, String pkg, String cls, Bitmap icon, SOURCE_TYPE type) {
            mSourceName = name;
            mPkgName = pkg;
            mClsName = cls;
            mIcon = icon;
            mType = type;
        }

        public String getSourceName() {
            return mSourceName;
        }

        public String getPkgName() {
            return mPkgName;
        }

        public String getClsName() {
            return mClsName;
        }

        public Bitmap getSourceIcon() {
            return mIcon;
        }
    }

}
