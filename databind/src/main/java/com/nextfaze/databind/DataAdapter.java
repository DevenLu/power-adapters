package com.nextfaze.databind;

import android.widget.BaseAdapter;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public abstract class DataAdapter<T> extends BaseAdapter implements DisposableListAdapter {

    @Getter
    @NonNull
    private final Data<T> mData;

    @NonNull
    private final DataObserver mDataObserver = new DataObserver() {
        @Override
        public void onChange() {
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            notifyDataSetInvalidated();
        }
    };

    public DataAdapter(@NonNull Data<T> data) {
        mData = data;
        mData.registerDataObserver(mDataObserver);
    }

    @Override
    public void dispose() {
        mData.unregisterDataObserver(mDataObserver);
    }

    @Override
    public final int getCount() {
        return mData.size();
    }

    @Override
    public final T getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}