package org.gdg.frisbee.android.eventseries;

import android.os.Bundle;

import com.android.volley.Response;

import org.gdg.frisbee.android.Const;
import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.adapter.EventAdapter;
import org.gdg.frisbee.android.api.ApiRequest;
import org.gdg.frisbee.android.api.model.Event;
import org.gdg.frisbee.android.app.App;
import org.gdg.frisbee.android.cache.ModelCache;
import org.gdg.frisbee.android.utils.Utils;
import org.joda.time.DateTime;

import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class GdgEventListFragment extends EventListFragment {

    public static EventListFragment newInstance(String plusId) {
        EventListFragment fragment = new GdgEventListFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Const.EXTRA_PLUS_ID, plusId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    EventAdapter createEventAdapter() {
        return new EventAdapter(getActivity());
    }

    @Override
    void fetchEvents() {
        final DateTime now = new DateTime();
        mStart = now.minusMonths(2).dayOfMonth().withMinimumValue();
        mEnd = now.plusYears(1).dayOfMonth().withMaximumValue();

        setIsLoading(true);
        final String plusId = getArguments().getString(Const.EXTRA_PLUS_ID);
        final String cacheKey = "event_" + plusId;

        Response.Listener<ArrayList<Event>> listener = new Response.Listener<ArrayList<Event>>() {
            @Override
            public void onResponse(final ArrayList<Event> events) {
                mEvents.addAll(events);

                App.getInstance().getModelCache().putAsync(cacheKey, mEvents, DateTime.now().plusHours(2), new ModelCache.CachePutListener() {
                    @Override
                    public void onPutIntoCache() {
                        mAdapter.addAll(mEvents);
                        setIsLoading(false);
                    }
                });
            }
        };

        ApiRequest fetchEvents = mClient.getChapterEventList(mStart, mEnd, plusId, listener, mErrorListener);

        if (Utils.isOnline(getActivity())) {
            fetchEvents.execute();
        } else {
            App.getInstance().getModelCache().getAsync(cacheKey, false, new ModelCache.CacheListener() {
                @Override
                public void onGet(Object item) {
                    ArrayList<Event> events = (ArrayList<Event>) item;

                    mAdapter.addAll(events);
                    setIsLoading(false);
                    Crouton.makeText(getActivity(), getString(R.string.cached_content), Style.INFO).show();
                }

                @Override
                public void onNotFound(String key) {
                    setIsLoading(false);
                    Crouton.makeText(getActivity(), getString(R.string.offline_alert), Style.ALERT).show();
                }
            });
        }
    }
}
