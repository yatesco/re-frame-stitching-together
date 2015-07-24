# Stitching together

A demonstration of different ways of stitching together data in a re-frame
application.

## Domain

The domain is of course trivial and composed of:

* fruit(s) which have a name and a...
* temperature

In this domain both are entities and when we render a fruit we want to render
their temperature as well.

## Demonstration

This application demonstrates 4 different modes, well actually three with a
(bad) implementation:

### 1. One subscription for the whole table
This is the most inefficient implementation and has a single subscription
which decorates each fruit with the temperature description.

If either the fruit or the temperature change then the entire subscription is
re-run resulting in the table being re-rendered. Of course, only the rows with
actual changed data will be re-rendered.

### 2. Row component subscribes to temperature changes
The table is provided the normalised fruits and passes those to the row
component. The row component has a subscription to the temperatures and
denormalises when the component renders.

This is conceptually quite clean except it isn't particularly efficient as
changes to any temperature will cause every row to be re-rendered. Changes to
fruits are properly scoped however.

### 3. A subscription to a de-normalised row (bad)
The table is provided the normalised fruits and passes the _id_ to the row. The
row component then subscribes to a denormalised view _for that id_.

This is conceptually the most efficient however, it is easy to get the
subscription wrong resulting in the subscription being sensitive to any changes
in either the fruits or the temperature.

### 4. A subscription to a de-normalised row (good)
As 3., the row subscribes to a normalised view of a single fruit.

Unlike 3, this subscription works correctly and uses chained reactions to make
the final subscription ignorant of everything except that particular fruit and
temperature.

An alternative implementation would move the nested reactions to top level
subscriptions.

## Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).
Clear the console and then click the `Change a fruit` or `Change a temperature`
button and check the console messages to see which components actually
changed.


## conclusiongit

The 4th option is the most efficient by far, and that efficiency is only going
to increase as the data sets increase in size.

## TODO

Check out "PROBLEM#Keys" - why doesn't it work when the keys are defined as part
of the component or as meta-data on the component?
