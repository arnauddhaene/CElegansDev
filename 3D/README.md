# Project 2 : C. Elegans Development

### Course
Bioimage Informatics BIO-410, École Polytechnique Fédérale de Lausanne

### Professors
- Prof. Daniel Sage
- Prof. Arne Seitz

### Authors
- Arnaud Dhaene, EPFL
- Audrey Ménaësse, EPFL

## Volume evolution during embryonic development
This repository contains the relevant code for three ImageJ PlugIns
enabling users to approximate volume evolution of C. Elegans embryonic
cells over time from a 4D image Hyperstack.


## Installation
To install these PlugIns, one must download the entire repository and copy the files
into a Java project in an Eclipse Workspace. The Java project must have the relevant
`plugins.config` and `build.xml` files as described in the earlier weeks of the
**Bioimage Informatics BIO-410** course.

The following lines must be added to the `plugins.config` file :
```
Plugins>BII_Project, "Image Preprocessing", Img_Preprocessing("")
Plugins>BII_Project, "Region Growing 3D", Region_Growing3D("")
Plugins>BII_Project, "Volume Calculation", Volume_Calc("")
```


## Usage
In order to perform the measurements, please follow the steps below. The values of the different parameters to use on the demo dataset is given in parentheses.

1. Open ImageJ from an Eclipse Workspace
2. Open the datset to analyse (`real-stack.tif`)
3. Execute the three PlugIns as follows

### Image Preprocessing
1. Run the preprocessing plugin in `Plugins > BII_Project >  Image Preprocessing`
2. In the first dialog box, select the original image to get the path where the intermediate images created by the plugin will be saved.
The `Denoised.tif` and `Shell.tif` images are created and saved automatically.
3. A second dialog box appear for the Canny Edge detector, select the following parameters:
	- `Gaussian kernel radius` (2) : gaussian radius for smoothing
	- `Low threshold` (4) : low threshold for non-maximum suppression
	- `High threshold`(8) : high threshold for non-maximum suppression
	- `Gaussian kernel width` :  width of gaussian to compute the gradients


### Region Growing

1. Open or select the `Denoised.tif` image.
2. Go to the central slice in z (13/27) on the last frame (20/20 for half of the time-points or 40/40)
3. Select the seed points manually at the approximate barycenter of each cell with the multi-points tool.
4. Run the 3D region growing plugin in `Plugins > BII_Project > Region Growing 3D`
5. In the first dialog box, select the original image to get the path to retrieve the intermediate images automatically saved by the preprocessing plugin.
6. In the second dialog box select the following parameters :
	- `Max number of iterations` (40)
	- `Max Mean Difference (graylevel)` (100)
	- `Threshold (graylevel)` (4000)
	- `Max distance (µm)` (150)

### Volume Calculation

1. Open or select the `areas.tif` image
2. Run the volume calculation plugin in `Plugins > BII_Project > Volume Calculation`. You will obtain both a graph and a table containing the volume of each cell in function of time.
3. Save the `Result Table` and the `Figure` manually if needed.

## Warning
As these PlugIns are automatically saving images or graphs, some errors can occur depending on the operating system you are using. In case of error, please check the lines containing `IJ.saveAs(...);` and the directory in particular first.

## PlugIns Framework

### Preprocessing

1. The original image is denoised with a 2x2x2 3D median filter before a 1.25x1.25x1.2 3D Gaussian filter.
2. A binary image of the footprint of the embryo in the image is created using :
	- An ImageJ Huang thresholding on the denoised image
	- A 2x2x2 3D median filter to reduce the salt and pepper noise
	- A 3D closing of the image using a 3D ball structural element.
3. A binary image of the membranes of the cells is created using a Canny Edge-detector according to the following steps :
	- A gaussian smoothing
	- The calculation of the gradients in X and Y directions
	- A non-maximum suppression
		- If the value of a given pixel is below the low threshold, it is considered as not an edge.
		- If the value of the pixel is between the low and the high threshold, the pixel is considered as an edge only if it has an edge-pixel in its neighborhood.
		- If the value of the pixel is above the high threshold it is considered as an edge.

### Region Growing

##### Selection of the seed points

The seed points for the region growing are manually selected on the central slice of the last frame using the multipoint tool of ImageJ.
The algorithm then works backwards in time and merge the seed points of two daughter cells which are the result of mitosis.
The list of seed points for each time frame is defined recursively as follows :
	- The seed points of the previous time frame are repositioned at the barycenter of their region
	- This list of seed point is then cloned
	- For each point one checks on the `edges.tif` file if a membrane is separating from the other ones. If not, the two non-separated seed points are merged in a new seed in the middle of the segment between the two old ones.

##### Region growing algorithm

In order to make the regions grow, the algorithm uses both hard and soft conditions :
- Hard conditions
	- Pixel is inside the embryo footprint of the image `shell.tif`
	- Pixel in output image is free (prevent region overlap)
	- Pixel neighbors are in Region
	- First half of the iterations: needs 1 neighbor or more in the 26-connected neighborhood
	- Second half of the iterations: needs 6 neighbors or more in the 26-connected neighborhood
	- Pixel value is below threshold

- Soft conditions
The soft conditions are utilised in a way that a cost function is used to give a probability of region adherence, which is a value between 0 and 1. The cost function consists of a weighted sum of sigmoid functions receiving the conditions explicited below as input, respectively. The weights were manually adjusted to get adequate results. Whilst not very robust, this method works very well for the project task at hand. The probability is thresholded by 0.5 to decide on adherence versus rejection.
	- Pixel value versus Region mean
	- Isotropic distance to Region seed

##### Image saving

 The output image is automatically saved as `areas.tif` in the same folder as the preprocessed images

### Volume Calculation

This plug-in calculates the volume of each cell in function of time from the `areas.tif` image. As in this image, each cell is associated with a color, this plugins counts the total number of voxels for each color before multiplying it by the calibration of the original image.

## Results

The results of this measurement is composed of an ImageJ Result Table and a plot which are displayed at the end of the procedure but not saved automatically.
