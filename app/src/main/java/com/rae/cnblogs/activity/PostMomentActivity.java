package com.rae.cnblogs.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.rae.cnblogs.AppMobclickAgent;
import com.rae.cnblogs.AppRoute;
import com.rae.cnblogs.AppUI;
import com.rae.cnblogs.GlideApp;
import com.rae.cnblogs.R;
import com.rae.cnblogs.ThemeCompat;
import com.rae.cnblogs.dialog.IAppDialog;
import com.rae.cnblogs.dialog.IAppDialogClickListener;
import com.rae.cnblogs.dialog.impl.DefaultDialog;
import com.rae.cnblogs.message.PostMomentEvent;
import com.rae.cnblogs.presenter.CnblogsPresenterFactory;
import com.rae.cnblogs.presenter.IPostMomentContract;
import com.rae.cnblogs.sdk.model.ImageMetaData;
import com.rae.cnblogs.sdk.model.MomentMetaData;
import com.rae.swift.Rx;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 发布闪存
 * Created by ChenRui on 2017/10/27 0027 14:04.
 */
@Route(path = AppRoute.PATH_MOMENT_POST)
public class PostMomentActivity extends BasicActivity implements IPostMomentContract.View {
    @BindView(R.id.et_content)
    EditText mContentView;
    @BindView(R.id.tv_post)
    TextView mPostView;
    @BindView(R.id.ll_blog_apply_tips)
    View mBlogApplyTipsLayout;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    private IPostMomentContract.Presenter mPresenter;
    private PostImageAdapter mAdapter;
    private MomentMetaData mMomentMetaData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(com.rae.cnblogs.R.anim.slide_in_bottom, android.R.anim.fade_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_moment);
        showHomeAsUp();

        // 发布失败后传递的对象
        mMomentMetaData = getIntent().getParcelableExtra(Intent.EXTRA_TEXT);

