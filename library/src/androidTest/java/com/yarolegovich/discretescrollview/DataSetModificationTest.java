package com.yarolegovich.discretescrollview;

import android.support.test.runner.AndroidJUnit4;

import com.yarolegovich.discretescrollview.context.TestData;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.matcher.ViewMatchers.*;
import static com.yarolegovich.discretescrollview.custom.CustomAssertions.currentPositionIs;
import static com.yarolegovich.discretescrollview.custom.CustomAssertions.doesNotHaveChildren;
import static org.hamcrest.Matchers.*;

/**
 * Created by yarolegovich on 2/3/18.
 */
@RunWith(AndroidJUnit4.class)
public class DataSetModificationTest extends DiscreteScrollViewTest {

    @Test
    public void notifyItemInserted_afterCurrentPosition_currentPositionIsNotAffected() {
        final int initialPosition = scrollView.getCurrentItem();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                data.add(initialPosition + 1, new TestData());
                adapter.notifyItemInserted(initialPosition + 1);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition));
    }

    @Test
    public void notifyItemInserted_beforeCurrentPosition_currentPositionIsShifterRightByOne() {
        final int initialPosition = scrollView.getCurrentItem();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                data.add(initialPosition, new TestData());
                adapter.notifyItemInserted(initialPosition);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition + 1));
    }

    @Test
    public void notifyItemRemoved_afterCurrentPosition_currentPositionIsNotAffected() {
        final int initialPosition = scrollView.getCurrentItem();
        assertThat(adapter.getItemCount(), is(greaterThan(1)));
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                data.remove(initialPosition + 1);
                adapter.notifyItemRemoved(initialPosition + 1);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition));
    }

    @Test
    public void notifyItemRemoved_beforeCurrentPosition_currentPositionIsShifterLeftByOne() {
        assertThat(adapter.getItemCount(), is(greaterThan(1)));
        final int initialPosition = adapter.getItemCount() / 2;
        ensurePositionIs(initialPosition);
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                data.remove(initialPosition - 1);
                adapter.notifyItemRemoved(initialPosition - 1);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition - 1));
    }

    @Test
    public void notifyItemInserted_multipleInsertsBeforeCurrent_currentIsShiftedCorrectly() {
        final int numberOfInserts = 5;
        final int initialPosition = scrollView.getCurrentItem();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                for (int i = 0; i < numberOfInserts; i++) {
                    data.add(initialPosition, new TestData());
                    adapter.notifyItemInserted(initialPosition);
                }
            }
        });
        onScrollView().check(currentPositionIs(initialPosition + numberOfInserts));
    }

    @Test
    public void notifyItemRemoved_calledUntilEmpty_scrollViewIsEmpty() {
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                while (data.size() > 0) {
                    data.remove(0);
                    adapter.notifyItemRemoved(0);
                }
            }
        });
        onScrollView().check(doesNotHaveChildren());
    }

    @Test
    public void notifyItemRangeInserted_afterCurrentPosition_positionIsNotAffected() {
        final int numOfItemsToInsert = 5;
        final int initialPosition = scrollView.getCurrentItem();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                data.addAll(initialPosition + 1, createItems(numOfItemsToInsert));
                adapter.notifyItemRangeInserted(initialPosition + 1, numOfItemsToInsert);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition));
    }

    @Test
    public void notifyItemRangeInserted_beforeCurrentPosition_positionIsShiftedRightByRangeLength() {
        final int numOfItemsToInsert = 5;
        final int initialPosition = scrollView.getCurrentItem();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                data.addAll(initialPosition, createItems(numOfItemsToInsert));
                adapter.notifyItemRangeInserted(initialPosition, numOfItemsToInsert);
            }
        });
        onScrollView().check(currentPositionIs(initialPosition + numOfItemsToInsert));
    }

    @Test
    public void notifyItemRangeRemoved_afterCurrentPosition_positionIsNotAffected() {
        final int initialPosition = scrollView.getCurrentItem();
        final int initialSize = adapter.getItemCount();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                List<TestData> toRemove = new ArrayList<>();
                for (int i = initialPosition + 1; i < adapter.getItemCount() - 1; i++) {
                    toRemove.add(data.get(i));
                }
                assertThat(toRemove.size(), is(greaterThan(1)));
                data.removeAll(toRemove);
                assertThat(data.size(), is(equalTo(initialSize - toRemove.size())));
                adapter.notifyItemRangeRemoved(initialPosition + 1, toRemove.size());
            }
        });
        onScrollView().check(currentPositionIs(initialPosition));
    }

    @Test
    public void notifyItemRangeRemoved_beforeCurrentPosition_positionIsShiftedLeftByRangeLength() {
        assertThat(adapter.getItemCount(), is(greaterThan(2)));
        final int initialPosition = adapter.getItemCount() - 1;
        final int numOfItemsToRemove = adapter.getItemCount() - 1;
        ensurePositionIs(initialPosition);
        onUiThread(new Runnable() {
            @Override
            public void run() {
                final int initialSize = adapter.getItemCount();
                List<TestData> data = adapter.getData();
                List<TestData> toRemove = new ArrayList<>();
                for (int i = initialPosition - 1; i >= 0; i--) {
                    toRemove.add(data.get(i));
                }
                assertThat(toRemove.size(), is(equalTo(numOfItemsToRemove)));
                data.removeAll(toRemove);
                assertThat(data.size(), is(equalTo(initialSize - toRemove.size())));
                adapter.notifyItemRangeRemoved(0, toRemove.size());
            }
        });
        onScrollView().check(currentPositionIs(initialPosition - numOfItemsToRemove));
    }

    @Test
    public void notifyDataSetChanged_currentItemRemainsInItemRange_currentIsNotAffected() {
        final int initialPosition = 0;
        ensurePositionIs(initialPosition);
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                assertThat(data.size(), is(greaterThan(2)));
                data.remove(data.size() - 1);
                data.remove(data.size() - 1);
                adapter.notifyDataSetChanged();
            }
        });
        onScrollView().check(currentPositionIs(initialPosition));
    }

    @Test
    public void notifyDataSetChanged_currentItemGoesOutsideItemRange_currentIsClampedToRange() {
        final int initialPosition = adapter.getItemCount() - 1;
        final int numOfItemsToRemove = 2;
        assertThat(adapter.getItemCount(), is(greaterThan(numOfItemsToRemove)));
        ensurePositionIs(initialPosition);
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                for (int i = numOfItemsToRemove - 1; i >= 0; i--) {
                    data.remove(i);
                }
                adapter.notifyDataSetChanged();
            }
        });
        onScrollView().check(currentPositionIs(adapter.getItemCount() - 1));
    }

    @Test
    public void notifyDataSetChanged_allItemsRemoved_scrollViewIsEmpty() {
        onUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.getData().clear();
                adapter.notifyDataSetChanged();
            }
        });
        onScrollView().check(doesNotHaveChildren());
    }

    @Test
    public void notifyDataSetChanged_scrollToPositionCalledAfterItemsAdded_positionIsCorrect() {
        final int targetPosition = adapter.getItemCount();
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                final int itemsToAdd = data.size();
                for (int i = 0; i < itemsToAdd; i++) {
                    data.add(new TestData());
                }
                adapter.notifyDataSetChanged();
                scrollView.scrollToPosition(targetPosition);
            }
        });
        onScrollView().check(currentPositionIs(targetPosition));
    }

    @Test
    public void notifyDataSetChanged_scrollToPositionCalledAfterItemsRemoved_positionIsCorrect() {
        final int initialPosition = adapter.getItemCount() - 1;
        final int targetPosition = adapter.getItemCount() / 4;
        assertThat(targetPosition, is(greaterThan(0)));
        ensurePositionIs(initialPosition);
        onUiThread(new Runnable() {
            @Override
            public void run() {
                List<TestData> data = adapter.getData();
                final int itemsToRemove = data.size() / 2;
                for (int i = 0; i < itemsToRemove; i++) {
                    data.remove(0);
                }
                adapter.notifyDataSetChanged();
                scrollView.scrollToPosition(targetPosition);
            }
        });
        onScrollView().check(currentPositionIs(targetPosition));
    }

    private List<TestData> createItems(int count) {
        List<TestData> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new TestData());
        }
        return result;
    }
}
