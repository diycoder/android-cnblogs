package com.rae.cnblogs.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.rae.cnblogs.AppRoute;
import com.rae.cnblogs.AppUI;
import com.rae.cnblogs.R;
import com.rae.cnblogs.RaeViewCompat;
import com.rae.cnblogs.adapter.BaseItemAdapter;
import com.rae.cnblogs.adapter.MomentAdapter;
import com.rae.cnblogs.adapter.MomentDetailAdapter;
import com.rae.cnblogs.dialog.IAppDialog;
import com.rae.cnblogs.dialog.IAppDialogClickListener;
import com.rae.cnblogs.dialog.impl.EditCommentDialog;
import com.rae.cnblogs.dialog.impl.HintCardDialog;
import com.rae.cnblogs.dialog.impl.MenuDeleteDialog;
import com.rae.cnblogs.model.MomentHolder;
import com.rae.cnblogs.presenter.CnblogsPresenterFactory;
import com.rae.cnblogs.presenter.IMomentDetailContract;
import com.rae.cnblogs.sdk.UserProvider;
import com.rae.cnblogs.sdk.bean.BlogCommentBean;
import com.rae.cnblogs.sdk.bean.FriendsInfoBean;
import com.rae.cnblogs.sdk.bean.MomentBean;
import com.rae.cnblogs.sdk.bean.MomentCommentBean;
import com.rae.cnblogs.widget.AppLayout;
import com.rae.cnblogs.widget.PlaceholderView;
import com.rae.cnblogs.widget.RaeLoadMoreView;
import com.rae.cnblogs.widget.RaeRecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;

/**
 * 闪存详情
 * Created by ChenRui on 2017/11/2 0002 15:35.
 */
public class MomentDetailFragment extends BasicFragment implements IMomentDetailContract.View {

    @BindView(R.id.recycler_view)
    RaeRecyclerView mRecyclerView;
    @BindView(R.id.placeholder)
    PlaceholderView mPlaceholderView;
    @BindView(R.id.ptr_content)
    AppLayout mAppLayout;
    private MomentBean mData;
    private MomentDetailAdapter mAdapter;
    private IMomentDetailContract.Presenter mPresenter;

