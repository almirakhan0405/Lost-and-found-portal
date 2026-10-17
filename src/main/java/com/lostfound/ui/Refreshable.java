package com.lostfound.ui;

/** Panels implementing this reload their data whenever their tab is selected. */
interface Refreshable {
    void refresh();
}
