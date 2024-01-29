package com.bely.automotiveplayerclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BrowseActivity extends AppCompatActivity {

    List<BrowseItem> mBrowseList = new ArrayList<>();

    RecyclerView mBrowseView;
    BrowseAdapter mBrowseAdapter;

    Handler mUIHandler;
    Handler mBGHandler;

    String mCurrentParent;

    TextView mPath;
    List<String> browsePath = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        mBrowseView = findViewById(R.id.listview);
        mPath = findViewById(R.id.browsepath);
        mBrowseAdapter = new BrowseAdapter();
        mBrowseView.setAdapter(mBrowseAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mBrowseView.setLayoutManager(layoutManager);

        HandlerThread bgthread = new HandlerThread("workthread");
        bgthread.start();

        mBGHandler = new Handler(bgthread.getLooper());
        mUIHandler = new Handler(Looper.getMainLooper());

        mCurrentParent = "root";
        mBGHandler.post(() -> browse());
    }

    private void updatePath() {
        runOnUiThread(()-> {
            String pathStr = "";
            for (String path : browsePath) {
                pathStr += ">" + path;
            }
            mPath.setText(pathStr);
        });
    }
    private void browse() {
        if (Utils.browser != null && Utils.browser.isConnected()) {
            browsePath.add(mCurrentParent);
            updatePath();
            Utils.browser.subscribe(mCurrentParent, mBrowseCallback);
        }
    }

    private void handleBrowseList(List<MediaBrowserCompat.MediaItem> children) {
        mBrowseList.clear();

        if (children != null && children.size() > 0) {
            for (MediaBrowserCompat.MediaItem item : children) {
                BrowseItem bitem = new BrowseItem();
                bitem.mName = item.getDescription().getTitle().toString();
                bitem.mIsPlayable = item.isPlayable();
                bitem.mediaId = item.getMediaId();
                mBrowseList.add(bitem);
            }
        }

        mBrowseAdapter.notifyDataSetChanged();
    }

    private MediaBrowserCompat.SubscriptionCallback mBrowseCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            mUIHandler.post(() -> handleBrowseList(children));
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
            mUIHandler.post(() -> handleBrowseList(children));
        }

        @Override
        public void onError(@NonNull String parentId) {

        }

        @Override
        public void onError(@NonNull String parentId, @NonNull Bundle options) {

        }
    };

    public void onBack(View view) {
        if (browsePath.size() > 0) {
            mBGHandler.post(()-> {
                Utils.browser.unsubscribe(mCurrentParent);
                mCurrentParent = browsePath.remove(browsePath.size()-1);
                updatePath();
                Utils.browser.subscribe(mCurrentParent, mBrowseCallback);
            });
        } else {
            if (Utils.browser != null && Utils.browser.isConnected() && !TextUtils.isEmpty(mCurrentParent)) {
                mBGHandler.post(()-> Utils.browser.unsubscribe(mCurrentParent));
            }
            finish();
        }
    }

    public class BrowseAdapter extends RecyclerView.Adapter <BrowseAdapter.BrowseViewHolder> {

        @NonNull
        @Override
        public BrowseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sourceitem, parent, false);
            return new BrowseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BrowseViewHolder holder, int position) {
            holder.mName.setText(mBrowseList.get(position).mName);
            holder.mIcon.setImageResource(mBrowseList.get(position).mIsPlayable ?
                    android.R.drawable.ic_media_play :
                    android.R.drawable.sym_contact_card);
            holder.itemView.setTag(mBrowseList.get(position).mediaId);
            holder.itemView.setOnClickListener(l->{
                mBGHandler.post(()-> Utils.browser.unsubscribe(mCurrentParent));
                mCurrentParent = (String)l.getTag();
                mBGHandler.post(() -> browse());
            });
        }

        @Override
        public int getItemCount() {
            return mBrowseList.size();
        }

        public class BrowseViewHolder extends RecyclerView.ViewHolder {
            TextView mName;
            ImageView mIcon;

            public BrowseViewHolder(@NonNull View itemView) {
                super(itemView);

                mName = itemView.findViewById(R.id.text_sourcename);
                mIcon = itemView.findViewById(R.id.img_sourceicon);
            }
        }
    }

    public class BrowseItem {
        String mName;
        boolean mIsPlayable;
        String mediaId;
    }
}