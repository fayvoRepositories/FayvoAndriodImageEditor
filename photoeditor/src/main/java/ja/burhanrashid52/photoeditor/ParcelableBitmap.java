package ja.burhanrashid52.photoeditor;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableBitmap implements Parcelable {
    private BitmapLoader laptop;

    public ParcelableBitmap() {
    }

    public BitmapLoader getLaptop() {
        return laptop;
    }

    public ParcelableBitmap(BitmapLoader laptop) {
        super();
        this.laptop = laptop;
    }

    private ParcelableBitmap(Parcel in) {
        laptop = new BitmapLoader();
        laptop.setBitmap((Bitmap) in.readParcelable(Bitmap.class
                .getClassLoader()));
    }

    /*
     * you can use hashCode() here.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /*
     * Actual object Serialization/flattening happens here. You need to
     * individually Parcel each property of your object.
     */
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(laptop.getBitmap(),
                PARCELABLE_WRITE_RETURN_VALUE);
    }

    /*
     * Parcelable interface must also have a static field called CREATOR,
     * which is an object implementing the Parcelable.Creator interface.
     * Used to un-marshal or de-serialize object from Parcel.
     */
    public static final Parcelable.Creator<ParcelableBitmap> CREATOR =
            new Parcelable.Creator<ParcelableBitmap>() {
                public ParcelableBitmap createFromParcel(Parcel in) {
                    return new ParcelableBitmap(in);
                }

                public ParcelableBitmap[] newArray(int size) {
                    return new ParcelableBitmap[size];
                }
            };
}