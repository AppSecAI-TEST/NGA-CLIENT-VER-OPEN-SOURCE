package sp.phone.presenter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.anzong.androidnga.activity.MyApp;
import sp.phone.bean.BoardCategory;
import sp.phone.bean.User;
import sp.phone.common.BoardManager;
import sp.phone.common.BoardManagerImpl;
import sp.phone.common.PhoneConfiguration;
import sp.phone.interfaces.PageCategoryOwner;
import sp.phone.presenter.contract.BoardContract;
import sp.phone.utils.HttpUtil;
import sp.phone.utils.StringUtil;

/**
 * Created by Yang Yihang on 2017/6/29.
 */

public class BoardPresenter implements BoardContract.Presenter, PageCategoryOwner {


    private BoardContract.View mView;

    private BoardManager mBoardManager;

    public BoardPresenter(BoardContract.View view) {
        mView = view;
        mView.setPresenter(this);
        mBoardManager = BoardManagerImpl.getInstance();
    }

    @Override
    public Context getContext() {
        return mView.getContext();
    }

    @Override
    public void setView(Object view) {

    }

    @Override
    public void loadBoardInfo() {

    }

    @Override
    public boolean addBoard(String fid, String name) {
        if (name.equals("")) {
            mView.showToast("请输入版面名称");
            return false;
        } else {
            Pattern pattern = Pattern.compile("-?[0-9]*");
            Matcher match = pattern.matcher(fid);
            boolean checkInt = true;
            try {
                Integer.parseInt(fid);
            } catch (NumberFormatException e) {
                checkInt = false;
            }
            if (!match.matches() || fid.equals("") || !checkInt) {
                mView.showToast("请输入正确的版面ID(个人版面要加负号)");
                return false;
            } else {// CHECK PASS, READY TO ADD FID
                for (int i = 0; i < mBoardManager.getCategorySize(); i++) {
                    BoardCategory curr = mBoardManager.getCategory(i);
                    for (int j = 0; j < curr.size(); j++) {
                        String URL = curr.get(j).getUrl();
                        if (URL.equals(fid)) {
                            mView.showToast("该版面已经存在于列表" + curr.get(j).getName() + "中");
                            return false;
                        }
                    }
                }
                mView.showToast("添加成功");
                BoardManagerImpl.getInstance().addBookmark(fid,name);
                return true;
            }
        }
    }

    @Override
    public void toggleUser(List<User> userList) {
        if (userList != null && userList.size() > 1) {
            int index = mView.switchToNextUser();
            User user = userList.get(index);
            MyApp app = (MyApp) getContext().getApplicationContext();
            PhoneConfiguration config = PhoneConfiguration.getInstance();
            app.addToUserList(user.getUserId(), user.getCid(),
                    user.getNickName(), user.getReplyString(),
                    user.getReplyTotalNum(), user.getBlackList());
            config.setUid(user.getUserId());
            config.setNickname(user.getNickName());
            config.setCid(user.getCid());
            config.setReplyString(user.getReplyString());
            config.setReplyTotalNum(user.getReplyTotalNum());
            config.blacklist = StringUtil.blackliststringtolisttohashset(user.getBlackList());
            mView.showToast("切换账户成功,当前账户名:" + user.getNickName());
        } else {
            mView.jumpToLogin();
        }
    }

    @Override
    public void toTopicListPage(int position, String fidString) {

        if (position != 0 && !HttpUtil.HOST_PORT.equals("")) {
            HttpUtil.HOST = HttpUtil.HOST_PORT + HttpUtil.Servlet_timer;
        }
        int fid = 0;
        try {
            fid = Integer.parseInt(fidString);
        } catch (Exception e) {
            final String tag = this.getClass().getSimpleName();
            Log.e(tag, Log.getStackTraceString(e));
            Log.e(tag, "invalid fid " + fidString);
        }
        if (fid == 0) {
            String tip = fidString + "绝对不存在";
            mView.showToast(tip);
            return;
        }

        Log.i(this.getClass().getSimpleName(), "set host:" + HttpUtil.HOST);

        String url = HttpUtil.Server + "/thread.php?fid=" + fidString + "&rss=1";
        PhoneConfiguration config = PhoneConfiguration.getInstance();
        if (!StringUtil.isEmpty(config.getCookie())) {
            url = url + "&" + config.getCookie().replace("; ", "&");
        } else if (fid < 0) {
            mView.jumpToLogin();
            return;
        }
        if (!StringUtil.isEmpty(url)) {
            Intent intent = new Intent();
            intent.putExtra("tab", "1");
            intent.putExtra("fid", fid);
            intent.setClass(getContext(), config.topicActivityClass);
            getContext().startActivity(intent);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        loadBoardInfo();
        mView.notifyDataSetChanged();
    }

    @Override
    public void clearRecentBoards() {
        mBoardManager.removeAllBookmarks();
        mView.notifyDataSetChanged();
    }


    @Override
    public int getCategoryCount() {
        return BoardManagerImpl.getInstance().getCategoryList().size();
    }

    @Override
    public String getCategoryName(int position) {
        return BoardManagerImpl.getInstance().getCategoryList().get(position).getName();
    }

    @Override
    public BoardCategory getCategory(int category) {
        return BoardManagerImpl.getInstance().getCategoryList().get(category);
    }


}
