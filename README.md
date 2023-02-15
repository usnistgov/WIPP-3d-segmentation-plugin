# WIPP-3d-segmentation-plugin

##  Statements of purpose and maturity
The purpose of this work is to create a [WIPP](https://github.com/usnistgov/WIPP) plugin for the segmentation of 3D microscopy images.
See license for disclaimer.

##  Description of the repository contents

- `src`: contains the source Java code
- `Dockerfile`
- `plugin.json` WIPP plugin manifest

###   Technical installation instructions, including operating system or software dependencies

The project Java 8+ and Maven.

## Installation (optional if using the pre-built Docker image)

### Build JAR
```
mvn clean package
```

### Build the Docker image
```
docker build . -t wipp/wipp-3d-segmentation-plugin:0.0.1
```

## Execution

### Run the Python code

From this directory:
```
python ./src/stardist-inference.py \
--inputImages ./sample-data/images \
--output ./sample-data/outputs
--pretrainedModel 2D_versatile_fluo
```

### Run the Docker image
From this directory, assuming the images to process are in a folder "sample-data/images":
```
docker run -v "$PWD"/sample-data:/data \
wipp/wipp-3d-segmentation-plugin:0.0.1 \
--inputImages /data/images \
--output /data/outputs \
--threshold Otsu \
--removeEdgeComponents false \
--fillHoles false \
--makeSingleComponent false
```
`-v`: mounts a volume/folder from your machine inside of the Docker container

### Run the WIPP plugin
	- register the plugin.json in a deployed WIPP instance - see https://github.com/usnistgov/WIPP
	- upload input images as WIPP image collection
	- create a workflow by adding one step called 3d-seg
	- run and monitor the workflow execution
	- download resulting WIPP image colection

## Additional Information

###    Contact information
-   WIPP team, ITL NIST, Software and System Division, Information Systems Group
-   Contact email address at NIST: wipp-team@nist.gov

###    Related Material
-    https://isg.nist.gov/deepzoomweb/data/stemcellmaterialinteractions

###    Citation:
Simon Jr., C. , Bajcsy, P. , Chalfoun, J. , Majurski, M. , Brady, M. , Simon, M. , Hotaling, N. , Schaub, N. , Horenberg, A. , Szczypinski, P. , Wang, D. , DeFelice, V. , Yoon, S. and Florczyk, S. (2023), Measuring Dimensionality of Cell-Scaffold Contacts of Primary Human Bone Marrow Stromal Cells Cultured on Electrospun Fiber Scaffolds, Journal of Biomedical Materials Research, [online], https://doi.org/10.1002/jbm.a.37449, https://tsapps.nist.gov/publication/get_pdf.cfm?pub_id=934599 (Accessed February 10, 2023)
