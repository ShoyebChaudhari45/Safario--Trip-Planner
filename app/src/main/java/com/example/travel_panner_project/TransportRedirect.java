package com.example.travel_panner_project;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class TransportRedirect {

    public static void openBusBooking(Context context, String source, String destination) {
        String url = "https://www.redbus.in/search?fromCityName="
                + Uri.encode(source)
                + "&toCityName=" + Uri.encode(destination);
        openWebPage(context, url);
    }

    public static void openTrainBooking(Context context, String source, String destination) {
        String url = "https://www.irctc.co.in/nget/train-search?fromStation="
                + Uri.encode(source)
                + "&toStation=" + Uri.encode(destination);
        openWebPage(context, url);
    }

    public static void openFlightBooking(Context context, String source, String destination) {
        String url = "https://www.google.com/flights?hl=en#flt="
                + Uri.encode(source) + "." + Uri.encode(destination);
        openWebPage(context, url);
    }

    private static void openWebPage(Context context, String url) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
