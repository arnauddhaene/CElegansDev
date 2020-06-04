# Project 2: C. Elegans Development

##### Course

Bioimage Informatics BIO-410, École Polytechnique Fédérale de Lausanne

##### Professors
* Prof. Daniel Sage
* Prof. Arne Seitz

##### Authors
* Arnaud Dhaene, EPFL
* Audrey Menaesse, EPFL

## Surface evolution during embryonic development

This repository contains the relevant code for the ImageJ PlugIn enabling users to approximate surface evolution of C. Elegans embryonic cells over time.

## Installation

To install the PlugIn, one must simply download the entire repo and copy the files into a Java project. The Java project must have the relevant `plugins.config` and `build.xml` files as described in the earlier weeks of the **Bioimage Informatics BIO-410** course.

The following line must be added to the `plugins.config` file:

```
Plugins>BII2020, "Region Growing 2D", Region_Growing2D("")
```

## Usage

To run the plugin, please follow the steps below:

1. Run ImageJ from your Eclipse project
2. Load an image to ImageJ
3. (optional) Create a Substack of your 4D Hyperstack
  * `Image > Stacks > Tools > Make Substack...`
  * Select **one** slice and the wanted time-frames
4. Use the scroller to get to the last timeframe
5. Use the `Multi-point` selection tool and place a **seed** at the approximate barycenter of each cell
6. Select `Plugins > Region Growing 2D` in the menubar
7. Select the wanted parameters and run the PlugIn

## PlugIn framework

### Preprocessing

The image is filtered using a median filter with sigma = 2.0.

### Main algorithm

The following steps are followed

1. Use seeds to run Region Growing algorithm
2. Use previous regions for barycenter seed modification and directional gradient between seeds to identify edges (to identify cell mitosis) between regions to find seeds of the following (or previous, with respect to time) frame
3. Go back to step **1** until t = 1

### Region Growing

The algorithm uses both both *hard* and *soft* conditions for region growth:

##### Hard conditions

* Pixel in output image is free (prevent region overlap)
* Pixel neighbors are in Region
  * 1st third of the iterations: 1 neighbor or more
  * 2nd and 3rd thirds of the iterations: 3 neighbors or more
* Pixel value is below threshold

##### Soft conditions

The soft conditions are utilised in a way that a cost function is used to give a _probability of region adherence_, which is a value between 0 and 1. The cost function consists of a weighted sum of sigmoid functions receiving the conditions explicited below as input, respectively. The weights were manually adjusted to get adequate results. Whilst not very robust, this method works very well for the project task at hand. The probability is thresholded by 0.5 to decide on adherence versus rejection.

* Pixel value versus Region mean
* Isotropic distance to Region seed

### Results

An ImageJ ResultsTable and plot are displayed at the end of the procedure.
