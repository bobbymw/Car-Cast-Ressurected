package com.weinmann.ccr.ui;

import android.view.View;
import android.view.View.OnClickListener;

public class Bumper implements OnClickListener {
    int bump;
    CarCastResurrected carCast;

    public Bumper(CarCastResurrected carCast, int bump) {
        this.bump = bump;
        this.carCast = carCast;
    }

    @Override public void onClick(View v) {
        carCast.getContentService().bumpForwardSeconds(bump);
        carCast.updateUI();
    }
}
