detector = vision.CascadeObjectDetector('../data/detector.xml');


images = imageSet('../data/raw/');

for i=1:images.Count
    display(i)
    img = read(images, i);
    bbox = step(detector, img);
    if size(bbox) > 0
        detectedImg = imcrop(img, bbox(1,:));
        detectedImg = imresize(detectedImg, [30, 32]);
    
        info = images.ImageLocation(i)
        [pathstr, name, ext] = fileparts(positiveInstances(i).imageFilename);
        imwrite(detectedImg, strcat(strcat('../data/imgs/', name), ext))

    end
end