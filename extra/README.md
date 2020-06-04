# Project 2 : C. Elegans Development

#### Course

Bioimage Informatics BIO-410, École Polytechnique Fédérale de Lausanne

#### Professors
- Prof. Daniel Sage
- Prof. Arne Seitz

#### Authors
- Arnaud Dhaene, EPFL
- Audrey Ménaësse, EPFL

This folder contains unfinished code with regards to a star distance region growing algorithm. The idea was to have a number of equidistant radii coming out of a seed (using the Fibonacci sphere) and making them grow following specific conditions. This polyhedron would then represent each embryonic cell.

Difficulties were met when needing to fetch all points inside a star convex 3D polyhedron in Java. Utility functions are sparse and the implementation would be very complex. Furthermore, the investment of time was met with an uncertain return of accuracy in approximating 3D regions within the embryo.
