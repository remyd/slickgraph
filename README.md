# *slick***graph**

*slick***graph** is a timeseries visualization library for JavaFX that uses the canvas.
It ensures a high visual accuracy by implementing a binning and smoothing algorithm based on the pixels.
Usually, visualization techniques for timeseries takes arbitrary time intervals to bin the data and draw interpolated curves between the interval values.
This approach introduces visual artifacts.
*slick***graph** takes a different approach: it relies on the pixels to bin the timeseries and computes a convolution with a Gaussian kernel to provide a smooth visualization.
Visual artifacts still exist but are minimized to the pixels.

## Documentation

You will find the instructions to use the library [here](http://caladan.fr/getstarted.html).
