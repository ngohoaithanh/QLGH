package com.hoaithanh.qlgh.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hoaithanh.qlgh.model.goong.PlaceAutoCompleteResponse;

import java.util.ArrayList;
import java.util.List;

public class PlaceSuggestionAdapter extends ArrayAdapter<String> {
    private final List<PlaceAutoCompleteResponse.Prediction> data = new ArrayList<>();

    public PlaceSuggestionAdapter(@NonNull Context ctx) {
        super(ctx, android.R.layout.simple_list_item_1, new ArrayList<>());
    }

    public void setPredictions(List<PlaceAutoCompleteResponse.Prediction> predictions) {
        data.clear();
        if (predictions != null) data.addAll(predictions);

        // update display list (description)
        clear();
        for (PlaceAutoCompleteResponse.Prediction p : data) {
            add(p.description);
        }
        notifyDataSetChanged();
    }

    @Nullable
    public PlaceAutoCompleteResponse.Prediction getPredictionAt(int position) {
        if (position < 0 || position >= data.size()) return null;
        return data.get(position);
    }
}
