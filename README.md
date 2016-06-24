<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Power Adapters](#power-adapters)
- [Feature Summary](#feature-summary)
- [Usage](#usage)
  - [Adapter Composition](#adapter-composition)
  - [Data Type Binding](#data-type-binding)
    - [Binder](#binder)
    - [Mapper](#mapper)
  - [Conversion](#conversion)
  - [Asynchronous Data Loading](#asynchronous-data-loading)
    - [Basic Data Usage](#basic-data-usage)
    - [Invalidating and Reloading](#invalidating-and-reloading)
    - [DataLayout](#datalayout)
    - [RxJava Module](#rxjava-module)
    - [Data Views](#data-views)
  - [Samples](#samples)
- [Build](#build)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Power Adapters

Presenting large data sets efficiently can be a challenging part of Android development. It gets more complicated as we
begin to handle edge cases and add additional decorations like headers. We also often find ourselves repeating
undesirable boilerplate as we write adapters for each data source. In addition, Android doesn't provide a clean
object-oriented, reusable way of presenting collections of multiple types.

# Feature Summary

This library provides the following features:

* Show **headers** and **footers**
* Show a **loading** to indicate a loading state
* Show an **empty** item to indicate an empty underlying data set
* **Concatenate** multiple adapters together
* Add **dividers** in between items of an existing adapter
* Show an adapter or item range only when a **condition** evaluates to `true`
* Present **multiple data types** within an adapter in a type-safe manner
* Present **nested** adapters, a powerful substitute for `ExpandableListView` without any limitation of nesting level
* Load from remote or slow data sources **asynchronously**

Power adapters are compatible with the following collection view classes:
* `android.support.v7.widget.RecyclerView`
* `android.widget.ListView`
* `android.widget.GridView`
* `android.support.v4.view.ViewPager`
* Any other view that accepts a `android.widget.Adapter`

# Usage

Get it from Maven Central, using Gradle:

```groovy
compile 'com.nextfaze.poweradapters:power-adapters:0.10.0-SNAPSHOT'
```

## Adapter Composition

Power Adapters can be composed by using the fluent chaining methods.
For example, say you want to present a list of tweets, with a loading indicator, but show an empty message when there
are no tweets, you can write the following:

```
PowerAdapter adapter = new TweetsAdapter()
    .limit(10) // Only show up to 10 tweets
    .append(
        // Show empty item while no tweets have loaded
        asAdapter(R.layout.tweets_empty_item).showOnlyWhile(noTweets()),
        // Show loading indicator while loading
        asAdapter(R.layout.loading_indicator).showOnlyWhile(tweetsAreLoading())
    )
recyclerView.setAdapter(toRecyclerAdapter(adapter));
```

This lets you write a simple `TweetAdapter` class, the only responsibility of which is to present tweets. By using
`PowerAdapter.append` as such, the `TweetAdapter` need not be modified, and can be potentially reused elsewhere more
easily. The use of `showOnlyWhile` applies a condition to the empty footer item, so it remains hidden unless the
underlying list of tweets is empty.

## Data Type Binding

Included in Power Adapters is the ability to bind elements in your data set to views in a reusable, readable, and
type-safe manner.

### Binder

The primary class needed to achieve this is a `Binder`. The responsibilities of a `Binder` include:

* Construct a `View` to be bound, and re-used by the adapter/recycler view
* Bind an object and/or data set index to the `View`

Multiple types of commonly required binders are supplied. If you prefer the widely used view holder pattern, use
a `ViewHolderBinder`:

```
Binder<BlogPost, View> blogPostBinder = new ViewHolderBinder<BlogPost, BlogPostHolder>(android.R.layout.simple_list_item_1) {
    @NonNull
    @Override
    protected BlogPostHolder newViewHolder(@NonNull View v) {
        return new BlogPostHolder(v);
    }

    @Override
    protected void bindViewHolder(@NonNull BlogPost blogPost,
                                  @NonNull BlogPostHolder blogPostHolder,
                                  @NonNull Holder holder) {
        blogPostHolder.labelView.setText("Blog: " + blogPost.getTitle());
    }
};

class BlogPostHolder extends ViewHolder {

    @NonNull
    final TextView labelView;

    BlogPostHolder(@NonNull View view) {
        super(view);
        labelView = (TextView) view.findViewById(android.R.id.text1);
    }
}
```

If you use custom views for each of your data models, use an `AbstractBinder`:

```
Binder<Tweet, TweetView> tweetBinder = new AbstractBinder<Tweet, TweetView>(R.layout.tweet_item) {
    @Override
    public void bindView(@NonNull Tweet tweet, @NonNull TweetView v, @NonNull Holder holder) {
        v.setTweet(tweet);
        v.setOnClickListener(v -> onTweetClick(tweet));
    }
}
```

### Mapper

The second class involved is the `Mapper`. It is consulted to determine which `Binder` to use for presenting a
particular element in your data set. Typically you'll use `MapperBuilder` to declaratively assign your model classes to
binders:

```
Mapper mapper = new MapperBuilder()
    .bind(Tweet.class, new TweetBinder())
    .bind(Ad.class, new AdBinder())
    .bind(Video.class, new VideoBinder())
    .build();
ListBindingAdapter<Object> adapter = new ListBindingAdapter<>(mapper);
adapter.add(new Tweet());
adapter.add(new Ad());
adapter.add(new Video());
```

## Conversion

Once you're ready to assign a `PowerAdapter` to a collection view, simply invoke one of the following conversion methods:

|Collection View    |Converter                                  |Extension Module                                           |
|:------------------|------------------------------------------:|:---------------------------------------------------------:|
|`ListView`         |            `PowerAdapters.toListAdapter()`|None                                                       |
|`RecyclerView`     |`RecyclerPowerAdapters.toRecyclerAdapter()`|`power-adapters-recyclerview-v7`                           |
|`ViewPager`        |`SupportPowerAdapters.toPagerAdapter()`    |`power-adapters-support-v4`                                |

## Asynchronous Data Loading

Implementing a UI for presenting the contents of a remote collection, like a list of comments or products, requires
several different mechanics. Among them are:
* Perform requests asynchronously to avoid blocking the UI thread
* Presenting a loading indicator to give the user feedback on progress
* Allow the user to page through results
* Handle and present errors as they occur
* Dispatch change notifications to your adapter so your `RecyclerView` or `ListView` can react to content changes

The `power-adapters-data` extension module aims to simplify this by encapsulating the above concerns into a single
object: `Data<T>`. In doing so, it allows you to retain one object when a config change occurs, like an orientation
change. This way you don't need to reload or parcel/unparcel all of your list results when that occurs. The `Data<T>`
object comprises much of the repetitive asynchronous UI "glue" code you'd otherwise have to write (and debug) yourself.

```groovy
compile 'com.nextfaze.poweradapters:power-adapters-data:0.10.0-SNAPSHOT'
```

### Basic Data Usage

The recommended usage pattern is to instantiate a `Data<T>` object in your retained `Fragment`:

```
public final class ProductListFragment extends Fragment {

    private final Data<Product> mProducts = new ArrayData<>() {
        @NonNull
        @Override
        protected List<Product> load() throws Throwable {
            return mApi.getProducts();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
```

Now hook up your `Data<Product>` instance with your `RecyclerView`:

```
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mRecyclerView.setAdapter(new RecyclerDataAdapter(mProducts));
}

@Override
public void onDestroyView() {
    super.onDestroyView();
    // Must nullify adapter, otherwise after a config change, RecyclerView will
    // be retained by a strong reference chain of observers.
    mRecyclerView.setAdapter(null);
}
```

### Invalidating and Reloading

At some stage you'll want to request a reload of the elements from the remote source. You can do this using `reload()`,
`refresh()`, or `invalidate()`. The behaviour of these methods differ slightly, but ultimately they all result in your
items being reloaded from the source. See the `Data` javadoc for how they differ.

### DataLayout

`DataLayout` aids in presenting the various states of a `Data` instance, by hiding and showing contents, empty, error,
and loading child views.
It's a `RelativeLayout` subclass, and it works by accepting a `Data` instance, then registering to receive change
notifications. If the contents is empty, your marked empty view will be shown instead of the list view. If an error occurs,
the error view will be shown until a reload is triggered. `DataLayout` has several extension points to customize this behaviour
to suite the needs of your application.

Here's an example of how to declare a `DataLayout` in XML:

```xml
<com.nextfaze.poweradapters.data.widget.DataLayout
            xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            android:id="@+id/news_fragment_data_layout"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

    <ListView
            android:id="@+id/list"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:layout_component="content"/>

    <ProgressBar
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            app:layout_component="loading"/>

    <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            app:layout_component="empty"
            android:text="No items!"/>

    <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            app:layout_component="error"
            android:textColor="#ffff0000"/>

</com.nextfaze.poweradapters.data.widget.DataLayout>
```

Now you need to connect to your `DataLayout` and `ListView` in Java code:

```
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mListView.setAdapter(new DataAdapter(mProducts));
    mDataLayout.setData(mProducts);
}
```

### RxJava Module

An RxJava module is provided: `power-adapters-data-rx`. This is a simple adapter library that provides `Observable`s
for properties of `Data`:

```
RxData.changes(mProducts).subscribe(new Action1<InsertEvent>() {
    @Override
    public void call(InsertEvent event) {
        ...
    }
});
```

### Data Views

`Data` instances can be represented as a view, much like a relational database. The `Datas` utility class provides
static factory methods for wrapping an existing `Data` object, and providing a filtered or transformed view of its contents.

```
Data<String> names = ...
Data<Integer> lengths = Datas.transform(names, new Function<String, Integer>() {
    @NonNull
    @Override
    public Integer apply(@NonNull String name) {
        return name.length;
    }
});
```

```
Data<Post> allPosts = ...
Data<Post> todaysPosts = Datas.filter(names, new Predicate<Post>() {
    @Override
    public boolean apply(@NonNull Post post) {
        return isToday(post.getDate());
    }
});
```

## Samples

Check the included sample project for a range of usage pattern examples.

# Build

Building instructions:

```bash
$ git clone <github_repo_url>
$ cd power-adapters
$ ./gradlew clean build

```

# License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.