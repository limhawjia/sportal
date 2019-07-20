package wjhj.orbital.sportsmatchfindingapp.utils;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;
import com.google.android.material.textfield.TextInputLayout;

public class BindingAdapters {

    @BindingAdapter("android:bindingSrc")
    public static void setImageResource(ImageView view, int resource) {
        view.setImageResource(resource);
    }

    @BindingAdapter("android:errorText")
    public static void setErrorMessage(TextInputLayout view, String errorMessage) {
        view.setError(errorMessage);
    }

    @BindingAdapter("android:errorText")
    public static void setErrorMessage(TextView view, String errorMessage) {
        view.setError(errorMessage);
    }

}