package sp.phone.fragment.material;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import gov.anzong.androidnga.R;
import sp.phone.adapter.TopicViewPagerAdapter;
import sp.phone.bean.TopicListRequestInfo;
import sp.phone.common.BoardManager;
import sp.phone.common.BoardManagerImpl;
import sp.phone.common.PhoneConfiguration;
import sp.phone.fragment.SearchDialogFragment;
import sp.phone.fragment.TopicListContainer;
import sp.phone.presenter.contract.TopicListContract;
import sp.phone.utils.StringUtil;
import sp.phone.view.ScrollableViewPager;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshAttacher;

/**
 * Created by Yang Yihang on 2017/6/3.
 */

public class TopicTabFragment extends MaterialCompatFragment implements View.OnClickListener{

    private PullToRefreshAttacher mAttacher = null;

    private static final String TAG = TopicTabFragment.class.getSimpleName();

    private TopicListRequestInfo mRequestInfo;

    private TopicListContract.Presenter[] mPresenters = new TopicListContract.Presenter[3];

    private int mCurrentIndex;

    private boolean[] mPreloadFlags = new boolean[3];

    private FloatingActionsMenu mFam;

    private String mBoardName;

    private BoardManager mBoardManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayoutId(R.layout.fragment_material_topic_list);
        mBoardManager = BoardManagerImpl.getInstance();
        mRequestInfo = getArguments().getParcelable("requestInfo");
        mBoardName = mRequestInfo.boardName;
        if (mBoardName == null) {
            mBoardName = mBoardManager.getBoardName(String.valueOf(mRequestInfo.fid));
            if (mBoardName != null) {
                getActivity().setTitle(mBoardName);
            }
        }
    }

    @Override
    public View onCreateContainerView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mAttacher = getAttacher();
        ViewPager viewPager = new ScrollableViewPager(getContext());
        viewPager.setId(R.id.pager);
        viewPager.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final TopicViewPagerAdapter adapter = new TopicViewPagerAdapter(getChildFragmentManager(),mPresenters,mRequestInfo);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                mCurrentIndex = position;
                if (!mPreloadFlags[position]){
                    mPreloadFlags[position] = true;
                    getCurrentPresenter().refresh();
                }
                super.onPageSelected(position);
            }
        });
        setTabViewPager(viewPager);
        return viewPager;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (mRequestInfo.searchMode) {
            handleSearch();
        }
        mPreloadFlags[mCurrentIndex] = true;
        updateFloatingMenu();
        super.onViewCreated(view, savedInstanceState);
    }

    private void updateFloatingMenu() {

        View rootView =  getView();
        rootView.findViewById(R.id.fab_post).setOnClickListener(this);
        rootView.findViewById(R.id.fab_refresh).setOnClickListener(this);
        mFam = (FloatingActionsMenu) rootView.findViewById(R.id.fab_menu);
        if (mConfiguration.isLeftHandMode()) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mFam.getLayoutParams();
            lp.gravity = Gravity.START | Gravity.BOTTOM;
            mFam.setExpandDirection(FloatingActionsMenu.EXPAND_UP,FloatingActionsMenu.LABELS_ON_RIGHT_SIDE);
            mFam.setLayoutParams(lp);
        }
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.topic_list_menu, menu);
    }


    @Override
    public void onResume() {
        mFam.collapse();
        super.onResume();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mBoardName == null) {
            menu.findItem(R.id.menu_add_bookmark).setVisible(false);
            menu.findItem(R.id.menu_remove_bookmark).setVisible(false);
        } else if (mBoardManager.isBookmarkBoard(String.valueOf(mRequestInfo.fid))){
            menu.findItem(R.id.menu_add_bookmark).setVisible(false);
            menu.findItem(R.id.menu_remove_bookmark).setVisible(true);
        } else {
            menu.findItem(R.id.menu_add_bookmark).setVisible(true);
            menu.findItem(R.id.menu_remove_bookmark).setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.threadlist_menu_newthread:
                handlePostThread();
                break;
            case R.id.goto_bookmark_item:
                Intent intent_bookmark = new Intent(getActivity(), PhoneConfiguration.getInstance().topicActivityClass);
                intent_bookmark.putExtra("favor", 1);
                startActivity(intent_bookmark);
                break;
            case R.id.search:
                Intent intent = new Intent();
                intent.putExtra("fid", mRequestInfo.fid);
                intent.putExtra("action", "search");
                if (!StringUtil.isEmpty(PhoneConfiguration.getInstance().userName)) {// 登入了才能发
                    handleSearch();
                } else {
                    intent.setClass(getActivity(), PhoneConfiguration.getInstance().loginActivityClass);
                    startActivity(intent);
                    if (PhoneConfiguration.getInstance().showAnimation) {
                        getActivity().overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
                    }
                }
                break;
            case R.id.menu_add_bookmark:
                mBoardManager.addBookmark(String.valueOf(mRequestInfo.fid),mBoardName);
                break;
            case R.id.menu_remove_bookmark:
                mBoardManager.removeBookmark(String.valueOf(mRequestInfo.fid));
            default:
                return false;
        }
        return true;
    }


    private void handleSearch() {
        Bundle arg = new Bundle();
        arg.putInt("id", mRequestInfo.fid);
        arg.putInt("authorid", mRequestInfo.authorId);
        DialogFragment df = new SearchDialogFragment();
        df.setArguments(arg);
        final String dialogTag = "search_dialog";
        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(dialogTag);
        if (prev != null) {
            ft.remove(prev);
        }
        try {
            df.show(ft, dialogTag);
        } catch (Exception e) {
            Log.e(TopicListContainer.class.getSimpleName(), Log.getStackTraceString(e));
        }
    }

    private boolean handlePostThread() {
        Intent intent = new Intent();
        intent.putExtra("fid", mRequestInfo.fid);
        intent.putExtra("action", "new");
        if (!StringUtil.isEmpty(PhoneConfiguration.getInstance().userName)) {// 登入了才能发
            intent.setClass(getActivity(), PhoneConfiguration.getInstance().postActivityClass);
        } else {
            intent.setClass(getActivity(), PhoneConfiguration.getInstance().loginActivityClass);
        }
        startActivity(intent);
        if (PhoneConfiguration.getInstance().showAnimation) {
            getActivity().overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
        }
        return true;
    }

    private TopicListContract.Presenter getCurrentPresenter(){
        return mPresenters[mCurrentIndex];
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_refresh:
                getCurrentPresenter().refresh();
                getCurrentPresenter().showFirstItem();
                mFam.collapse();
                break;
            case R.id.fab_post:
                handlePostThread();
                break;
        }
    }
}
