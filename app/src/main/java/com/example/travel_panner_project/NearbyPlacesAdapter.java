package com.example.travel_panner_project;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NearbyPlacesAdapter
        extends RecyclerView.Adapter<NearbyPlacesAdapter.PlaceViewHolder> {

    private final List<NearbyPlace> places;

    public NearbyPlacesAdapter(List<NearbyPlace> places) {
        this.places = places;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_place, parent, false);
        return new PlaceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder h, int position) {
        NearbyPlace place = places.get(position);

        h.name.setText(place.title);

        // Address line: use description if available, else "title, area"
        if (place.description != null && !place.description.isEmpty()) {
            h.address.setText(place.description);
        } else {
            h.address.setText(place.title);
        }

        // Distance — convert metres to km, show as "X km"
        if (place.dist < 1000) {
            h.distance.setText(place.dist + " m");
        } else {
            h.distance.setText(String.format("%.1f km", place.dist / 1000.0));
        }

        // Image
        if (place.thumbUrl != null && !place.thumbUrl.isEmpty()) {
            h.noImageLayout.setVisibility(View.GONE);
            Glide.with(h.image.getContext())
                    .load(place.thumbUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_place_placeholder)
                    .into(h.image);
        } else {
            h.noImageLayout.setVisibility(View.VISIBLE);
            h.image.setImageDrawable(null);
        }

        // Tap → open Wikipedia page in browser
        h.itemView.setOnClickListener(v -> {
            String url = "https://en.wikipedia.org/wiki/"
                    + Uri.encode(place.title.replace(" ", "_"));
            v.getContext().startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        ImageView    image;
        LinearLayout noImageLayout;
        TextView     name, address, distance;

        PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            image         = itemView.findViewById(R.id.placeImage);
            noImageLayout = itemView.findViewById(R.id.noImageLayout);
            name          = itemView.findViewById(R.id.placeName);
            address       = itemView.findViewById(R.id.placeAddress);
            distance      = itemView.findViewById(R.id.placeDistance);
        }
    }
}
