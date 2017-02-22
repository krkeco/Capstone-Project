package com.krkeco.dateit.admob;

import android.app.Activity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.krkeco.dateit.R;

/**
 * Created by KC on 2/21/2017.
 */

public class AdMob {
    private InterstitialAd mInterstitialAd;
    private Activity mActivity;


    public AdMob(Activity activity) {
    mActivity = activity;
    }

        public InterstitialAd newInterstitialAd() {
            InterstitialAd interstitialAd = new InterstitialAd(mActivity);
            interstitialAd.setAdUnitId(mActivity.getString(R.string.interstitial_ad_unit_id));
            interstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    //mNextLevelButton.setEnabled(true);
                    loadInterstitial();
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    //mNextLevelButton.setEnabled(true);
                }

                @Override
                public void onAdClosed() {
                    // Proceed to the next level.
                    goToNextLevel();
                }
            });

            return interstitialAd;
        }

    public  void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and reload the ad.
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            //Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
            goToNextLevel();
        }
    }

    public void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        mInterstitialAd.loadAd(adRequest);
    }

    public  void goToNextLevel() {
        // Show the next level and reload the ad to prepare for the level after.
        mInterstitialAd = newInterstitialAd();
        loadInterstitial();
    }





}
