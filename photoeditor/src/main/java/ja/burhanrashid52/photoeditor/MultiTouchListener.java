package ja.burhanrashid52.photoeditor;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created on 18/01/2017.
 *
 * @author <a href="https://github.com/burhanrashid52">Burhanuddin Rashid</a>
 * <p></p>
 */
class MultiTouchListener implements OnTouchListener {

    private static final int INVALID_POINTER_ID = -1;
    private final GestureDetector mGestureListener;
    private boolean isRotateEnabled = true;
    private boolean isTranslateEnabled = true;
    private boolean isScaleEnabled = true;
    private float minimumScale = 0.5f;
    private float maximumScale = 10.0f;
    private int mActivePointerId = INVALID_POINTER_ID;
    private float mPrevX, mPrevY, mPrevRawX, mPrevRawY;
    private ScaleGestureDetector mScaleGestureDetector;

    private int[] location = new int[2];
    private Rect outRect;
    private View deleteView;
    private ImageView photoEditImageView;
    private RelativeLayout parentView;

    private OnMultiTouchListener onMultiTouchListener;
    private OnGestureControl mOnGestureControl;
    private DragDeleteListener dragDeleteListener;
    private boolean mIsTextPinchZoomable;
    private OnPhotoEditorListener mOnPhotoEditorListener;

    private PhotoEditor mPhotoEditor;

    MultiTouchListener(@Nullable View deleteView, RelativeLayout parentView,
                       ImageView photoEditImageView, boolean isTextPinchZoomable,
                       OnPhotoEditorListener onPhotoEditorListener, PhotoEditor mPhotoEditor, DragDeleteListener dragDeleteListener) {
        mIsTextPinchZoomable = isTextPinchZoomable;
        this.dragDeleteListener = dragDeleteListener;
        mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
        mGestureListener = new GestureDetector(new GestureListener());
        this.mPhotoEditor = mPhotoEditor;
        this.deleteView = deleteView;
        this.parentView = parentView;
        this.photoEditImageView = photoEditImageView;
        this.mOnPhotoEditorListener = onPhotoEditorListener;
        if (deleteView != null) {
            outRect = new Rect(deleteView.getLeft(), deleteView.getTop(),
                    deleteView.getRight(), deleteView.getBottom());
        } else {
            outRect = new Rect(0, 0, 0, 0);
        }
    }

    private static float adjustAngle(float degrees) {
        if (degrees > 180.0f) {
            degrees -= 360.0f;
        } else if (degrees < -180.0f) {
            degrees += 360.0f;
        }

        return degrees;
    }

    private static void move(View view, TransformInfo info) {
        computeRenderOffset(view, info.pivotX, info.pivotY);
        adjustTranslation(view, info.deltaX, info.deltaY);

        float scale = view.getScaleX() * info.deltaScale;
        scale = Math.max(info.minimumScale, Math.min(info.maximumScale, scale));
        view.setScaleX(scale);
        view.setScaleY(scale);

        float rotation = adjustAngle(view.getRotation() + info.deltaAngle);
        view.setRotation(rotation);
    }

    private static void adjustTranslation(View view, float deltaX, float deltaY) {
        float[] deltaVector = {deltaX, deltaY};
        view.getMatrix().mapVectors(deltaVector);
        view.setTranslationX(view.getTranslationX() + deltaVector[0]);
        view.setTranslationY(view.getTranslationY() + deltaVector[1]);
    }

    private static void computeRenderOffset(View view, float pivotX, float pivotY) {
        if (view.getPivotX() == pivotX && view.getPivotY() == pivotY) {
            return;
        }

        float[] prevPoint = {0.0f, 0.0f};
        view.getMatrix().mapPoints(prevPoint);

        view.setPivotX(pivotX);
        view.setPivotY(pivotY);

        float[] currPoint = {0.0f, 0.0f};
        view.getMatrix().mapPoints(currPoint);

        float offsetX = currPoint[0] - prevPoint[0];
        float offsetY = currPoint[1] - prevPoint[1];

        view.setTranslationX(view.getTranslationX() - offsetX);
        view.setTranslationY(view.getTranslationY() - offsetY);
    }

