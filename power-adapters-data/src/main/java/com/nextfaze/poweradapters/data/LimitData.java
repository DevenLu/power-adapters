package com.nextfaze.poweradapters.data;

import lombok.NonNull;

import static java.lang.Math.max;
import static java.lang.Math.min;

final class LimitData<T> extends DataWrapper<T> {

    @NonNull
    private final Data<? extends T> mData;

    private final int mLimit;

    LimitData(@NonNull Data<? extends T> data, int limit) {
        super(data);
        mData = data;
        mLimit = max(0, limit);
    }

    @NonNull
    @Override
    public T get(int position, int flags) {
        if (position >= size()) {
            throw new IndexOutOfBoundsException("Position: " + position + ", size: " + size());
        }
        return mData.get(position, flags);
    }

    @Override
    public int size() {
        return min(super.size(), mLimit);
    }

    @Override
    protected void forwardItemRangeChanged(int innerPositionStart, int innerItemCount) {
        if (innerItemCount > 0 && innerPositionStart < mLimit) {
            notifyItemRangeChanged(innerPositionStart, min(innerItemCount, mLimit - innerPositionStart));
        }
    }

    @Override
    protected void forwardItemRangeInserted(int innerPositionStart, int innerItemCount) {
        if (innerItemCount > 0 && innerPositionStart < mLimit) {
            int innerTotalPostInsert = super.size();
            int innerTotalPreInsert = innerTotalPostInsert - innerItemCount;
            if (innerTotalPreInsert >= mLimit) {
                notifyItemRangeChanged(innerPositionStart, mLimit - innerPositionStart);
            } else {
                int insertCount = min(mLimit - innerPositionStart, innerItemCount);
                if (innerPositionStart <= innerTotalPreInsert) {
                    int remainingSpace = mLimit - innerTotalPreInsert;
                    int removeCount = insertCount - remainingSpace;
                    if (removeCount > 0) {
                        notifyItemRangeRemoved(innerTotalPreInsert - removeCount, removeCount);
                    }
                }
                notifyItemRangeInserted(innerPositionStart, insertCount);
            }
        }
    }

    @Override
    protected void forwardItemRangeRemoved(int innerPositionStart, int innerItemCount) {
        if (innerItemCount > 0 && innerPositionStart < mLimit) {
            int innerTotalPostRemove = super.size();
            int innerTotalPreRemove = innerTotalPostRemove + innerItemCount;
            if (innerTotalPostRemove >= mLimit) {
                notifyItemRangeChanged(innerPositionStart, mLimit - innerPositionStart);
            } else {
                int removeCount = min(mLimit - innerPositionStart, innerItemCount);
                notifyItemRangeRemoved(innerPositionStart, removeCount);
                if (innerPositionStart + innerItemCount >= mLimit) {
                    int insertCount = innerTotalPreRemove - innerPositionStart - innerItemCount;
                    if (insertCount > 0) {
                        notifyItemRangeInserted(innerPositionStart, insertCount);
                    }
                }
            }
        }
    }

    @Override
    protected void forwardItemRangeMoved(int innerFromPosition, int innerToPosition, int innerItemCount) {
        int upperBound = max(innerFromPosition + innerItemCount, innerToPosition + innerItemCount);
        if (upperBound < mLimit) {
            notifyItemRangeMoved(innerFromPosition, innerToPosition, innerItemCount);
        } else {
            // TODO: Split into a removal and an insertion?
            notifyDataChanged();
        }
    }
}
