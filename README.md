# DiscreteScrollView

The library is a RecyclerView-based implementation of a scrollable list, where current item is centered and can be changed using swipes.
It is similar to a ViewPager, but you can painleslly create layout, where views adjacent to the currently selected view are partially or 
fully visible on the screen (I've tried to use ViewPager for this purpose and, please, don't repeat my mistakes - better use this library!).

//GIF1 goes here

## Gradle 
Add this into your dependencies block. (May be not added to jCenter() yet).
```
compile 'com.yarolegovich:discrete-scrollview:1.0.0
```
## Sample
Please see the sample app for examples of library usage. 
//GIF2 goes here

## Wiki
###General
The library uses a custom LayoutManager to adjust items' positions on the screen and handle scroll, however it is not exposed to the client 
code. All public API is accessible through DiscreteScrollView class, which is a simple descendant of RecyclerView.

If you have ever used RecyclerView - you already know how to use this library. One thing to note - you should NOT set LayoutManager.

#### How to use the library:
 1. Add DiscreteScrollView to your layout either using xml or code:
 2. Create your implementation of RecyclerView.Adapter. Refer to the sample for an example, if you don't know how to do it.
 3. Set the adapter.
 4. You are done! 
```xml
<com.yarolegovich.discretescrollview.DiscreteScrollView
        android:id="@+id/picker"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
```
```java
DiscreteScrollView scrollView = findViewById(R.id.picker);
scrollView.setAdapter(new YourAdapterImplementation());
```
###API
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
#### Callbacks
* Scroll state changes
```java
scrollView.setScrollStateChangeListener(listener);

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
   */
  void onScroll(float scrollPosition); 
}
```
* Current selection changes
```java
scrollView.setCurrentItemChangeListener(listener);

public interface CurrentItemChangeListener<T extends ViewHolder> {
  /**
   * Called when new item is selected. It is similar to the onScrollEnd of ScrollStateChangeListener, except that it is 
   * also called when currently selected item appears on the screen for the first time.
   */
  void onCurrentItemChanged(T viewHolder, int adapterPosition); 
}
```

###License
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