    int width = 0;
    int height = 0;
    boolean isScaleUp = true;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(view, event);
        mGestureListener.onTouchEvent(event);

        if (!isTranslateEnabled) {
            return true;
        }

        int action = event.getAction();

        int x = (int) event.getRawX();
        int y = (int) event.getRawY();

        switch (action & event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragDeleteListener.onTouch();
                if (width == 0) {
                    width = view.getWidth();
                }
                if (height == 0) {
                    height = view.getHeight();
                }
                Log.d("Drag ", "ACTION_DOWN");
                mPrevX = event.getX();
                mPrevY = event.getY();
                mPrevRawX = event.getRawX();
                mPrevRawY = event.getRawY();
                mActivePointerId = event.getPointerId(0);
                if (deleteView != null) {
                    deleteView.setVisibility(View.VISIBLE);
                }
                view.bringToFront();
                firePhotoEditorSDKListener(view, true);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("Drag ", "ACTION_MOVE");
                if (deleteView != null) {
                    deleteView.setVisibility(View.VISIBLE);
                }
                int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                if (pointerIndexMove != -1) {
                    float currX = event.getX(pointerIndexMove);
                    float currY = event.getY(pointerIndexMove);
                    if (!mScaleGestureDetector.isInProgress()) {
                        adjustTranslation(view, currX - mPrevX, currY - mPrevY);
                    }
                }
                dragDeleteListener.onMove();
                if (deleteView != null) {
                    final Rect imageViewArea = new Rect();
                    deleteView.getGlobalVisibleRect(imageViewArea);
                    if(imageViewArea.contains(x, y)) {
                        // swipe is passing over ImageView....

                        scaleDraggedView(view, false);
                        Log.d("Overlap ", " " + true);
                    }else{
                        scaleDraggedView(view, true);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                dragDeleteListener.onStop();
                scaleDraggedView(view, false);
                Log.d("Drag ", "ACTION_CANCEL");
                if (deleteView != null) {
                    deleteView.setVisibility(View.VISIBLE);
                }
                mActivePointerId = INVALID_POINTER_ID;

                break;
            case MotionEvent.ACTION_UP:
                dragDeleteListener.onStop();
                scaleDraggedView(view, false);
                Log.d("Drag ", "ACTION_UP");
                mActivePointerId = INVALID_POINTER_ID;
                if (deleteView != null && isViewInBounds(deleteView, x, y)) {
                    if (onMultiTouchListener != null)
                        onMultiTouchListener.onRemoveViewListener(view);
                } else if (!isViewInBounds(photoEditImageView, x, y)) {
                    view.animate().translationY(0).translationY(0);
                }

                firePhotoEditorSDKListener(view, false);
                if (deleteView != null) {
                    final Rect imageViewArea = new Rect();
                    deleteView.getGlobalVisibleRect(imageViewArea);
                    if(imageViewArea.contains(x, y)) {
                        // swipe is passing over ImageView....
                        Log.d("Overlap ", " " + true);
                        mPhotoEditor.viewUndo(view, ViewType.TEXT);
                    }

                    deleteView.setVisibility(View.GONE);

                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                dragDeleteListener.onStop();
                Log.d("Drag ", "ACTION_POINTER_UP");
                int pointerIndexPointerUp = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int pointerId = event.getPointerId(pointerIndexPointerUp);
                if (pointerId == mActivePointerId) {
                    int newPointerIndex = pointerIndexPointerUp == 0 ? 1 : 0;
                    mPrevX = event.getX(newPointerIndex);
                    mPrevY = event.getY(newPointerIndex);
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                if (deleteView != null) {
                    if (isViewOverlapping(view, deleteView, mPrevRawX, mPrevRawY)) {
                        mPhotoEditor.viewUndo(view, ViewType.TEXT);
                    }
                    deleteView.setVisibility(View.GONE);
                }
                break;
        }
        return true;
    }

    private void scaleDraggedView(View view, boolean scaleDown) {
       /* boolean alreadyScaled = (scaleDown && view.getScaleX() < 1f) || (!scaleDown && view.getScaleX() == 1f);
        if (alreadyScaled)
            return;*/
        float from = scaleDown ? 1f : .4f;
        float to = scaleDown ? .4f : 1f;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", from, to);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", from, to);
        AnimatorSet scaleAnimSet = new AnimatorSet();
        scaleAnimSet.play(scaleX).with(scaleY);
        scaleAnimSet.setDuration(600);
        scaleAnimSet.start();
    }

    private boolean isViewOverlapping(View firstView, View secondView, float x, float y) {
        int[] firstPosition = new int[2];
        int[] secondPosition = new int[2];
        firstPosition[0] = (int) x;
        firstPosition[1] = (int) y;
        firstView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        firstView.getLocationOnScreen(firstPosition);
        secondView.getLocationOnScreen(secondPosition);
        int r = 10 + firstPosition[0];
        int l = secondPosition[0];
        return r >= l && (r != 0 && l != 0);
    }

    private void objectAnimator(View view){
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.7f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.7f);
        scaleDownX.setDuration(1500);
        scaleDownY.setDuration(1500);

        ObjectAnimator moveUpY = ObjectAnimator.ofFloat(view, "translationY", -100);
        moveUpY.setDuration(1500);

        AnimatorSet scaleDown = new AnimatorSet();
        AnimatorSet moveUp = new AnimatorSet();

        scaleDown.play(scaleDownX).with(scaleDownY);
        moveUp.play(moveUpY);

        scaleDown.start();
        moveUp.start();
    }

    private void firePhotoEditorSDKListener(View view, boolean isStart) {
        Object viewTag = view.getTag();
        if (mOnPhotoEditorListener != null && viewTag != null && viewTag instanceof ViewType) {
            if (isStart)
                mOnPhotoEditorListener.onStartViewChangeListener(((ViewType) view.getTag()));
            else
                mOnPhotoEditorListener.onStopViewChangeListener(((ViewType) view.getTag()));
        }
    }

    private boolean isViewInBounds(View view, int x, int y) {
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }

    void setOnMultiTouchListener(OnMultiTouchListener onMultiTouchListener) {
        this.onMultiTouchListener = onMultiTouchListener;
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mPivotX;
        private float mPivotY;
        private Vector2D mPrevSpanVector = new Vector2D();

        @Override
        public boolean onScaleBegin(View view, ScaleGestureDetector detector) {
            mPivotX = detector.getFocusX();
            mPivotY = detector.getFocusY();
            mPrevSpanVector.set(detector.getCurrentSpanVector());
            return mIsTextPinchZoomable;
        }

        @Override
        public boolean onScale(View view, ScaleGestureDetector detector) {
            TransformInfo info = new TransformInfo();
            info.deltaScale = isScaleEnabled ? detector.getScaleFactor() : 1.0f;
            info.deltaAngle = isRotateEnabled ? Vector2D.getAngle(mPrevSpanVector, detector.getCurrentSpanVector()) : 0.0f;
            info.deltaX = isTranslateEnabled ? detector.getFocusX() - mPivotX : 0.0f;
            info.deltaY = isTranslateEnabled ? detector.getFocusY() - mPivotY : 0.0f;
            info.pivotX = mPivotX;
            info.pivotY = mPivotY;
            info.minimumScale = minimumScale;
            info.maximumScale = maximumScale;
            move(view, info);
            return !mIsTextPinchZoomable;
        }
    }

    public class TransformInfo {
        float deltaX;
        float deltaY;
        float deltaScale;
        float deltaAngle;
        float pivotX;
        float pivotY;
        float minimumScale;
        float maximumScale;
    }

    interface OnMultiTouchListener {
        void onEditTextClickListener(String text, int colorCode);

        void onRemoveViewListener(View removedView);
    }

    interface OnGestureControl {
        void onClick();

        void onLongClick();
    }


    interface DragDeleteListener{
        void onMove();
        void onTouch();
        void onStop();
    }

    void setOnGestureControl(OnGestureControl onGestureControl) {
        mOnGestureControl = onGestureControl;
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mOnGestureControl != null) {
                mOnGestureControl.onClick();
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            if (mOnGestureControl != null) {
                mOnGestureControl.onLongClick();
            }
        }
    }
}