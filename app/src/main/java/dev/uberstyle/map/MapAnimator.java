package dev.uberstyle.map;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class MapAnimator {
    private int GREY = Color.parseColor("#FFA7A6A6");
    private static MapAnimator mapAnimator;
    private Polyline backgroundPolyline;
    private AnimatorSet firstRunAnimSet;
    private Polyline foregroundPolyline;
    private PolylineOptions optionsForeground;
    private AnimatorSet secondLoopRunAnimSet;
    private boolean stopAnim = false;

    class MapAnimatorOne implements AnimatorUpdateListener {
        MapAnimatorOne() {
        }

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            List points = backgroundPolyline.getPoints();
            points.subList(0, (int) (((float) points.size()) * (((float) (Integer) valueAnimator.getAnimatedValue()) / 100.0f))).clear();
            MapAnimator.this.foregroundPolyline.setPoints(points);
        }
    }

    class MapAnimatorTwo implements AnimatorListener {
        public void onAnimationCancel(Animator animator) {
        }

        public void onAnimationRepeat(Animator animator) {
        }

        public void onAnimationStart(Animator animator) {
        }

        MapAnimatorTwo() {
        }

        public void onAnimationEnd(Animator animator) {
            MapAnimator.this.foregroundPolyline.setColor(GREY);
            MapAnimator.this.foregroundPolyline.setPoints(backgroundPolyline.getPoints());
        }
    }

    class MapAnimatorThree implements AnimatorUpdateListener {
        MapAnimatorThree() {
        }

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            MapAnimator.this.foregroundPolyline.setColor((Integer) valueAnimator.getAnimatedValue());
        }
    }

    class MapAnimatorFour implements AnimatorListener {
        public void onAnimationCancel(Animator animator) {
        }

        public void onAnimationRepeat(Animator animator) {
        }

        public void onAnimationStart(Animator animator) {
        }

        MapAnimatorFour() {
        }

        public void onAnimationEnd(Animator animator) {
            try {
                if (MapAnimator.this.foregroundPolyline != null && MapAnimator.this.backgroundPolyline != null) {
                    MapAnimator.this.backgroundPolyline.setPoints(MapAnimator.this.foregroundPolyline.getPoints());
                }
            } catch (Exception animator2) {
                animator2.printStackTrace();
            }
        }
    }

    private MapAnimator() {
    }

    static MapAnimator getInstance() {
        if (mapAnimator == null) {
            mapAnimator = new MapAnimator();
        }
        return mapAnimator;
    }

    void stopAnim() {
        if (this.firstRunAnimSet != null) {
            this.firstRunAnimSet.removeAllListeners();
            this.firstRunAnimSet.end();
            this.firstRunAnimSet.cancel();
            this.stopAnim = true;
            this.firstRunAnimSet = new AnimatorSet();
        }
    }

    void animateRoute(final GoogleMap googleMap, final List<LatLng> list) {
        this.stopAnim = false;
        if (this.firstRunAnimSet == null) {
            this.firstRunAnimSet = new AnimatorSet();
        } else {
            this.firstRunAnimSet.removeAllListeners();
            this.firstRunAnimSet.end();
            this.firstRunAnimSet.cancel();
            this.firstRunAnimSet = new AnimatorSet();
        }
        if (this.foregroundPolyline != null) {
            this.foregroundPolyline.remove();
        }
        if (this.backgroundPolyline != null) {
            this.backgroundPolyline.remove();
        }
        this.backgroundPolyline = googleMap.addPolyline(new PolylineOptions().add(list.get(0)).color(ViewCompat.MEASURED_STATE_MASK).width(8.0f));
        this.optionsForeground = new PolylineOptions().add(list.get(0)).color(-7829368).width(8.0f);
        this.foregroundPolyline = googleMap.addPolyline(this.optionsForeground);
        ValueAnimator ofInt = ValueAnimator.ofInt(0, 100);
        ofInt.setDuration(2000);
        ofInt.setInterpolator(new DecelerateInterpolator());
        ofInt.addUpdateListener(new MapAnimatorOne());
        ofInt.addListener(new MapAnimatorTwo());
        ValueAnimator ofObject = ValueAnimator.ofObject(new ArgbEvaluator(), GREY, ViewCompat.MEASURED_STATE_MASK);
        ofObject.setInterpolator(new AccelerateInterpolator());
        ofObject.setDuration(1200);
        ofObject.addUpdateListener(new MapAnimatorThree());
        ObjectAnimator ofObject2 = ObjectAnimator.ofObject(this, "routeIncreaseForward", new RouteEvaluator(), list.toArray());
        ofObject2.setInterpolator(new AccelerateDecelerateInterpolator());
        ofObject2.addListener(new MapAnimatorFour());
        ofObject2.setDuration(1600);
        this.firstRunAnimSet.playSequentially(ofObject2, ofInt);
        this.firstRunAnimSet.addListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                try {
                    firstRunAnimSet = null;
                    animateRoute(googleMap, list);
                } catch (Exception animator2) {
                    animator2.printStackTrace();
                }
            }
        });
        this.firstRunAnimSet.start();
    }

    public void setRouteIncreaseForward(LatLng latLng) {
        List points = this.foregroundPolyline.getPoints();
        points.add(latLng);
        this.foregroundPolyline.setPoints(points);
    }
}
