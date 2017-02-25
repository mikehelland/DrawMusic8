package com.monadpad.le;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ThreeDeeFragment extends Fragment {

    private MonadPad3DActiFrag mActiFrag;
    public View onCreateView(LayoutInflater li, ViewGroup c, Bundle savedState){

        mActiFrag = new MonadPad3DActiFrag(getActivity(), c, savedState);

        return mActiFrag.getView();
    }

}
