# DiscreteScrollView

The library is a RecyclerView-based implementation of a scrollable list, where current item is centered and can be changed using swipes.
It is similar to a ViewPager, but you can quickly and painlessly create layout, where views adjacent to the currently selected view are partially or fully visible on the screen. 

![GifSampleShop](https://github.com/yarolegovich/DiscreteScrollView/blob/master/images/cards_shop.gif)

[ ![Download](https://api.bintray.com/packages/jjhesk/maven/discrete-scrollview/images/download.svg) ](https://bintray.com/jjhesk/maven/discrete-scrollview/_latestVersion)

## Gradle 
Add this into your dependencies block.
```
compile 'com.hkm.ui:discrete-scrollview:1.3.1'
```

## Sample
<a href="https://play.google.com/store/apps/details?id=com.yarolegovich.discretescrollview.sample"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge.png" width="185" height="60"/></a><br>

Please see the [sample app](https://github.com/yarolegovich/DiscreteScrollView/tree/master/sample/src/main/java/com/yarolegovich/discretescrollview/sample) for examples of library usage.

![GifSampleWeather](https://github.com/yarolegovich/DiscreteScrollView/blob/master/images/cards_weather.gif)

## Wiki
### General
The library uses a custom LayoutManager to adjust items' positions on the screen and handle scroll, however it is not exposed to the client 
code. All public API is accessible through DiscreteScrollView class, which is a simple descendant of RecyclerView.

If you have ever used RecyclerView - you already know how to use this library. One thing to note - you should NOT set LayoutManager.

#### Usage:
 1. Add DiscreteScrollView to your layout either using xml or code:
 2. Create your implementation of RecyclerView.Adapter. Refer to the [sample](https://github.com/yarolegovich/DiscreteScrollView/blob/master/sample/src/main/java/com/yarolegovich/discretescrollview/sample/shop/ShopAdapter.java) for an example, if you don't know how to do it.
 3. Set the adapter.
 4. You are done! 
```xml
<com.yarolegovich.discretescrollview.DiscreteScrollView
  android:id="@+id/picker"
  android:layout_width="match_parent"
  android:layout_height="wrap_content"
  app:dsv_orientation="horizontal|vertical" />  <!-- orientation is optional, default is horizontal -->
```
```java
DiscreteScrollView scrollView = findViewById(R.id.picker);
scrollView.setAdapter(new YourAdapterImplementation());
```

### API
#### Layout
```java
scrollView.setOrientation(Orientation o); //Sets an orientation of the view
scrollView.setOffscreenItems(count); //Reserve extra space equal to (childSize * count) on each side of the view
```
#### Related to the current item:
```java
scrollView.getCurrentItem(); //returns adapter position of the currently selected item or -1 if adapter is empty.
scrollView.scrollToPosition(int position); //position becomes selected
scrollView.smoothScrollToPosition(int position); //position becomes selected with animated scroll
scrollView.setItemTransitionTimeMillis(int millis); //determines how much time it takes to change the item on fling, settle or smoothScroll
```
#### Transformations
One useful feature of ViewPager is page transformations. It allows you, for example, to create carousel effect. DiscreteScrollView also supports 
page transformations.
```java
scrollView.setItemTransformer(transformer);

public interface DiscreteScrollItemTransformer {
    /**
     * In this method you apply any transform you can imagine (perfomance is not guaranteed).
     * @param position is a value inside the interval [-1f..1f]. In idle state:
     * |view1|  |currentlySelectedView|  |view2|
     * -view1 and everything to the left is on position -1;
     * -currentlySelectedView is on position 0;
     * -view2 and everything to the right is on position 1.
     */
    void transformItem(View item, float position); 
}
```
Because scale transformation is the most common, I included a helper class - ScaleTransformer, here is how to use it:
```java
cityPicker.setItemTransformer(new ScaleTransformer.Builder()
  .setMaxScale(1.05f) 
  .setMinScale(0.8f) 
  .setPivotX(Pivot.X.CENTER) // CENTER is a default one
  .setPivotY(Pivot.Y.BOTTOM) // CENTER is a default one
  .build());
```
You may see how it works on GIFs.

#### Slide through multiple items

To allow slide through multiple items call:
```java
scrollView.setSlideOnFling(true);
```
The default threshold is set to 2100. Lower the threshold, more fluid the animation. You can adjust the threshold by calling:
```java
scrollView.setSlideOnFlingThreshold(value);
```

#### Infinite scroll
Infinite scroll is implemented on the adapter level:
```java
InfiniteScrollAdapter wrapper = InfiniteScrollAdapter.wrap(yourAdapter);
scrollView.setAdapter(wrapper);
```
An instance of `InfiniteScrollAdapter` has the following useful methods:
```java
int getRealItemCount();

int getRealCurrentPosition();

int getRealPosition(int position);

/*
 * You will probably want this method in the following use case:
 * int targetAdapterPosition = wrapper.getClosestPosition(targetPosition);
 * scrollView.smoothScrollTo(targetAdapterPosition);
 * To scroll the data set for the least required amount to reach targetPosition.
 */
int getClosestPosition(int position); 
```
Currently `InfiniteScrollAdapter` handles data set changes inefficiently, so your contributions are welcome. 
#### Callbacks
* Scroll state changes:
```java
scrollView.addScrollStateChangeListener(listener);
scrollView.removeScrollStateChangeListener(listener);

public interface ScrollStateChangeListener<T extends ViewHolder> {

  void onScrollStart(T currentItemHolder, int adapterPosition); //called when scroll is started, including programatically initiated scroll
  
  void onScrollEnd(T currentItemHolder, int adapterPosition); //called when scroll ends
  /**
   * Called when scroll is in progress. 
   * @param scrollPosition is a value inside the interval [-1f..1f], it corresponds to the position of currentlySelectedView.
   * In idle state:
   * |view1|  |currentlySelectedView|  |view2|
   * -view1 is on position -1;
   * -currentlySelectedView is on position 0;
   * -view2 is on position 1.
   * @param currentIndex - index of current view
   * @param newIndex - index of a view which is becoming the new current
   * @param currentHolder - ViewHolder of a current view
   * @param newCurrent - ViewHolder of a view which is becoming the new current
   */
  void onScroll(float scrollPosition, int currentIndex, int newIndex, @Nullable T currentHolder, @Nullable T newCurrentHolder); 
}
```
* Scroll:
```java
scrollView.addScrollListener(listener);
scrollView.removeScrollListener(listener);

public interface ScrollListener<T extends ViewHolder> {
  //The same as ScrollStateChangeListener, but for the cases when you are interested only in onScroll()
  void onScroll(float scrollPosition, int currentIndex, int newIndex, @Nullable T currentHolder, @Nullable T newCurrentHolder);
}
```
* Current selection changes:
```java
scrollView.addOnItemChangedListener(listener);
scrollView.removeOnItemChangedListener(listener);

public interface OnItemChangedListener<T extends ViewHolder> {
  /**
   * Called when new item is selected. It is similar to the onScrollEnd of ScrollStateChangeListener, except that it is 
   * also called when currently selected item appears on the screen for the first time.
   * viewHolder will be null, if data set becomes empty 
   */
  void onCurrentItemChanged(@Nullable T viewHolder, int adapterPosition); 
}
```

## Special thanks
Thanks to [Tayisiya Yurkiv](https://www.behance.net/yurkivt) for sample app design and beautiful GIFs.

## License
```
Copyright 2017 Yaroslav Shevchuk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```