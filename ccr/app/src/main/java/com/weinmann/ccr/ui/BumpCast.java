package com.weinmann.ccr.ui;

import android.view.View;
import android.view.View.OnClickListener;

public class BumpCast implements OnClickListener {
    final CarCastResurrected carCast;
    final boolean direction;

    public BumpCast(CarCastResurrected carCast, boolean direction) {
        this.carCast = carCast;
        this.direction = direction;
    }

    @Override public void onClick(View v) {
        if (direction) {
            carCast.getContentService().next();
        } else {
            carCast.getContentService().previous();
        }
        carCast.updateUI();
    }
}