    private EditCommentDialog mEditCommentDialog;
    private MenuDeleteDialog mDeleteDialog;
    private final View.OnClickListener mOnFollowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!UserProvider.getInstance().isLogin()) {
                AppRoute.routeToLogin(v.getContext());
                return;
            }
            ((Button) v).setText("请稍后");
            v.setEnabled(false);
            mPresenter.follow();
        }
    };


    public static MomentDetailFragment newInstance(MomentBean data) {
        Bundle args = new Bundle();
        args.putParcelable("data", data);
        MomentDetailFragment fragment = new MomentDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fm_moment_detail;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPresenter = CnblogsPresenterFactory.getMomentDetailPresenter(getContext(), this);

        if (getArguments() != null) {
            mData = getArguments().getParcelable("data");
        }
    }


    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mData == null) {
            mPlaceholderView.empty("闪存数据为空");
            mAppLayout.setEnabled(false);
            return;
        }


        mEditCommentDialog = new EditCommentDialog(getContext());
        mEditCommentDialog.setOnEditCommentListener(new EditCommentDialog.OnEditCommentListener() {
            @Override
            public void onPostComment(String content, BlogCommentBean parent, boolean isReference) {
                // 发布评论
                mEditCommentDialog.showLoading();
                MomentCommentBean commentBean = mEditCommentDialog.getMomentCommentBean();
                performPostComment(content, commentBean);
            }

            private void performPostComment(String content, MomentCommentBean commentBean) {
                String ingId = mData.getId();
                String userId = commentBean == null ? mData.getUserAlias() : commentBean.getUserAlias();
                String commentId = commentBean == null ? "0" : commentBean.getId();
                if (commentBean != null && !TextUtils.isEmpty(commentBean.getUserAlias())) {
                    content = String.format("@%s：%s", commentBean.getAuthorName(), content);
                }
                mPresenter.postComment(ingId, userId, commentId, content);
            }
        });

        mDeleteDialog = new MenuDeleteDialog(this.getContext());
        mDeleteDialog.setOnDeleteClickListener(new MenuDeleteDialog.onDeleteClickListener() {
            @Override
            public void onMenuDeleteClicked() {
                // 执行删除
                AppUI.loading(getContext(), "正在删除");
                String tag = mDeleteDialog.getTag().toString();
                if ("ing".equalsIgnoreCase(tag)) {
                    // 删除闪存
                    mPresenter.deleteMoment();
                } else {
                    mPresenter.deleteComment(mDeleteDialog.getTag().toString());
                }
            }
        });

        mPlaceholderView.dismiss();

        mAdapter = new MomentDetailAdapter(mData);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setNoMoreText(R.string.no_more_comment);
        mRecyclerView.setPullRefreshEnabled(false);
        mRecyclerView.setLoadingMoreEnabled(true);
        mAdapter.setOnPlaceholderClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCommentClick();
            }
        });
        mAdapter.setOnBloggerClickListener(new MomentAdapter.OnBloggerClickListener() {
            @Override
            public void onBloggerClick(String blogApp) {
                AppRoute.routeToBlogger(getContext(), blogApp);
            }
        });
        mAdapter.setOnFollowClickListener(mOnFollowClickListener);
        mAdapter.setOnItemClickListener(new BaseItemAdapter.onItemClickListener<MomentCommentBean>() {
            @Override
            public void onItemClick(MomentCommentBean item) {
                if (item == null) {
                    AppUI.failed(getContext(), "数据为空");
                    return;
                }

                // 如果是自己的话就弹出删除
                if (UserProvider.getInstance().isLogin() && UserProvider.getInstance().getLoginUserInfo().getBlogApp().equals(item.getBlogApp())) {
                    mDeleteDialog.setTag(item.getId());
                    mDeleteDialog.show();
                } else {
                    mEditCommentDialog.show(item);
                }
            }
        });
        mAdapter.setMomentDeleteOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 删除闪存
                mDeleteDialog.setTag("ing");
                mDeleteDialog.show();
            }
        });

        // 加载更多样式
        RaeLoadMoreView footView = mRecyclerView.getFootView();
        footView.setTextColor(ContextCompat.getColor(getContext(), R.color.dividerColor));
        footView.setPadding(footView.getPaddingLeft(), footView.getPaddingTop() + 20, footView.getPaddingRight(), footView.getPaddingBottom() + 20);

        mRecyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {

            }

            @Override
            public void onLoadMore() {
                mPresenter.loadMore();
            }
        });

        mAppLayout.setPtrHandler(new PtrDefaultHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                if (mData != null) {
                    mPresenter.refresh();
                }
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return mRecyclerView.isOnTop();
            }
        });

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().findViewById(R.id.tool_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RaeViewCompat.scrollToTop(mRecyclerView);
            }
        });

        start();
    }

    public void start() {
        if (mData != null) {
            mPresenter.start();
        }
    }

    @Override
    public MomentBean getMomentInfo() {
        return mData;
    }

    @Override
    public void onEmptyComment(String message) {
        AppUI.dismiss();
        mRecyclerView.setNoMore(true);
        mAppLayout.refreshComplete();
        mRecyclerView.loadMoreComplete();
        mAdapter.empty(message);
    }

    @Override
    public void onLoadComments(List<MomentCommentBean> data, boolean hasMore) {
        AppUI.dismiss();
        mRecyclerView.loadMoreComplete();
        mAppLayout.refreshComplete();
        mAdapter.invalidate(data);
        mAdapter.notifyDataSetChanged();
        mRecyclerView.setNoMore(!hasMore);
    }

    @Override
    public void onPostCommentFailed(String message) {
        mEditCommentDialog.dismissLoading();
        AppUI.failed(getContext(), message);
    }

    @Override
    public void onPostCommentSuccess() {
        mEditCommentDialog.dismiss();
        AppUI.toastInCenter(getContext(), getString(R.string.tips_comment_success));
        // 重新加载
        mPresenter.refresh();
    }

    @Override
    public String getBlogApp() {
        return mData.getBlogApp();
    }

    @Override
    public void onLoadBloggerInfoFailed(String msg) {
        MomentHolder holder = mAdapter.getMomentHolder();
        if (holder != null && holder.followView != null) {
            holder.followView.setEnabled(true);
            if (UserProvider.getInstance().isLogin())
                holder.followView.setText("信息异常");
        }
    }

    @Override
    public void onLoadBloggerInfo(FriendsInfoBean info) {
        MomentHolder holder = mAdapter.getMomentHolder();
        if (holder != null && holder.followView != null) {
            holder.followView.setEnabled(true);
            holder.followView.setText(info.isFollowed() ? "取消关注" : "加关注");
        }
    }

    @Override
    public void onFollowFailed(String msg) {
        AppUI.failed(getContext(), msg);
        onFollowSuccess();
    }

    @Override
    public void onFollowSuccess() {
        MomentHolder holder = mAdapter.getMomentHolder();
        if (holder != null && holder.followView != null) {
            holder.followView.setEnabled(true);
            holder.followView.setText(mPresenter.isFollowed() ? R.string.cancel_follow : R.string.following);
        }
    }

    @Override
    public void onDeleteCommentFailed(String message) {
        AppUI.dismiss();
        AppUI.failed(getContext(), message);
    }

    @Override
    public void onDeleteMomentFailed(String msg) {
        AppUI.dismiss();
        AppUI.failed(getContext(), msg);
    }

    @Override
    public void onDeleteMomentSuccess() {
        AppUI.success(getContext(), R.string.tips_del_moment_success);
        getActivity().finish();
    }

    @Override
    public void onLoadMoreNotLogin() {
        mRecyclerView.loadMoreComplete();
        HintCardDialog dialog = new HintCardDialog(getContext());
        dialog.showCloseButton();
        dialog.setMessage(getString(R.string.moment_unlogin_hint));
        dialog.setEnSureText(getString(R.string.go_login));
        dialog.setOnEnSureListener(new IAppDialogClickListener() {
            @Override
            public void onClick(IAppDialog dialog, int buttonType) {
                dialog.dismiss();
                AppRoute.routeToLogin(getContext());
            }
        });
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPresenter.destroy();
    }

    /**
     * 评论
     */
    @OnClick(R.id.tv_edit_comment)
    public void onCommentClick() {
        mEditCommentDialog.show();
    }


}
