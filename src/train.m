detector = vision.CascadeObjectDetector('../data/detector.xml');
disp('Detector');
display(numel(positiveInstances))

for i=1:numel(positiveInstances)
    img = imread(positiveInstances(i).imageFilename);
    detectedImg = imcrop(img, positiveInstances(i).objectBoundingBoxes(1,:));
    
    detectedImg = imresize(detectedImg, [30, 32]);
    [pathstr, name, ext] = fileparts(positiveInstances(i).imageFilename);
    imwrite(detectedImg, strcat(strcat('../data/train/', name), ext));
    display(i)
end