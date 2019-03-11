package ja.burhanrashid52.photoeditor;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Created by Administrator on 4/20/2018.
 */

public class DragDropOnDragListener implements View.OnDragListener {

    private Context context = null;
    private LinearLayout ivDelete;
    public DragDropOnDragListener(Context context, LinearLayout ivDelete) {
        this.context = context;
        this.ivDelete = ivDelete;
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {

        // Get the drag drop action.
        int dragAction = dragEvent.getAction();

        if (dragAction == dragEvent.ACTION_DRAG_STARTED) {

            ivDelete.setVisibility(View.VISIBLE);
            // Check whether the dragged view can be placed in this target view or not.

            ClipDescription clipDescription = dragEvent.getClipDescription();

            if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {

                // Return true because the target view can accept the dragged object.
                return true;

            }
        } else if (dragAction == dragEvent.ACTION_DRAG_ENTERED) {
            ivDelete.setVisibility(View.VISIBLE);
            // When the being dragged view enter the target view, change the target view background color.
//            changeTargetViewBackground(view, Color.GREEN);

            return true;
        } else if (dragAction == dragEvent.ACTION_DRAG_EXITED) {
            ivDelete.setVisibility(View.GONE);
            // When the being dragged view exit target view area, clear the background color.
//            resetTargetViewBackground(view);

            return true;
        } else if (dragAction == dragEvent.ACTION_DRAG_ENDED) {
            ivDelete.setVisibility(View.GONE);
            // When the drop ended reset target view background color.
//            resetTargetViewBackground(view);


            // Get drag and drop action result.
            boolean result = dragEvent.getResult();

            // result is true means drag and drop action success.
            if (result) {
                Toast.makeText(context, "Drag and drop action complete successfully.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Drag and drop action failed.", Toast.LENGTH_LONG).show();
                view.setVisibility(View.VISIBLE);
            }

            return true;

        } else if (dragAction == dragEvent.ACTION_DROP) {
            // When drop action happened.
//            view.setVisibility(View.GONE);
            // Get clip data in the drag event first.
            ClipData clipData = dragEvent.getClipData();

            // Get drag and drop item count.
            int itemCount = clipData.getItemCount();

            // If item count bigger than 0.
            if (itemCount > 0) {
                // Get clipdata item.
                ClipData.Item item = clipData.getItemAt(0);

                // Get item text.
                String dragDropString = item.getText().toString();

                // Show a toast popup.
                Toast.makeText(context, "Dragged object is a " + dragDropString, Toast.LENGTH_LONG).show();

                // Reset target view background color.
                resetTargetViewBackground(view);

                // Get dragged view object from drag event object.
                View srcView = (View) dragEvent.getLocalState();
                // Get dragged view's parent view group.
                ViewGroup owner = (ViewGroup) srcView.getParent();
                // Remove source view from original parent view group.
                owner.removeView(srcView);

                // The drop target view is a LinearLayout object so cast the view object to it's type.
                LinearLayout newContainer = (LinearLayout) view;
                // Add the dragged view in the new container.
                newContainer.addView(srcView);
                // Make the dragged view object visible.
                srcView.setVisibility(View.VISIBLE);

                // Returns true to make DragEvent.getResult() value to true.
                return true;
            }

        } else if (dragAction == dragEvent.ACTION_DRAG_LOCATION) {
//            ivDelete.setVisibility(View.VISIBLE);
            return false;
        }else
        {
            ivDelete.setVisibility(View.GONE);
//            Toast.makeText(context, "Drag and drop unknow action type.", Toast.LENGTH_LONG).show();
        }

        return false;
    }


    /* Reset drag and drop target view's background color. */
    private void resetTargetViewBackground(View view) {
        // Clear color filter.
//        view.getBackground().clearColorFilter();

        // Redraw the target view use original color.
        view.invalidate();
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

    /* Change drag and drop target view's background color. */
    private void changeTargetViewBackground(View view, int color) {
        /* When the being dragged view enter the target view, change the target view background color.
         *
         *  color : The changed to color value.
         *
         *  mode : When to change the color, SRC_IN means source view ( dragged view ) enter target view.
         * */
//        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        // Redraw the target view use new color.
        view.invalidate();
    }
}