        mContentView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mPostView.setEnabled(mContentView.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mPresenter = CnblogsPresenterFactory.getPostMomentPresenter(this, this);
        mRecyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        mAdapter = new PostImageAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnAddImageClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppRoute.jumpToImageSelection(PostMomentActivity.this, mAdapter.getImageSelectedList());
            }
        });
        mAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppRoute.routeToImagePreview(PostMomentActivity.this, mAdapter.getImageSelectedList(), (Integer) v.getTag(), mAdapter.getImageSelectedList(), mAdapter.getMaxCount());
            }
        });


        if (mMomentMetaData != null) {
            mContentView.setText(mMomentMetaData.content);
            mContentView.setSelection(mContentView.length());
            ArrayList<String> urls = new ArrayList<>();
            for (ImageMetaData image : mMomentMetaData.images) {
                urls.add(image.localPath);
            }
            mAdapter.setUrls(urls);
            mAdapter.notifyDataSetChanged();
        }

        String msg = getIntent().getStringExtra(Intent.EXTRA_HTML_TEXT); // 发布失败的消息
        if (!TextUtils.isEmpty(msg)) {
            DefaultDialog dialog = new DefaultDialog(this);
            dialog.setMessage(msg);
            dialog.setCancelButtonVisibility(View.GONE);
            dialog.show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.destroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == AppRoute.REQ_IMAGE_SELECTION || requestCode == AppRoute.REQ_CODE_IMAGE_SELECTED) {
                mAdapter.setUrls(data.getStringArrayListExtra("selectedImages"));
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected int getHomeAsUpIndicator() {
        if (ThemeCompat.isNight()) return R.drawable.ic_back_closed_night;
        return R.drawable.ic_back_closed;
    }

    @Override
    public String getContent() {
        return mContentView.getText().toString();
    }

    @Override
    public void onPostMomentFailed(String msg) {
        mPostView.setEnabled(true);
        AppUI.dismiss();
        DefaultDialog dialog = new DefaultDialog(this);
        dialog.setMessage(msg);
        dialog.setCancelButtonVisibility(View.GONE);
        dialog.show();
    }

    @Override
    public void onPostMomentSuccess() {
        AppUI.success(this, R.string.tips_post_moment_success);
        EventBus.getDefault().post(new PostMomentEvent(0, true, null));
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public List<String> getImageUrls() {
        return mAdapter.getImageSelectedList();
    }

    @Override
    public void onPostMomentInProgress() {
        AppUI.dismiss();
        if (config().getPostMomentInProgressTips()) {
            DefaultDialog dialog = new DefaultDialog(this);
            dialog.setMessage(getString(R.string.tips_post_moment_progress));
            dialog.setCancelButtonVisibility(View.GONE);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
            dialog.show();
        } else {
            AppUI.success(getContext(), R.string.tips_post_moment_progress_simple);
            finish();
        }
    }

    @Override
    public void onLoadBlogOpenStatus(Boolean value) {
        mBlogApplyTipsLayout.setVisibility(value ? View.GONE : View.VISIBLE);
    }

    @OnClick(R.id.tv_blog_apply)
    public void onBlogApplyClick() {
        AppRoute.routeToWeb(this, getString(R.string.url_blog_apply));
    }

    @OnClick(R.id.tv_post)
    public void onPostViewClick() {
        // 检查是否开通博客
        if (!mPresenter.isBlogOpened() && Rx.getCount(getImageUrls()) > 0) {
            DefaultDialog dialog = new DefaultDialog(this);
            dialog.setMessage(getString(R.string.tips_post_moment_apply));
            dialog.setEnSureText("继续发布");
            dialog.setCancelText(getString(R.string.blog_apply));
            dialog.setOnCancelListener(new IAppDialogClickListener() {
                @Override
                public void onClick(IAppDialog dialog, int buttonType) {
                    dialog.dismiss();
                    onBlogApplyClick();
                }
            });
            dialog.setOnEnSureListener(new IAppDialogClickListener() {
                @Override
                public void onClick(IAppDialog dialog, int buttonType) {
                    dialog.dismiss();
                    performPostMoment();
                }
            });
            dialog.show();
            return;
        }

        performPostMoment();


    }

    private void performPostMoment() {
        // 统计发布
        if (mPresenter.post()) {
            AppUI.loading(this, "正在发布");
            AppMobclickAgent.onClickEvent(this, "PostMoment_Publish");
            mPostView.setEnabled(false);
        } else {
            AppMobclickAgent.onClickEvent(this, "PostMoment_Return");
        }
    }

    @Override
    public void onBackPressed() {
        if (!TextUtils.isEmpty(getContent()) || !Rx.isEmpty(getImageUrls())) {
            // 提示
            DefaultDialog dialog = new DefaultDialog(this);
            dialog.setMessage("内容还没有发布，真的要放弃吗？");
            dialog.setEnSureText("我再想想");
            dialog.setCancelText("不想要了");
            dialog.setOnCancelListener(new IAppDialogClickListener() {
                @Override
                public void onClick(IAppDialog dialog, int buttonType) {
                    finish();
                }
            });

            dialog.show();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, com.rae.cnblogs.R.anim.slide_out_bottom);
    }

    private static class PostImageHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;

        public PostImageHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.img_photo);
        }
    }

    private static class PostImageAdapter extends RecyclerView.Adapter<PostImageHolder> {

        private static final int VIEW_TYPE_ADD = 1;
        private static final int VIEW_TYPE_NORMAL = 0;
        private ArrayList<String> mUrls = new ArrayList<>();
        private LayoutInflater mLayoutInflater;
        private View.OnClickListener mOnAddImageClickListener;
        private View.OnClickListener mOnClickListener;
        private final int mMaxCount = 6;

        public PostImageAdapter() {
            super();
        }

        @Override
        public PostImageHolder onCreateViewHolder(ViewGroup parent, int i) {
            if (mLayoutInflater == null) {
                mLayoutInflater = LayoutInflater.from(parent.getContext());
            }
            return new PostImageHolder(mLayoutInflater.inflate(R.layout.item_post_moment_image, parent, false));
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0) return VIEW_TYPE_ADD;
            if (mUrls.size() < mMaxCount && position == getItemCount() - 1) return VIEW_TYPE_ADD;
            return VIEW_TYPE_NORMAL;
        }

        @Override
        public void onBindViewHolder(PostImageHolder holder, int position) {
            int viewType = getItemViewType(position);
            holder.itemView.setTag(position);
            switch (viewType) {
                case VIEW_TYPE_ADD:
                    holder.itemView.setOnClickListener(mOnAddImageClickListener);
                    holder.mImageView.setImageResource(R.drawable.ic_add_photo_holder);
                    break;
                default:
                    holder.itemView.setOnClickListener(mOnClickListener);
                    GlideApp.with(holder.itemView.getContext()).load(mUrls.get(position % mUrls.size())).into(holder.mImageView);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return Math.min(mMaxCount, mUrls.size() + 1);
        }

        public void setUrls(ArrayList<String> urls) {
            mUrls = urls;
        }

        public void remove(String fileName) {
            mUrls.remove(fileName);
        }

        public void setOnAddImageClickListener(View.OnClickListener listener) {
            mOnAddImageClickListener = listener;
        }

        public ArrayList<String> getImageSelectedList() {
            return mUrls;
        }

        public void setOnClickListener(View.OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
        }

        public int getMaxCount() {
            return mMaxCount;
        }
    }
}
