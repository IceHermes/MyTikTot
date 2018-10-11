package com.github.mvps;

import android.app.Activity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

public class BasePresenter implements Presenter {
    private View mRootView;

    private List<Presenter> mChildPresenterList = new ArrayList<>();

    private boolean isValid = true;
    private boolean isBinding = false;
    private boolean isInitialized = false;

    @Override
    public void create(View view) {
        if (isInitialized()) {
            throw new IllegalStateException("Presenter只能被初始化一次!");
        }
        try {
            mRootView = view;

            ButterKnife.bind(this, view);

            createChildren();

            onCreate();

        } catch (Exception e) {
            isValid = false;
        }
        isInitialized = true;
    }

    private void onCreate() {

    }

    private void createChildren() {
        for (Presenter childPresenter : mChildPresenterList) {
            childPresenter.create(mRootView);
        }
    }

    @Override
    public void bind(Object... callerContext) {
        if (!isValid) {
            return;
        }
        if (!isInitialized) {
            throw new IllegalStateException("Presenter必须先初始化!");
        }
//        if (mInjector == null) {
//            mInjector = Injectors.injector(getClass());
//        }
//
//        checkInjection(callerContext);
//
//        mInjector.reset(this);
//
//        bindChild(callerContext);
//
//        if (callerContext != null) {
//            for (Object context : callerContext) {
//                mInjector.inject(this, context);
//            }
//        }
//
//        onBind(callerContext);

        isBinding = true;
    }

    @Override
    public void unbind() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public Presenter add(Presenter presenter) {
        return null;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public Activity getActivity() {
        return null;
    }
}
